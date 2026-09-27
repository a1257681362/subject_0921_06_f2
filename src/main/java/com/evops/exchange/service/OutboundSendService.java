package com.evops.exchange.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evops.common.BusinessException;
import com.evops.exchange.client.PlatformClient;
import com.evops.exchange.client.PlatformSendResult;
import com.evops.exchange.config.ExchangeProperties;
import com.evops.exchange.entity.ExchangeMessage;
import com.evops.exchange.enums.ExchangeDirection;
import com.evops.exchange.enums.ExchangeMessageStatus;
import com.evops.exchange.enums.ExchangeMessageType;
import com.evops.exchange.mapper.ExchangeMessageMapper;
import com.evops.exchange.xml.FuYinOrderData;
import com.evops.exchange.xml.FuYinOrderMessage;
import com.evops.exchange.xml.FuYinOrderReply;
import com.evops.exchange.xml.JaxbConverter;
import com.evops.printorder.entity.PrintOrder;
import com.evops.printorder.mapper.PrintOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 出站发送与失败重试。
 * 登记 PENDING 后立即试投一次：成功转 SENT 并把平台回执原文按入站反向归档；
 * 失败转 RETRYING 按指数退避（base×2^(n-1)，上限 30 分钟）等定时任务重投；
 * 重试次数到顶转 DEAD 转人工，也可手工触发重试。
 */
@Service
public class OutboundSendService {

    private static final Logger log = LoggerFactory.getLogger(OutboundSendService.class);
    private static final long MAX_DELAY_SECONDS = 30 * 60;

    private final ExchangeMessageMapper messageMapper;
    private final PrintOrderMapper printOrderMapper;
    private final PrintOrderXmlMapper xmlMapper;
    private final MessageArchiveService archiveService;
    private final PlatformClient platformClient;
    private final ExchangeProperties properties;

    public OutboundSendService(ExchangeMessageMapper messageMapper,
                               PrintOrderMapper printOrderMapper,
                               PrintOrderXmlMapper xmlMapper,
                               MessageArchiveService archiveService,
                               PlatformClient platformClient,
                               ExchangeProperties properties) {
        this.messageMapper = messageMapper;
        this.printOrderMapper = printOrderMapper;
        this.xmlMapper = xmlMapper;
        this.archiveService = archiveService;
        this.platformClient = platformClient;
        this.properties = properties;
    }

    /** 把一张印单组装成付印报文，登记并立即试投一次（失败留 RETRYING 由定时任务接管） */
    public ExchangeMessage pushOrder(Long orderId) {
        PrintOrder order = printOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("印单不存在或已删除");
        }

        FuYinOrderData data = xmlMapper.toXmlData(order);
        FuYinOrderMessage message = new FuYinOrderMessage();
        message.setHeader(new FuYinOrderMessage.Header(
                "OUT-" + uuid(), "EVOPS", LocalDateTime.now(),
                ExchangeMessageType.PRINT_ORDER.name()));
        message.setBody(data);

        String xml = JaxbConverter.toXml(message);
        ExchangeMessage record = archiveService.register(
                ExchangeDirection.OUT, ExchangeMessageType.PRINT_ORDER,
                message.getHeader().getMessageId(), order.getOrderNo(), null, xml,
                ExchangeMessageStatus.PENDING);

