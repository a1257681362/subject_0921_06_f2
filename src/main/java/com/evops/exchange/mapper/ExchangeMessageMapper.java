package com.evops.exchange.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evops.exchange.entity.ExchangeMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ExchangeMessageMapper extends BaseMapper<ExchangeMessage> {

    /**
     * 按报文ID + 方向统计全量行数（含已逻辑删除）。
     * 报文ID按方向唯一：平台重复推送同一报文直接拒绝；
     * 发出与接收各自归档（如联调自环时同一报文双向留痕）。
     */
    @Select("SELECT COUNT(1) FROM t_exchange_message WHERE message_id = #{messageId} AND direction = #{direction}")
    long countAllByMessageIdAndDirection(@Param("messageId") String messageId, @Param("direction") String direction);
}
