package com.evops.exchange.enums;

/**
 * 报文状态：
 * PENDING   出站待发送
 * SENT      出站已发送成功（同步回执随 HTTP 返回也算已送达）
 * RECEIVED  入站已受理并落库
 * RETRYING  出站发送失败，等待退避重试
 * DEAD      超过最大重试次数，转人工
 * REJECTED  入站报文解析/业务拒绝（重复印单号、机台号不存在等）
 */
public enum ExchangeMessageStatus {
    PENDING,
    SENT,
    RECEIVED,
    RETRYING,
    DEAD,
    REJECTED
}
