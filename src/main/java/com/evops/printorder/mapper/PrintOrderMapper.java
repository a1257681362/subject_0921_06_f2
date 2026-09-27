package com.evops.printorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.evops.printorder.entity.PrintOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PrintOrderMapper extends BaseMapper<PrintOrder> {

    /**
     * 按印单号统计全量行数（含已逻辑删除）。
     * 印单号唯一索引为全量约束：号一旦建档永久占用，重复建档一律拒绝。
     */
    @Select("SELECT COUNT(1) FROM t_print_order WHERE order_no = #{orderNo}")
    long countAllByOrderNo(@Param("orderNo") String orderNo);
}
