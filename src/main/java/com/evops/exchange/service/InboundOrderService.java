package com.evops.exchange.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evops.exchange.entity.ExchangeMessage;
import com.evops.exchange.enums.ExchangeDirection;
import com.evops.exchange.enums.ExchangeMessageStatus;
import com.evops.exchange.enums.ExchangeMessageType;
import com.evops.exchange.enums.ReplyResult;
import com.evops.exchange.mapper.ExchangeMessageMapper;
import com.evops.exchange.xml.FuYinOrderMessage;
import com.evops.exchange.xml.FuYinOrderReply;
import com.evops.exchange.xml.JaxbConverter;
import com.evops.printorder.dto.PrintOrderCreateRequest;
import com.evops.printorder.service.PrintOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 入站付印数据受理：
 * 1) 原文先归档（排障以原文为准）；2) 反序列化并幂等校验；3) 转建档请求落库；
 * 4) 组装 ACCEPTED/REJECTED 回执（回执原文同时按出站归档）。
 * 业务拒绝（机台号不存在、印单号重复等）不抛到 Web 层，统一回 REJECTED 回执，
 * 且入站报文保留并置 REJECTED。
 */
@Service
public class InboundOrderService {

    private static final Logger log = LoggerFactory.getLogger(InboundOrderService.class);

    private final MessageArchiveService archiveService;
    private final PrintOrderXmlMapper xmlMapper;
    private final PrintOrderService printOrderService;
    private final ExchangeMessageMapper messageMapper;
    private final Validator validator;

    public InboundOrderService(MessageArchiveService archiveService,
                               PrintOrderXmlMapper xmlMapper,
                               PrintOrderService printOrderService,
                               ExchangeMessageMapper messageMapper,
                               Validator validator) {
        this.archiveService = archiveService;
        this.xmlMapper = xmlMapper;
        this.printOrderService = printOrderService;
        this.messageMapper = messageMapper;
        this.validator = validator;
    }

    /** 处理入站原文，返回回执 XML 字符串 */
    public String receive(String rawXml) {
        FuYinOrderMessage message;
        try {
            message = JaxbConverter.fromXml(rawXml, FuYinOrderMessage.class);
        } catch (IllegalArgumentException e) {
            // 报文无法解析：原文仍留档（REJECTED），回通用拒绝回执
            log.warn("入站报文无法解析: {}", e.getMessage());
            String fallbackId = "BAD-" + uuid();
            archiveService.register(ExchangeDirection.IN, ExchangeMessageType.PRINT_ORDER,
                    fallbackId, "UNKNOWN", null, rawXml, ExchangeMessageStatus.REJECTED);
            return replyXml(newReply(fallbackId, fallbackId, "UNKNOWN",
                    ReplyResult.REJECTED, "报文格式错误，无法解析: " + safe(e.getMessage())));
        }

        String messageId = message.getHeader() == null ? null : message.getHeader().getMessageId();
        String orderNo = message.getBody() == null ? null : message.getBody().getOrderNo();
        if (messageId == null || messageId.trim().isEmpty()) {
            return replyXml(newReply("RPL-" + uuid(), "MISSING", orderNo == null ? "UNKNOWN" : orderNo,
                    ReplyResult.REJECTED, "报文头 messageId 缺失"));
        }

        // 幂等：同一流水号重复推送，直接回放上次结论，不再重复建档
        ExchangeMessage existing = messageMapper.selectOne(
                new LambdaQueryWrapper<ExchangeMessage>()
                        .eq(ExchangeMessage::getDirection, ExchangeDirection.IN.name())
                        .eq(ExchangeMessage::getMessageId, messageId)
                        .last("LIMIT 1"));
        if (existing != null) {
            ReplyResult result = ExchangeMessageStatus.REJECTED.name().equals(existing.getStatus())
                    ? ReplyResult.REJECTED : ReplyResult.ACCEPTED;
            log.info("入站报文重复推送，幂等回放 messageId={} result={}", messageId, result);
            return replyXml(newReply("RPL-" + uuid(), messageId, existing.getOrderNo(),
                    result, "重复报文，已按首次处理结论幂等回放"));
        }

        // 原文先留档（独立事务）；即便后续建档被拒，原文也在
        ExchangeMessage record;
        try {
            record = archiveService.register(ExchangeDirection.IN, ExchangeMessageType.PRINT_ORDER,
                    messageId, orderNo, null, rawXml, ExchangeMessageStatus.RECEIVED);
        } catch (DataIntegrityViolationException dup) {
            // 并发重复推送撞唯一索引：回放 ACCEPTED
            log.info("入站报文并发重复，幂等回放 messageId={}", messageId);
            return replyXml(newReply("RPL-" + uuid(), messageId, orderNo,
                    ReplyResult.ACCEPTED, "重复报文，已按首次处理结论幂等回放"));
        }

        try {
            if (message.getBody() == null) {
                throw new IllegalArgumentException("报文体 body 缺失");
            }
            PrintOrderCreateRequest request = xmlMapper.toCreateRequest(message.getBody());
            validate(request);
            printOrderService.create(request);
            log.info("入站付印数据受理成功 messageId={} orderNo={}", messageId, orderNo);
            return replyXml(archiveReply(record, messageId, orderNo, ReplyResult.ACCEPTED, null));
        } catch (RuntimeException e) {
            // 业务拒绝（机台号不存在/印单号重复/字段不合法等）：置 REJECTED 并回原因
            markRejected(record, e.getMessage());
            log.warn("入站付印数据受理拒绝 messageId={} orderNo={}: {}", messageId, orderNo, e.getMessage());
            return replyXml(archiveReply(record, messageId, orderNo,
                    ReplyResult.REJECTED, safe(e.getMessage())));
        }
    }

    /** 回执原文同时按出站归档，双向报文都留底 */
    private FuYinOrderReply archiveReply(ExchangeMessage inboundRecord, String refMessageId,
                                         String orderNo, ReplyResult result, String reason) {
        FuYinOrderReply reply = newReply("RPL-" + uuid(), refMessageId, orderNo, result, reason);
        archiveService.register(ExchangeDirection.OUT, ExchangeMessageType.PRINT_ORDER_REPLY,
                reply.getMessageId(), orderNo, refMessageId,
                JaxbConverter.toXml(reply), ExchangeMessageStatus.SENT);
        return reply;
    }

    private void markRejected(ExchangeMessage record, String reason) {
        record.setStatus(ExchangeMessageStatus.REJECTED.name());
        record.setErrorMessage(truncate(reason, 900));
        messageMapper.updateById(record);
    }

    /** bean 校验：字段缺失/非法回明确原因（如印单号不能为空、印数必须大于0） */
    private void validate(PrintOrderCreateRequest request) {
        Set<ConstraintViolation<PrintOrderCreateRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException(msg);
        }
    }

    private FuYinOrderReply newReply(String messageId, String refMessageId, String orderNo,
                                     ReplyResult result, String reason) {
        FuYinOrderReply reply = new FuYinOrderReply();
        reply.setMessageId(messageId);
        reply.setRefMessageId(refMessageId);
        reply.setOrderNo(orderNo);
        reply.setResult(result.name());
        reply.setReason(reason);
        reply.setReplyTime(LocalDateTime.now());
        return reply;
    }

    private String replyXml(FuYinOrderReply reply) {
        return JaxbConverter.toXml(reply);
    }

    private static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String safe(String s) {
        return truncate(s, 500);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
