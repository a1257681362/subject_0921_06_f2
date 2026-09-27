package com.evops.exchange.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.LocalDateTime;

/**
 * 受理回执：指回原付印报文 messageId 与印单号，给出 ACCEPTED/REJECTED 及原因。
 */
@XmlRootElement(name = "FuYinReply")
@XmlAccessorType(XmlAccessType.FIELD)
public class FuYinOrderReply {

    @XmlElement(name = "messageId", required = true)
    private String messageId;

    /** 原付印报文流水号 */
    @XmlElement(name = "refMessageId", required = true)
    private String refMessageId;

    @XmlElement(name = "orderNo", required = true)
    private String orderNo;

    /** ACCEPTED / REJECTED */
    @XmlElement(name = "result", required = true)
    private String result;

    @XmlElement(name = "reason")
    private String reason;

    @XmlElement(name = "replyTime")
    private LocalDateTime replyTime;

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getRefMessageId() { return refMessageId; }
    public void setRefMessageId(String refMessageId) { this.refMessageId = refMessageId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getReplyTime() { return replyTime; }
    public void setReplyTime(LocalDateTime replyTime) { this.replyTime = replyTime; }
}
