package com.evops.machine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evops.common.BusinessException;
import com.evops.common.PageResult;
import com.evops.machine.dto.MachineCreateRequest;
import com.evops.machine.dto.MachineQuery;
import com.evops.machine.entity.Machine;
import com.evops.machine.mapper.MachineMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MachineService {

    private final MachineMapper machineMapper;

    public MachineService(MachineMapper machineMapper) {
        this.machineMapper = machineMapper;
    }

    /** 建档：机台号唯一，重复直接拒绝并提示已存在 */
    @Transactional(rollbackFor = Exception.class)
    public Machine create(MachineCreateRequest req) {
        // 唯一判断以全量计数为准（含已删除，机台号一旦建档永久占用）
        if (machineMapper.countAllByMachineCode(req.getMachineCode()) > 0) {
            throw new BusinessException("机台号 " + req.getMachineCode() + " 已存在，请勿重复建档");
        }

        Machine machine = new Machine();
        machine.setMachineCode(req.getMachineCode());
        machine.setMachineName(req.getMachineName());
        machine.setMachineType(req.getMachineType());
        machine.setMaxSheetSize(req.getMaxSheetSize());
        machine.setRemark(req.getRemark());
        machine.setStatus("ACTIVE");
        machineMapper.insert(machine);
        return machine;
    }

    /** 条件分页查询（逻辑删除行由 MyBatis-Plus 自动过滤） */
    public PageResult<Machine> page(MachineQuery query) {
        Page<Machine> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Machine> wrapper = new LambdaQueryWrapper<Machine>()
                .eq(StringUtils.hasText(query.getMachineCode()), Machine::getMachineCode, query.getMachineCode())
                .like(StringUtils.hasText(query.getMachineName()), Machine::getMachineName, query.getMachineName())
                .eq(StringUtils.hasText(query.getMachineType()), Machine::getMachineType, query.getMachineType())
                .eq(StringUtils.hasText(query.getStatus()), Machine::getStatus, query.getStatus())
                .orderByDesc(Machine::getId);
        Page<Machine> result = machineMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    public Machine getById(Long id) {
        Machine machine = machineMapper.selectById(id);
        if (machine == null) {
            throw new BusinessException("机台不存在或已删除");
        }
        return machine;
    }

    /** 删除只打标记（@TableLogic 自动 UPDATE deleted=1） */
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        Machine machine = getById(id);
        machineMapper.deleteById(machine.getId());
    }
}
