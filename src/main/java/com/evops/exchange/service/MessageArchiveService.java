package com.evops.exchange.service;

import com.evops.exchange.config.ExchangeProperties;
import com.evops.exchange.entity.ExchangeMessage;
import com.evops.exchange.enums.ExchangeDirection;
import com.evops.exchange.enums.ExchangeMessageStatus;
import com.evops.exchange.enums.ExchangeMessageType;
import com.evops.exchange.mapper.ExchangeMessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 报文归档登记：payload 原文整存数据库（排障以原文为准），同时按印单号落一份 XML 文件。
 * 目录结构：{归档根目录}/{印单号}/{时间}-{方向}-{类型}-{流水号}.xml。
 * 数据库为权威原文；文件归档失败只告警不阻断交换（文件可事后用 payload 重放补齐）。
 */
@Service
public class MessageArchiveService {

    private static final Logger log = LoggerFactory.getLogger(MessageArchiveService.class);
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ExchangeMessageMapper messageMapper;
    private final ExchangeProperties properties;

    public MessageArchiveService(ExchangeMessageMapper messageMapper, ExchangeProperties properties) {
        this.messageMapper = messageMapper;
        this.properties = properties;
    }

    /** 登记一条新报文并归档（同事务落库；文件在落库后写出） */
    @Transactional(rollbackFor = Exception.class)
    public ExchangeMessage register(ExchangeDirection direction,
                                    ExchangeMessageType type,
                                    String messageId,
                                    String orderNo,
                                    String refMessageId,
                                    String payload,
                                    ExchangeMessageStatus status) {
        ExchangeMessage msg = new ExchangeMessage();
        msg.setMessageId(messageId);
        msg.setDirection(direction.name());
        msg.setMessageType(type.name());
        msg.setOrderNo(orderNo == null ? "UNKNOWN" : orderNo);
        msg.setRefMessageId(refMessageId);
        msg.setPayload(payload);
        msg.setPayloadDigest(PayloadDigests.sha256(payload));
        msg.setStatus(status.name());
        msg.setRetryCount(0);
        msg.setMaxRetry(properties.getRetry().getMaxRetry());
        messageMapper.insert(msg);

        String relative = writeFile(msg, payload);
        if (relative != null) {
            msg.setArchivePath(relative);
            messageMapper.updateById(msg);
        }
        return msg;
    }

    /** 重写归档文件（状态/重试变化不改写文件，仅登记时写一次；此处供补档使用） */
    private String writeFile(ExchangeMessage msg, String payload) {
        try {
            String safeOrderNo = sanitize(msg.getOrderNo());
            String fileName = LocalDateTime.now().format(FILE_TS)
                    + "-" + msg.getDirection()
                    + "-" + msg.getMessageType()
                    + "-" + sanitize(msg.getMessageId()) + ".xml";
            Path dir = Paths.get(properties.getArchive().getDir(), safeOrderNo);
            Files.createDirectories(dir);
            Path file = dir.resolve(fileName);
            Files.write(file, payload.getBytes(StandardCharsets.UTF_8));
            return Paths.get(properties.getArchive().getDir()).relativize(file).toString().replace('\\', '/');
        } catch (IOException e) {
            // 数据库已留原文，文件失败不阻断业务，告警后人工补档
            log.error("报文文件归档失败 orderNo={} messageId={}", msg.getOrderNo(), msg.getMessageId(), e);
            return null;
        }
    }

    /** 文件名/目录名安全化：路径分隔符等一律替换，避免目录穿越 */
    private static String sanitize(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "UNKNOWN";
        }
        return raw.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
