package com.evops.exchange.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.LocalDateTime;

/**
 * 付印数据报文：报文头（流水号/发送方/时间/类型）+ 报文体。
 * 出站、入站同一结构，保证往返一致。
 */
@XmlRootElement(name = "FuYinMessage")
@XmlAccessorType(XmlAccessType.FIELD)
public class FuYinOrderMessage {

    @XmlElement(name = "header", required = true)
    private Header header;

    @XmlElement(name = "body", required = true)
    private FuYinOrderData body;

    public Header getHeader() { return header; }
    public void setHeader(Header header) { this.header = header; }
    public FuYinOrderData getBody() { return body; }
    public void setBody(FuYinOrderData body) { this.body = body; }

    /** 报文头 */
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Header {
        /** 报文流水号，幂等键 */
        @XmlElement(name = "messageId", required = true)
        private String messageId;

        /** 发送方系统标识，如 SELF-PORTAL / EVOPS */
        @XmlElement(name = "sender")
        private String sender;

        @XmlElement(name = "sendTime")
        private LocalDateTime sendTime;

        /** 报文类型，固定 PRINT_ORDER */
        @XmlElement(name = "messageType")
        private String messageType;

        public Header() {
        }

        public Header(String messageId, String sender, LocalDateTime sendTime, String messageType) {
            this.messageId = messageId;
            this.sender = sender;
            this.sendTime = sendTime;
            this.messageType = messageType;
        }

        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }
        public LocalDateTime getSendTime() { return sendTime; }
        public void setSendTime(LocalDateTime sendTime) { this.sendTime = sendTime; }
        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
    }
}
