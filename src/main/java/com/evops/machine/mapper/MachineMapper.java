package com.evops.machine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evops.machine.entity.Machine;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface MachineMapper extends BaseMapper<Machine> {

    /**
     * 按机台号统计全量行数（含已逻辑删除）。
     * 机台号唯一索引为全量约束：号一旦建档永久占用，重复建档一律拒绝。
     */
    @Select("SELECT COUNT(1) FROM t_machine WHERE machine_code = #{machineCode}")
    long countAllByMachineCode(@Param("machineCode") String machineCode);
}
