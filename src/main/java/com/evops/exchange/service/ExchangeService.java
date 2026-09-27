package com.evops.exchange.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evops.common.BusinessException;
import com.evops.common.PageResult;
import com.evops.exchange.dto.ExchangeMessageQuery;
import com.evops.exchange.entity.ExchangeMessage;
import com.evops.exchange.enums.ExchangeDirection;
import com.evops.exchange.enums.ExchangeMessageStatus;
import com.evops.exchange.enums.ExchangeMessageType;
import com.evops.exchange.mapper.ExchangeMessageMapper;
import com.evops.exchange.message.PrintOrderMessage;
import com.evops.exchange.message.PrintOrderMessageCodec;
import com.evops.machine.entity.Machine;
import com.evops.machine.mapper.MachineMapper;
import com.evops.printorder.entity.PrintOrder;
import com.evops.printorder.enums.PrintOrderStatus;
import com.evops.printorder.mapper.PrintOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 付印数据交换：报文生成、发送、重试、接收、归档检索。
 * 归档（留原文）与发送分步提交：先把报文原文落库，再调客户平台，按结果更新状态——
 * 发送耗时不裹在数据库事务里，失败时原文已留档，随时可重试。
 */
@Service
public class ExchangeService {

    /** 无法识别印单号的来文（如解析失败）归档占位，便于对账排查 */
    private static final String UNKNOWN_ORDER_NO = "UNKNOWN";

    private final PrintOrderMapper printOrderMapper;
    private final MachineMapper machineMapper;
    private final ExchangeMessageMapper exchangeMessageMapper;
    private final PrintOrderMessageCodec codec;
    private final ExchangeGatewayClient gatewayClient;

    public ExchangeService(PrintOrderMapper printOrderMapper,
                           MachineMapper machineMapper,
                           ExchangeMessageMapper exchangeMessageMapper,
                           PrintOrderMessageCodec codec,
                           ExchangeGatewayClient gatewayClient) {
        this.printOrderMapper = printOrderMapper;
        this.machineMapper = machineMapper;
        this.exchangeMessageMapper = exchangeMessageMapper;
        this.codec = codec;
        this.gatewayClient = gatewayClient;
    }

    /**
     * 生成付印报文并发送：先归档留原文（PENDING），再调客户平台，按结果更新 SENT/FAILED。
     * 作废印单不允许发送。
     */
    public ExchangeMessage sendPrintOrder(Long orderId) {
        PrintOrder order = printOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("印单不存在或已删除");
        }
        if (PrintOrderStatus.VOID.name().equals(order.getStatus())) {
            throw new BusinessException("印单 " + order.getOrderNo() + " 已作废，不可发送付印报文");
        }

        PrintOrderMessage message = buildMessage(order);
        String xml = codec.toXml(message);

        ExchangeMessage archived = newArchive(message, xml,
                ExchangeDirection.OUTBOUND, ExchangeMessageStatus.PENDING);
        exchangeMessageMapper.insert(archived);