        attempt(record, xml);
        return messageMapper.selectById(record.getId());
    }

    /** 手工重试 DEAD/RETRYING 报文：立即试投一次（失败则重新排期） */
    public ExchangeMessage retryNow(Long messageId) {
        ExchangeMessage record = messageMapper.selectById(messageId);
        if (record == null) {
            throw new BusinessException("报文不存在");
        }
        if (!ExchangeDirection.OUT.name().equals(record.getDirection())) {
            throw new BusinessException("仅出站报文可重试");
        }
        if (ExchangeMessageStatus.SENT.name().equals(record.getStatus())) {
            throw new BusinessException("报文已发送成功，无需重试");
        }
        attempt(record, messageMapper.selectPayloadById(record.getId()));
        return messageMapper.selectById(record.getId());
    }

    /** 定时扫描到期的失败报文重投（测试环境可调大 exchange.retry.scan-interval-ms 关闭干扰） */
    @Scheduled(fixedDelayString = "${exchange.retry.scan-interval-ms:30000}",
               initialDelayString = "${exchange.retry.scan-interval-ms:30000}")
    public void retryDueMessages() {
        List<ExchangeMessage> due = messageMapper.selectList(
                new LambdaQueryWrapper<ExchangeMessage>()
                        .eq(ExchangeMessage::getStatus, ExchangeMessageStatus.RETRYING.name())
                        .isNotNull(ExchangeMessage::getNextRetryTime)
                        .le(ExchangeMessage::getNextRetryTime, LocalDateTime.now())
                        .orderByAsc(ExchangeMessage::getId)
                        .last("LIMIT " + properties.getRetry().getScanLimit()));
        for (ExchangeMessage msg : due) {
            try {
                // payload 不随列表查询，按 ID 取归档原文重投
                attempt(msg, messageMapper.selectPayloadById(msg.getId()));
            } catch (Exception e) {
                // 单条异常不影响整批扫描
                log.error("重试报文异常 messageId={}", msg.getMessageId(), e);
            }
        }
    }

    /**
     * 单次试投并按结果落状态。乐观锁 version 兜底并发（定时任务与手工重试撞车时后到者更新失败即跳过）。
     */
    private void attempt(ExchangeMessage record, String payload) {
        PlatformSendResult result = platformClient.sendPrintOrder(payload);

        if (result.isSuccess()) {
            record.setStatus(ExchangeMessageStatus.SENT.name());
            record.setHttpStatus(result.getHttpStatus());
            record.setErrorMessage(null);
            record.setNextRetryTime(null);
            record.setSentTime(LocalDateTime.now());
            messageMapper.updateById(record);

            // 平台回执原文按入站反向归档（回执自身带流水号，重复回执会被幂等约束挡下）
            archiveReply(result.getReplyPayload(), record.getOrderNo());
            log.info("付印报文发送成功 orderNo={} messageId={} http={}",
                    record.getOrderNo(), record.getMessageId(), result.getHttpStatus());
            return;
        }

        int retried = (record.getRetryCount() == null ? 0 : record.getRetryCount()) + 1;
        int max = record.getMaxRetry() == null ? properties.getRetry().getMaxRetry() : record.getMaxRetry();
        record.setHttpStatus(result.getHttpStatus());
        record.setErrorMessage(result.getErrorMessage());
        record.setRetryCount(retried);
        record.setSentTime(LocalDateTime.now());
        if (retried >= max) {
            record.setStatus(ExchangeMessageStatus.DEAD.name());
            record.setNextRetryTime(null);
            log.error("付印报文重试到顶转 DEAD orderNo={} messageId={} retried={}",
                    record.getOrderNo(), record.getMessageId(), retried);
        } else {
            record.setStatus(ExchangeMessageStatus.RETRYING.name());
            record.setNextRetryTime(LocalDateTime.now().plusSeconds(backoffSeconds(retried)));
            log.warn("付印报文发送失败将重试 orderNo={} messageId={} retried={}/{} next={}",
                    record.getOrderNo(), record.getMessageId(), retried, max, record.getNextRetryTime());
        }
        messageMapper.updateById(record);
    }

    /** 平台回执原文反向往档；归档失败不影响发送成功结论，仅告警 */
    private void archiveReply(String replyPayload, String orderNo) {
        if (replyPayload == null || replyPayload.trim().isEmpty()) {
            return;
        }
        try {
            FuYinOrderReply reply = JaxbConverter.fromXml(replyPayload, FuYinOrderReply.class);
            archiveService.register(
                    ExchangeDirection.IN, ExchangeMessageType.PRINT_ORDER_REPLY,
                    reply.getMessageId(), orderNo, reply.getRefMessageId(),
                    replyPayload, ExchangeMessageStatus.RECEIVED);
        } catch (org.springframework.dao.DataIntegrityViolationException dup) {
            // 平台对同一投递重复回执：幂等约束挡下，发送本身仍算成功
            log.info("平台回执重复，已按幂等约束忽略: {}", orderNo);
        } catch (Exception e) {
            log.warn("平台回执归档失败，仅跳过回执归档（发送本身已成功）: {}", e.getMessage());
        }
    }

    /** 指数退避：base×2^(n-1)，上限 30 分钟 */
    private long backoffSeconds(int retryCount) {
        long base = properties.getRetry().getBaseDelaySeconds();
        long delay = base;
        for (int i = 1; i < retryCount; i++) {
            delay *= 2;
            if (delay >= MAX_DELAY_SECONDS) {
                return MAX_DELAY_SECONDS;
            }
        }
        return Math.min(delay, MAX_DELAY_SECONDS);
    }

    private static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
