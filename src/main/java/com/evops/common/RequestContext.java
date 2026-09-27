package com.evops.common;

/**
 * 基于 ThreadLocal 的请求上下文：请求号与操作人随一次请求贯穿 Service/Mapper，
 * 由审计字段自动填充器读取并落库。
 */
public final class RequestContext {
    private static final ThreadLocal<Ctx> HOLDER = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void set(Ctx ctx) {
        HOLDER.set(ctx);
    }

    public static Ctx get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static class Ctx {
        private final String requestNo;
        private final Long operatorId;
        private final String operatorName;

        public Ctx(String requestNo, Long operatorId, String operatorName) {
            this.requestNo = requestNo;
            this.operatorId = operatorId;
            this.operatorName = operatorName;
        }

        public String getRequestNo() {
            return requestNo;
        }

        public Long getOperatorId() {
            return operatorId;
        }

        public String getOperatorName() {
            return operatorName;
        }
    }
}
