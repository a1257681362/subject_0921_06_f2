package com.evops.exchange.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;

import java.time.LocalDateTime;

/**
 * 客户自助下单平台交换报文登记。
 * payload 为报文原文（整存）；按 order_no 归档检索；出站失败退避重试。
 */
@TableName("t_exchange_message")
public class ExchangeMessage extends BaseEntity {
    private String messageId;
    /** IN / OUT */
    private String direction;
    /** PRINT_ORDER / PRINT_ORDER_REPLY */
    private String messageType;
    private String orderNo;
    private String refMessageId;

    /** 报文原文（UTF-8 XML 原样整存）；CLOB 较大，列表/详情默认不取，走 /payload 专用端点 */
    @TableField(select = false)
    private String payload;
    /** 原文 SHA-256 摘要 */
    private String payloadDigest;
    /** 文件归档相对路径（按印单号建目录） */
    private String archivePath;

    /** PENDING/SENT/RECEIVED/RETRYING/DEAD/REJECTED */
    private String status;
    private Integer httpStatus;
    /** 成功转 SENT / 转 DEAD 时需清空，故始终参与更新（默认策略会忽略 null） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String errorMessage;
    /** 累计失败投递次数 */
    private Integer retryCount;
    private Integer maxRetry;
    /** 成功/转 DEAD 时需清空，故始终参与更新 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime nextRetryTime;
    private LocalDateTime sentTime;

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getRefMessageId() { return refMessageId; }
    public void setRefMessageId(String refMessageId) { this.refMessageId = refMessageId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getPayloadDigest() { return payloadDigest; }
    public void setPayloadDigest(String payloadDigest) { this.payloadDigest = payloadDigest; }
    public String getArchivePath() { return archivePath; }
    public void setArchivePath(String archivePath) { this.archivePath = archivePath; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public Integer getMaxRetry() { return maxRetry; }
    public void setMaxRetry(Integer maxRetry) { this.maxRetry = maxRetry; }
    public LocalDateTime getNextRetryTime() { return nextRetryTime; }
    public void setNextRetryTime(LocalDateTime nextRetryTime) { this.nextRetryTime = nextRetryTime; }
    public LocalDateTime getSentTime() { return sentTime; }
    public void setSentTime(LocalDateTime sentTime) { this.sentTime = sentTime; }
}
