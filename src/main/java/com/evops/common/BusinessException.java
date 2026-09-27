package com.evops.common;

/**
 * 业务异常：用于参数校验失败、重复建档拒绝、状态不允许等可预期的业务拒绝。
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
