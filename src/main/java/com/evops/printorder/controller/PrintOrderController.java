package com.evops.printorder.controller;

import com.evops.common.ApiResponse;
import com.evops.common.PageResult;
import com.evops.printorder.dto.PrintOrderCreateRequest;
import com.evops.printorder.dto.PrintOrderQuery;
import com.evops.printorder.dto.PrintOrderVoidRequest;
import com.evops.printorder.entity.PrintOrder;
import com.evops.printorder.entity.PrintOrderRecord;
import com.evops.printorder.service.PrintOrderService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/print-orders")
public class PrintOrderController {

    private final PrintOrderService printOrderService;

    public PrintOrderController(PrintOrderService printOrderService) {
        this.printOrderService = printOrderService;
    }

    /** 印单建档（同事务写建档正向记录） */
    @PostMapping
    public ApiResponse<PrintOrder> create(@Valid @RequestBody PrintOrderCreateRequest request) {
        return ApiResponse.ok(printOrderService.create(request));
    }

    /** 按印单号 + 生产日期区间组合检索（分页） */
    @GetMapping
    public ApiResponse<PageResult<PrintOrder>> page(@ModelAttribute PrintOrderQuery query) {
        return ApiResponse.ok(printOrderService.page(query));
    }

    /** 印单详情 */
    @GetMapping("/{id}")
    public ApiResponse<PrintOrder> detail(@PathVariable Long id) {
        return ApiResponse.ok(printOrderService.getById(id));
    }

    /** 追溯链：建档/作废全量记录 */
    @GetMapping("/{id}/records")
    public ApiResponse<List<PrintOrderRecord>> records(@PathVariable Long id) {
        return ApiResponse.ok(printOrderService.records(id));
    }

    /** 作废：写反向记录并翻转状态 */
    @PostMapping("/{id}/void")
    public ApiResponse<PrintOrder> voidOrder(@PathVariable Long id,
                                             @RequestBody(required = false) PrintOrderVoidRequest request) {
        String reason = request == null ? null : request.getReason();
        return ApiResponse.ok(printOrderService.voidOrder(id, reason));
    }

    /** 删除：仅打删除标记 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        printOrderService.remove(id);
        return ApiResponse.ok(null);
    }
}
