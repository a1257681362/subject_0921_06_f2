package com.evops.exchange.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evops.common.BusinessException;
import com.evops.machine.entity.Machine;
import com.evops.machine.mapper.MachineMapper;
import com.evops.printorder.dto.PrintOrderCreateRequest;
import com.evops.exchange.xml.FuYinOrderData;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 付印报文体 与 印单建档请求 互转。
 * 机台：报文给机台号，台账解析成 machine_id；机台号不存在直接业务拒绝。
 * 金额、费率、日期原样承载，不在转换环节丢字段。
 */
@Component
public class PrintOrderXmlMapper {

    private final MachineMapper machineMapper;

    public PrintOrderXmlMapper(MachineMapper machineMapper) {
        this.machineMapper = machineMapper;
    }

    /** 入站：报文体 -> 建档请求（机台号 -> 机台ID） */
    public PrintOrderCreateRequest toCreateRequest(FuYinOrderData data) {
        PrintOrderCreateRequest req = new PrintOrderCreateRequest();
        req.setOrderNo(data.getOrderNo());
        req.setCustomerName(data.getCustomerName());
        req.setProductName(data.getProductName());
        req.setPrintDate(data.getPrintDate());
        req.setPrintQty(data.getPrintQty());
        req.setKai(data.getKai());
        req.setPaperLengthMm(data.getPaperLengthMm());
        req.setPaperWidthMm(data.getPaperWidthMm());
        req.setGsm(data.getGsm());
        req.setWasteRate(data.getWasteRate());
        req.setAddRate(data.getAddRate());
        req.setAmount(data.getAmount());
        req.setRemark(data.getRemark());

        if (StringUtils.hasText(data.getMachineCode())) {
            Machine machine = machineMapper.selectOne(
                    new LambdaQueryWrapper<Machine>()
                            .eq(Machine::getMachineCode, data.getMachineCode())
                            .last("LIMIT 1"));
            if (machine == null) {
                throw new BusinessException("机台号 " + data.getMachineCode() + " 在台账中不存在，无法排产");
            }
            req.setMachineId(machine.getId());
        }
        return req;
    }

    /** 出站：印单实体 -> 报文体（机台ID -> 机台号） */
    public FuYinOrderData toXmlData(com.evops.printorder.entity.PrintOrder order) {
        FuYinOrderData data = new FuYinOrderData();
        data.setOrderNo(order.getOrderNo());
        data.setCustomerName(order.getCustomerName());
        data.setProductName(order.getProductName());
        data.setPrintDate(order.getPrintDate());
        data.setPrintQty(order.getPrintQty());
        data.setKai(order.getKai());
        data.setPaperLengthMm(order.getPaperLengthMm());
        data.setPaperWidthMm(order.getPaperWidthMm());
        data.setGsm(order.getGsm());
        data.setWasteRate(order.getWasteRate());
        data.setAddRate(order.getAddRate());
        data.setAmount(order.getAmount());
        data.setRemark(order.getRemark());

        if (order.getMachineId() != null) {
            Machine machine = machineMapper.selectById(order.getMachineId());
            if (machine != null) {
                data.setMachineCode(machine.getMachineCode());
            }
        }
        return data;
    }
}
