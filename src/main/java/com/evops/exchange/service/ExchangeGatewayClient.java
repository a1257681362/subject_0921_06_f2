package com.evops.exchange.service;

/**
 * 客户自助下单平台发送通道。
 * 实现失败必须抛异常，由 ExchangeService 落 FAILED 状态并允许重试。
 */
public interface ExchangeGatewayClient {

    /** 发送报文原文到客户自助下单平台；失败抛异常 */
    void send(String xml);
}
