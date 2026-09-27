package com.evops.exchange.dto;

/**
 * 报文归档检索：按印单号（+方向/状态）组合过滤。
 */
public class ExchangeMessageQuery {
    private String orderNo;
    private String direction;
    private String status;
    private Integer pageNum = 1;
    private Integer pageSize = 20;

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
