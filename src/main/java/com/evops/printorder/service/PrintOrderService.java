package com.evops.printorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evops.common.BusinessException;
import com.evops.common.PageResult;
import com.evops.printorder.dto.PaperEstimate;
import com.evops.printorder.dto.PrintOrderCreateRequest;
import com.evops.printorder.dto.PrintOrderQuery;
import com.evops.printorder.entity.PrintOrder;
import com.evops.printorder.entity.PrintOrderRecord;
import com.evops.printorder.enums.PrintOrderRecordType;
import com.evops.printorder.enums.PrintOrderStatus;
import com.evops.printorder.mapper.PrintOrderMapper;
import com.evops.printorder.mapper.PrintOrderRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class PrintOrderService {

    private final PrintOrderMapper printOrderMapper;
    private final PrintOrderRecordMapper recordMapper;
    private final PaperEstimator paperEstimator;

    public PrintOrderService(PrintOrderMapper printOrderMapper,
                             PrintOrderRecordMapper recordMapper,
                             PaperEstimator paperEstimator) {
        this.printOrderMapper = printOrderMapper;
        this.recordMapper = recordMapper;
        this.paperEstimator = paperEstimator;
    }

    /**
     * 印单建档：印单 + CREATE 正向记录两张表一起写，要么全成要么全不动。
     * 同一印单号重复建档直接拒绝并提示已存在。
     */
    @Transactional(rollbackFor = Exception.class)
    public PrintOrder create(PrintOrderCreateRequest req) {
        if (printOrderMapper.countAllByOrderNo(req.getOrderNo()) > 0) {
            throw new BusinessException("印单号 " + req.getOrderNo() + " 已存在，请勿重复建档");
        }

        PrintOrder order = new PrintOrder();
        order.setOrderNo(req.getOrderNo());
        order.setCustomerName(req.getCustomerName());
        order.setProductName(req.getProductName());
        order.setMachineId(req.getMachineId());
        order.setPrintDate(req.getPrintDate());
        order.setPrintQty(req.getPrintQty());
        order.setKai(req.getKai());
        order.setPaperLengthMm(req.getPaperLengthMm());
        order.setPaperWidthMm(req.getPaperWidthMm());
        order.setGsm(req.getGsm());
        order.setWasteRate(req.getWasteRate());
        order.setAddRate(req.getAddRate());
        order.setRemark(req.getRemark());
        order.setStatus(PrintOrderStatus.ACTIVE.name());

        // 用纸量按开数与克重估算，损耗、加放单列
        PaperEstimate est = paperEstimator.estimate(
                req.getPrintQty(), req.getKai(),
                req.getPaperLengthMm(), req.getPaperWidthMm(),
                req.getGsm(), req.getWasteRate(), req.getAddRate());
        paperEstimator.apply(order, est);

        printOrderMapper.insert(order);

        PrintOrderRecord createRecord = newRecord(order, PrintOrderRecordType.CREATE, null, "建档");
        recordMapper.insert(createRecord);

        return order;
    }

    /** 按印单号与日期组合检索（可叠加客户、状态），分页 */
    public PageResult<PrintOrder> page(PrintOrderQuery query) {
        Page<PrintOrder> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<PrintOrder> wrapper = new LambdaQueryWrapper<PrintOrder>()
                .eq(StringUtils.hasText(query.getOrderNo()), PrintOrder::getOrderNo, query.getOrderNo())
                .like(StringUtils.hasText(query.getCustomerName()), PrintOrder::getCustomerName, query.getCustomerName())
                .eq(StringUtils.hasText(query.getStatus()), PrintOrder::getStatus, query.getStatus())
                .ge(query.getStartDate() != null, PrintOrder::getPrintDate, query.getStartDate())
                .le(query.getEndDate() != null, PrintOrder::getPrintDate, query.getEndDate())
                .orderByDesc(PrintOrder::getId);
        Page<PrintOrder> result = printOrderMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    public PrintOrder getById(Long id) {
        PrintOrder order = printOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("印单不存在或已删除");
        }
        return order;
    }

    /** 追溯链：按时间正序返回该印单的建档/作废全部记录 */
    public List<PrintOrderRecord> records(Long orderId) {
        getById(orderId);
        return recordMapper.selectList(
                new LambdaQueryWrapper<PrintOrderRecord>()
                        .eq(PrintOrderRecord::getOrderId, orderId)
                        .orderByAsc(PrintOrderRecord::getId));
    }

    /**
     * 作废：不删除原单，在同一事务内写一条 VOID 反向记录（指回 CREATE 记录），
     * 并把状态翻转为 VOID，保留完整追溯链。两表一起写，要么全成要么全不动。
     */
    @Transactional(rollbackFor = Exception.class)
    public PrintOrder voidOrder(Long id, String reason) {
        PrintOrder order = getById(id);
        if (PrintOrderStatus.VOID.name().equals(order.getStatus())) {
            throw new BusinessException("印单 " + order.getOrderNo() + " 已作废，请勿重复作废");
        }

        PrintOrderRecord createRecord = recordMapper.selectOne(
                new LambdaQueryWrapper<PrintOrderRecord>()
                        .eq(PrintOrderRecord::getOrderId, id)
                        .eq(PrintOrderRecord::getRecordType, PrintOrderRecordType.CREATE.name())
                        .orderByAsc(PrintOrderRecord::getId)
                        .last("LIMIT 1"));

        PrintOrderRecord voidRecord = newRecord(
                order, PrintOrderRecordType.VOID,
                createRecord == null ? null : createRecord.getId(),
                reason);
        recordMapper.insert(voidRecord);

        order.setStatus(PrintOrderStatus.VOID.name());
        printOrderMapper.updateById(order);

        return order;
    }

    /** 删除只打标记（@TableLogic 自动 UPDATE deleted=1），不触碰追溯链 */
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        getById(id);
        printOrderMapper.deleteById(id);
    }

    private PrintOrderRecord newRecord(PrintOrder order, PrintOrderRecordType type,
                                       Long reverseOfId, String reason) {
        PrintOrderRecord record = new PrintOrderRecord();
        record.setOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setRecordType(type.name());
        record.setReverseOfId(reverseOfId);
        record.setReason(reason);
        return record;
    }
}
