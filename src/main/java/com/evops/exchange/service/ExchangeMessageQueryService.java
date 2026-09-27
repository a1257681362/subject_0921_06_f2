package com.evops.exchange.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evops.common.BusinessException;
import com.evops.common.PageResult;
import com.evops.exchange.dto.ExchangeMessageQuery;
import com.evops.exchange.entity.ExchangeMessage;
import com.evops.exchange.mapper.ExchangeMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 报文归档检索与原文调阅。 */
@Service
public class ExchangeMessageQueryService {

    private final ExchangeMessageMapper messageMapper;

    public ExchangeMessageQueryService(ExchangeMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    public PageResult<ExchangeMessage> page(ExchangeMessageQuery query) {
        Page<ExchangeMessage> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<ExchangeMessage> wrapper = new LambdaQueryWrapper<ExchangeMessage>()
                .eq(StringUtils.hasText(query.getOrderNo()), ExchangeMessage::getOrderNo, query.getOrderNo())
                .eq(StringUtils.hasText(query.getDirection()), ExchangeMessage::getDirection, query.getDirection())
                .eq(StringUtils.hasText(query.getStatus()), ExchangeMessage::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getMessageType()), ExchangeMessage::getMessageType, query.getMessageType())
                .orderByDesc(ExchangeMessage::getId);
        Page<ExchangeMessage> result = messageMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    public ExchangeMessage getById(Long id) {
        ExchangeMessage msg = messageMapper.selectById(id);
        if (msg == null) {
            throw new BusinessException("报文不存在");
        }
        return msg;
    }

    /** 调阅报文原文（留底原文整存取回） */
    public String getPayload(Long id) {
        ExchangeMessage msg = getById(id);
        String payload = messageMapper.selectPayloadById(id);
        if (payload == null) {
            throw new BusinessException("报文原文缺失 messageId=" + msg.getMessageId());
        }
        return payload;
    }
}
