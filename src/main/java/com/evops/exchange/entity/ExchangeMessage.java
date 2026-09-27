package com.evops.exchange.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;

import java.time.LocalDateTime;

/**
 * 交换报文归档（与客户自助下单平台对接）。
 * 报文留原文：xml_content 存发出/接收的 XML 原文；按印单号归档检索；
 * 发送失败记 FAILED 与 last_error，重试按归档原文重发。
 */
@TableName("t_exchange_message")
public class ExchangeMessage extends BaseEntity {
    private String messageId;
    private String orderNo;
    private String direction;
    private String messageType;
    private String status;
    private String xmlContent;
    private Integer retryCount;
    private String lastError;
    private LocalDateTime sentTime;

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getXmlContent() { return xmlContent; }
    public void setXmlContent(String xmlContent) { this.xmlContent = xmlContent; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public LocalDateTime getSentTime() { return sentTime; }
    public void setSentTime(LocalDateTime sentTime) { this.sentTime = sentTime; }
}
