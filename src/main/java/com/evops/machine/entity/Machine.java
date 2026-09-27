package com.evops.machine.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;

/** 机台台账 */
@TableName("t_machine")
public class Machine extends BaseEntity {
    private String machineCode;
    private String machineName;
    private String machineType;
    private String maxSheetSize;
    private String status;
    private String remark;

    public String getMachineCode() { return machineCode; }
    public void setMachineCode(String machineCode) { this.machineCode = machineCode; }
    public String getMachineName() { return machineName; }
    public void setMachineName(String machineName) { this.machineName = machineName; }
    public String getMachineType() { return machineType; }
    public void setMachineType(String machineType) { this.machineType = machineType; }
    public String getMaxSheetSize() { return maxSheetSize; }
    public void setMaxSheetSize(String maxSheetSize) { this.maxSheetSize = maxSheetSize; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
