package com.evops.printorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;

/**
 * 印单追溯链记录。建档写 CREATE 正向记录；作废写 VOID 反向记录并以 reverse_of_id 指回原记录。
 */
@TableName("t_print_order_record")
public class PrintOrderRecord extends BaseEntity {
    private Long orderId;
    private String orderNo;
    private String recordType;
    private Long reverseOfId;
    private String reason;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }
    public Long getReverseOfId() { return reverseOfId; }
    public void setReverseOfId(Long reverseOfId) { this.reverseOfId = reverseOfId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
