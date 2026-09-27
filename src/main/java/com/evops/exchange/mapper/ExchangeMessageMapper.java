package com.evops.exchange.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evops.exchange.entity.ExchangeMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ExchangeMessageMapper extends BaseMapper<ExchangeMessage> {

    /** 幂等查询：同方向同流水号（全量，含已拒绝/已删除留痕） */
    @Select("SELECT COUNT(1) FROM t_exchange_message WHERE direction = #{direction} AND message_id = #{messageId}")
    long countByDirectionAndMessageId(@Param("direction") String direction,
                                      @Param("messageId") String messageId);

    /** 取报文原文（payload 默认不随实体查询，专用端点/重试用） */
    @Select("SELECT payload FROM t_exchange_message WHERE id = #{id}")
    String selectPayloadById(@Param("id") Long id);
}
