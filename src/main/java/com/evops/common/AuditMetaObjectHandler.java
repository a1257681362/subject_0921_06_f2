package com.evops.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计字段自动填充：请求号、操作人、时间、版本随写落库。
 * insert 填全套；update 刷新 update_by / update_time。
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        RequestContext.Ctx ctx = RequestContext.get();
        String requestNo = ctx == null ? null : ctx.getRequestNo();
        Long operatorId = ctx == null ? null : ctx.getOperatorId();
        String operatorName = ctx == null ? null : ctx.getOperatorName();
        LocalDateTime now = LocalDateTime.now();

        this.strictInsertFill(metaObject, "requestNo", String.class, requestNo);
        this.strictInsertFill(metaObject, "createBy", Long.class, operatorId);
        this.strictInsertFill(metaObject, "operatorName", String.class, operatorName);
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateBy", Long.class, operatorId);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "version", Integer.class, 0);
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        RequestContext.Ctx ctx = RequestContext.get();
        Long operatorId = ctx == null ? null : ctx.getOperatorId();

        // 更新强制覆盖：实体从库里查出时 updateBy/updateTime 已有值，
        // strictUpdateFill 遇非空会跳过，这里直接 setFieldValByName 保证刷新为本次操作人与时间。
        if (metaObject.hasSetter("updateBy")) {
            this.setFieldValByName("updateBy", operatorId, metaObject);
        }
        if (metaObject.hasSetter("updateTime")) {
            this.setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
        }
    }
}
