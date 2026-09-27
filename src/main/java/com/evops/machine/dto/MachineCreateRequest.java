package com.evops.machine.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 机台建档请求 */
public class MachineCreateRequest {
    @NotBlank(message = "机台号不能为空")
    @Size(max = 64, message = "机台号最长64位")
    private String machineCode;

    @NotBlank(message = "机台名称不能为空")
    @Size(max = 128, message = "机台名称最长128位")
    private String machineName;

    private String machineType;
    private String maxSheetSize;
    private String remark;

    public String getMachineCode() { return machineCode; }
    public void setMachineCode(String machineCode) { this.machineCode = machineCode; }
    public String getMachineName() { return machineName; }
    public void setMachineName(String machineName) { this.machineName = machineName; }
    public String getMachineType() { return machineType; }
    public void setMachineType(String machineType) { this.machineType = machineType; }
    public String getMaxSheetSize() { return maxSheetSize; }
    public void setMaxSheetSize(String maxSheetSize) { this.maxSheetSize = maxSheetSize; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
