package com.mzlhsc.orpl.report.controller.admin;

import com.mzlhsc.orpl.common.result.R;
import com.mzlhsc.orpl.report.domain.vo.OrderReportVO;
import com.mzlhsc.orpl.report.domain.vo.SalesTop10ReportVO;
import com.mzlhsc.orpl.report.domain.vo.TurnoverReportVO;
import com.mzlhsc.orpl.report.domain.vo.UserReportVO;
import com.mzlhsc.orpl.report.service.IReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 数据统计相关接口
 */
@RestController
@RequestMapping("/admin/report")
@Tag(name = "数据统计相关接口", description = "营业额、用户、订单、销量统计及报表导出")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final IReportService reportService;

    /**
     * 营业额统计
     */
    @GetMapping("/turnoverStatistics")
    @Operation(summary = "营业额统计", description = "统计指定时间区间内的营业额数据")
    public R<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("营业额数据统计：{},{}", begin, end);
        return R.ok(reportService.getTurnoverStatistics(begin, end));
    }

    /**
     * 用户统计
     */
    @GetMapping("/userStatistics")
    @Operation(summary = "用户统计", description = "统计指定时间区间内的用户数据")
    public R<UserReportVO> userStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("用户数据统计：{},{}", begin, end);
        return R.ok(reportService.getUserStatistics(begin, end));
    }

    /**
     * 订单统计
     */
    @GetMapping("/ordersStatistics")
    @Operation(summary = "订单统计", description = "统计指定时间区间内的订单数据")
    public R<OrderReportVO> ordersStatistics(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("订单数据统计：{},{}", begin, end);
        return R.ok(reportService.getOrderStatistics(begin, end));
    }

    /**
     * 销量排名top10
     */
    @GetMapping("/top10")
    @Operation(summary = "查询销量排名top10", description = "统计指定时间区间内的销量排名前10")
    public R<SalesTop10ReportVO> top10(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("销量排名top10：{},{}", begin, end);
        return R.ok(reportService.getSalesTop10(begin, end));
    }

    /**
     * 导出Excel报表
     */
    @GetMapping("/export")
    @Operation(summary = "导出Excel报表", description = "导出最近30天的运营数据报表")
    public void export(HttpServletResponse response) {
        log.info("导出运营数据报表");
        reportService.exportBusinessData(response);
    }
}
