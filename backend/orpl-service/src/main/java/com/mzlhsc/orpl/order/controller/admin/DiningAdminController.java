package com.mzlhsc.orpl.order.controller.admin;

import com.mzlhsc.orpl.common.result.R;
import com.mzlhsc.orpl.order.domain.dto.DiningCheckoutDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningMergeDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningOpenTableDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningTableDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningTransferDTO;
import com.mzlhsc.orpl.order.domain.po.DiningReservation;
import com.mzlhsc.orpl.order.domain.po.DiningTable;
import com.mzlhsc.orpl.order.domain.vo.DiningReportVO;
import com.mzlhsc.orpl.order.domain.vo.DiningSessionVO;
import com.mzlhsc.orpl.order.domain.vo.DiningTableVO;
import com.mzlhsc.orpl.order.service.IDiningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 堂食管理端：桌台、预约、会话、转台/并桌、结账、报表。
 */
@RestController
@RequestMapping("/admin/dining")
@Tag(name = "堂食管理接口", description = "管理端堂食桌台/预约/会话/结账/报表")
@RequiredArgsConstructor
@Slf4j
public class DiningAdminController {

    private final IDiningService diningService;

    // ==================== 桌台 ====================

    @GetMapping("/table/list")
    @Operation(summary = "桌台列表")
    public R<List<DiningTableVO>> listTables(@RequestParam(required = false) Integer status,
                                             @RequestParam(required = false) String area) {
        return R.ok(diningService.listTables(status, area));
    }

    @PostMapping("/table")
    @Operation(summary = "新增桌台")
    public R<DiningTable> createTable(@RequestBody @Valid DiningTableDTO dto) {
        return R.ok(diningService.createTable(dto));
    }

    @PutMapping("/table/{id}")
    @Operation(summary = "修改桌台")
    public R<DiningTable> updateTable(@PathVariable Long id, @RequestBody DiningTableDTO dto) {
        return R.ok(diningService.updateTable(id, dto));
    }

    @DeleteMapping("/table/{id}")
    @Operation(summary = "删除桌台")
    public R<Void> deleteTable(@PathVariable Long id) {
        diningService.deleteTable(id);
        return R.ok();
    }

    @PostMapping("/table/{id}/refresh-qr")
    @Operation(summary = "刷新桌台二维码")
    public R<DiningTableVO> refreshQr(@PathVariable Long id) {
        return R.ok(diningService.refreshQrToken(id));
    }

    // ==================== 预约 ====================

    @GetMapping("/reservation/list")
    @Operation(summary = "预约列表")
    public R<List<DiningReservation>> listReservations(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return R.ok(diningService.listReservations(status, date));
    }

    @PostMapping("/reservation/{id}/confirm")
    @Operation(summary = "确认预约")
    public R<Void> confirmReservation(@PathVariable Long id) {
        diningService.confirmReservation(id);
        return R.ok();
    }

    @PostMapping("/reservation/{id}/cancel")
    @Operation(summary = "取消预约")
    public R<Void> cancelReservation(@PathVariable Long id) {
        diningService.cancelReservation(id);
        return R.ok();
    }

    @PostMapping("/reservation/{id}/arrive")
    @Operation(summary = "预约到店")
    public R<DiningReservation> arriveReservation(@PathVariable Long id) {
        return R.ok(diningService.arriveReservation(id));
    }

    // ==================== 会话 ====================

    @GetMapping("/session/list")
    @Operation(summary = "堂食会话列表")
    public R<List<DiningSessionVO>> listSessions(@RequestParam(required = false) Integer status,
                                                 @RequestParam(required = false) Long tableId) {
        return R.ok(diningService.listSessions(status, tableId));
    }

    @GetMapping("/session/{id}")
    @Operation(summary = "会话详情")
    public R<DiningSessionVO> detail(@PathVariable Long id) {
        return R.ok(diningService.detail(id));
    }

    @PostMapping("/session/open")
    @Operation(summary = "开台（员工代客开台）")
    public R<DiningSessionVO> openTable(@RequestBody @Valid DiningOpenTableDTO dto) {
        return R.ok(diningService.openTableByAdmin(dto));
    }

    @PostMapping("/session/{id}/transfer")
    @Operation(summary = "转台")
    public R<Void> transfer(@PathVariable Long id, @RequestBody @Valid DiningTransferDTO dto) {
        diningService.transferTable(id, dto);
        return R.ok();
    }

    @PostMapping("/session/{targetId}/merge")
    @Operation(summary = "并桌")
    public R<Void> merge(@PathVariable Long targetId, @RequestBody @Valid DiningMergeDTO dto) {
        diningService.mergeSessions(targetId, dto);
        return R.ok();
    }

    @PostMapping({"/session/{id}/cancel", "/session/{id}/close"})
    @Operation(summary = "关闭会话", description = "结束就餐会话：取消未完成订单、清空共享购物车并释放桌台；已支付或已完成订单保留")
    public R<Void> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        diningService.cancelSession(id, reason);
        return R.ok();
    }

    @PostMapping("/session/checkout")
    @Operation(summary = "整单结账")
    public R<Void> checkout(@RequestBody @Valid DiningCheckoutDTO dto) {
        diningService.checkout(dto);
        return R.ok();
    }

    // ==================== 报表 ====================

    @GetMapping("/report")
    @Operation(summary = "堂食经营报表")
    public R<DiningReportVO> report(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime begin,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        return R.ok(diningService.report(begin, end));
    }
}

