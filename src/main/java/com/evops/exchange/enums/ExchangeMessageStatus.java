package com.evops.exchange.enums;

/** 报文状态：PENDING 待发送 / SENT 已发送 / FAILED 发送失败（可重试）/ RECEIVED 已接收 */
public enum ExchangeMessageStatus {
    PENDING,
    SENT,
    FAILED,
    RECEIVED
}
