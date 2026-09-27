package com.evops.exchange.client;

/** 平台单次发送结果 */
public class PlatformSendResult {
    private final boolean success;
    private final int httpStatus;
    private final String replyPayload;
    private final String errorMessage;

    private PlatformSendResult(boolean success, int httpStatus, String replyPayload, String errorMessage) {
        this.success = success;
        this.httpStatus = httpStatus;
        this.replyPayload = replyPayload;
        this.errorMessage = errorMessage;
    }

    public static PlatformSendResult ok(int httpStatus, String replyPayload) {
        return new PlatformSendResult(true, httpStatus, replyPayload, null);
    }

    public static PlatformSendResult fail(int httpStatus, String errorMessage) {
        return new PlatformSendResult(false, httpStatus, null, errorMessage);
    }

    public boolean isSuccess() { return success; }
    public int getHttpStatus() { return httpStatus; }
    public String getReplyPayload() { return replyPayload; }
    public String getErrorMessage() { return errorMessage; }
}
