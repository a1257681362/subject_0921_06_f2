package com.evops.exchange.dto;

/** 报文归档查询条件：按印单号 + 方向/状态/类型组合分页 */
public class ExchangeMessageQuery {
    private String orderNo;
    /** IN / OUT */
    private String direction;
    private String status;
    private String messageType;
    private Integer pageNum = 1;
    private Integer pageSize = 20;

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
