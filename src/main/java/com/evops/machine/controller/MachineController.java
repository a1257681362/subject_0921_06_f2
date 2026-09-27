package com.evops.machine.controller;

import com.evops.common.ApiResponse;
import com.evops.common.PageResult;
import com.evops.machine.dto.MachineCreateRequest;
import com.evops.machine.dto.MachineQuery;
import com.evops.machine.entity.Machine;
import com.evops.machine.service.MachineService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/machines")
public class MachineController {

    private final MachineService machineService;

    public MachineController(MachineService machineService) {
        this.machineService = machineService;
    }

    /** 机台建档 */
    @PostMapping
    public ApiResponse<Machine> create(@Valid @RequestBody MachineCreateRequest request) {
        return ApiResponse.ok(machineService.create(request));
    }

    /** 机台台账条件分页查询 */
    @GetMapping
    public ApiResponse<PageResult<Machine>> page(@ModelAttribute MachineQuery query) {
        return ApiResponse.ok(machineService.page(query));
    }

    /** 机台详情 */
    @GetMapping("/{id}")
    public ApiResponse<Machine> detail(@PathVariable Long id) {
        return ApiResponse.ok(machineService.getById(id));
    }

    /** 删除：仅打删除标记 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        machineService.remove(id);
        return ApiResponse.ok(null);
    }
}