        return dispatch(archived);
    }

    /**
     * 失败重试：重发归档原文（不重新生成报文，保证发的就是留的），重试次数累加。
     * 仅待发送/发送失败的发出报文可重试。
     */
    public ExchangeMessage retry(Long messageId) {
        ExchangeMessage message = getById(messageId);
        if (!ExchangeDirection.OUTBOUND.name().equals(message.getDirection())) {
            throw new BusinessException("接收报文无需重试");
        }
        if (ExchangeMessageStatus.SENT.name().equals(message.getStatus())) {
            throw new BusinessException("报文 " + message.getMessageId() + " 已发送成功，无需重试");
        }
        message.setRetryCount(message.getRetryCount() == null ? 1 : message.getRetryCount() + 1);
        return dispatch(message);
    }

    /** 发送并按结果更新状态：成功 SENT 记发送时间，失败 FAILED 记原因（可再重试） */
    private ExchangeMessage dispatch(ExchangeMessage message) {
        try {
            gatewayClient.send(message.getXmlContent());
            message.setStatus(ExchangeMessageStatus.SENT.name());
            message.setSentTime(LocalDateTime.now());
            message.setLastError(null);
        } catch (Exception e) {
            message.setStatus(ExchangeMessageStatus.FAILED.name());
            message.setLastError(e.getMessage());
        }
        exchangeMessageMapper.updateById(message);
        return message;
    }

    /**
     * 接收平台来文：解析校验 → 按印单号归档留原文（RECEIVED）。
     * 解析失败、缺报文头/印单号的来文同样留原文归档（FAILED，印单号记 UNKNOWN），便于对账排查。
     * 同一报文ID重复推送直接拒绝。
     */
    public PrintOrderMessage receive(String xml) {
        PrintOrderMessage parsed;
        try {
            parsed = codec.parse(xml);
        } catch (BusinessException ex) {
            exchangeMessageMapper.insert(failedArchive(xml, ex.getMessage()));
            throw ex;
        }
        if (parsed.getHeader() == null || !StringUtils.hasText(parsed.getHeader().getMessageId())) {
            BusinessException ex = new BusinessException("报文头缺少报文ID");
            exchangeMessageMapper.insert(failedArchive(xml, ex.getMessage()));
            throw ex;
        }
        if (!StringUtils.hasText(parsed.getOrderNo())) {
            BusinessException ex = new BusinessException("报文缺少印单号");
            exchangeMessageMapper.insert(failedArchive(xml, ex.getMessage()));
            throw ex;
        }
        String messageId = parsed.getHeader().getMessageId();
        if (exchangeMessageMapper.countAllByMessageIdAndDirection(
                messageId, ExchangeDirection.INBOUND.name()) > 0) {
            throw new BusinessException("报文 " + messageId + " 已接收，请勿重复推送");
        }

        exchangeMessageMapper.insert(newArchive(parsed, xml,
                ExchangeDirection.INBOUND, ExchangeMessageStatus.RECEIVED));
        return parsed;
    }

    /** 按印单号（+方向/状态）组合检索归档报文，分页 */
    public PageResult<ExchangeMessage> page(ExchangeMessageQuery query) {
        Page<ExchangeMessage> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<ExchangeMessage> wrapper = new LambdaQueryWrapper<ExchangeMessage>()
                .eq(StringUtils.hasText(query.getOrderNo()), ExchangeMessage::getOrderNo, query.getOrderNo())
                .eq(StringUtils.hasText(query.getDirection()), ExchangeMessage::getDirection, query.getDirection())
                .eq(StringUtils.hasText(query.getStatus()), ExchangeMessage::getStatus, query.getStatus())
                .orderByDesc(ExchangeMessage::getId);
        Page<ExchangeMessage> result = exchangeMessageMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    public ExchangeMessage getById(Long id) {
        ExchangeMessage message = exchangeMessageMapper.selectById(id);
        if (message == null) {
            throw new BusinessException("报文不存在或已删除");
        }
        return message;
    }

    /** 由印单组报文：机台号取台账机台号（机台删除则留空），金额类字段以来文为准（建档未采集） */
    private PrintOrderMessage buildMessage(PrintOrder order) {
        PrintOrderMessage message = new PrintOrderMessage();
        PrintOrderMessage.Header header = new PrintOrderMessage.Header();
        header.setMessageId(newMessageId());
        header.setMessageType(ExchangeMessageType.PRINT_ORDER.name());
        header.setSentTime(LocalDateTime.now());
        message.setHeader(header);

        message.setOrderNo(order.getOrderNo());
        message.setCustomerName(order.getCustomerName());
        message.setProductName(order.getProductName());
        message.setPrintDate(order.getPrintDate());
        message.setPrintQty(order.getPrintQty());
        message.setKai(order.getKai());
        message.setPaperLengthMm(order.getPaperLengthMm());
        message.setPaperWidthMm(order.getPaperWidthMm());
        message.setGsm(order.getGsm());
        message.setWasteRate(order.getWasteRate());
        message.setAddRate(order.getAddRate());
        message.setBaseSheets(order.getBaseSheets());
        message.setWasteSheets(order.getWasteSheets());
        message.setAddSheets(order.getAddSheets());
        message.setTotalSheets(order.getTotalSheets());
        message.setPaperWeightKg(order.getPaperWeightKg());
        message.setStatus(order.getStatus());
        message.setRemark(order.getRemark());

        if (order.getMachineId() != null) {
            Machine machine = machineMapper.selectById(order.getMachineId());
            if (machine != null) {
                message.setMachineCode(machine.getMachineCode());
            }
        }
        return message;
    }

    private ExchangeMessage newArchive(PrintOrderMessage message, String xml,
                                       ExchangeDirection direction, ExchangeMessageStatus status) {
        ExchangeMessage archive = new ExchangeMessage();
        archive.setMessageId(message.getHeader().getMessageId());
        archive.setOrderNo(message.getOrderNo());
        archive.setDirection(direction.name());
        archive.setMessageType(StringUtils.hasText(message.getHeader().getMessageType())
                ? message.getHeader().getMessageType() : ExchangeMessageType.UNKNOWN.name());
        archive.setStatus(status.name());
        archive.setXmlContent(xml);
        archive.setRetryCount(0);
        return archive;
    }

    /** 无法解析/校验不过的来文归档：留原文、记原因，印单号与类型记 UNKNOWN */
    private ExchangeMessage failedArchive(String xml, String error) {
        ExchangeMessage archive = new ExchangeMessage();
        archive.setMessageId(newMessageId());
        archive.setOrderNo(UNKNOWN_ORDER_NO);
        archive.setDirection(ExchangeDirection.INBOUND.name());
        archive.setMessageType(ExchangeMessageType.UNKNOWN.name());
        archive.setStatus(ExchangeMessageStatus.FAILED.name());
        archive.setXmlContent(xml);
        archive.setRetryCount(0);
        archive.setLastError(error);
        return archive;
    }

    private String newMessageId() {
        return "MSG" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
