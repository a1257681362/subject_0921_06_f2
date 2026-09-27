package com.evops.printorder.dto;

import javax.validation.constraints.Size;

/** 作废请求：作废走反向记录，需留原因以保留追溯链 */
public class PrintOrderVoidRequest {
    @Size(max = 500, message = "作废原因最长500字")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
