package com.evops.printorder;

import com.evops.printorder.dto.PrintOrderCreateRequest;
import com.evops.printorder.entity.PrintOrderRecord;
import com.evops.printorder.mapper.PrintOrderMapper;
import com.evops.printorder.mapper.PrintOrderRecordMapper;
import com.evops.printorder.service.PrintOrderService;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证"两张以上表一起写要么全成要么全不动"：
 * 建档时印单主表与 CREATE 追溯记录同事务，追溯记录写入失败则印单主表一并回滚。
 */
@SpringBootTest
class PrintOrderTransactionTest {

    @Autowired
    private PrintOrderService printOrderService;

    @Autowired
    private PrintOrderMapper printOrderMapper;

    @MockBean
    private PrintOrderRecordMapper recordMapper;

    @Test
    void create_rollsBackOrder_whenRecordInsertFails() {
        // 模拟追溯记录写入失败
        BDDMockito.willThrow(new RuntimeException("模拟追溯记录写入失败"))
                .given(recordMapper).insert(BDDMockito.any(PrintOrderRecord.class));

        assertThrows(RuntimeException.class, () -> printOrderService.create(sampleRequest("PO-TX-001")));

        // 主表不应残留任何记录（事务整体回滚）
        assertEquals(0L, printOrderMapper.countAllByOrderNo("PO-TX-001"),
                "追溯记录写入失败时，印单主表必须一并回滚");
    }

    private PrintOrderCreateRequest sampleRequest(String orderNo) {
        PrintOrderCreateRequest req = new PrintOrderCreateRequest();
        req.setOrderNo(orderNo);
        req.setCustomerName("测试客户");
        req.setPrintDate(LocalDate.of(2026, 9, 26));
        req.setPrintQty(10000L);
        req.setKai(16);
        req.setPaperLengthMm(889);
        req.setPaperWidthMm(1194);
        req.setGsm(157);
        req.setWasteRate(new BigDecimal("0.05"));
        req.setAddRate(new BigDecimal("0.02"));
        return req;
    }
}
