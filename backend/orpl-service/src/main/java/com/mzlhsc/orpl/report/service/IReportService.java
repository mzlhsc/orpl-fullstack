package com.mzlhsc.orpl.report.service;

import com.mzlhsc.orpl.report.domain.vo.OrderReportVO;
import com.mzlhsc.orpl.report.domain.vo.SalesTop10ReportVO;
import com.mzlhsc.orpl.report.domain.vo.TurnoverReportVO;
import com.mzlhsc.orpl.report.domain.vo.UserReportVO;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDate;

public interface IReportService {

    /**
     * 统计指定时间区间内的营业额数据
     */
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);

    /**
     * 统计指定时间区间内的用户数据
     */
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);

    /**
     * 统计指定时间区间内的订单数据
     */
    OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end);

    /**
     * 统计指定时间区间内的销量排名前10
     */
    SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end);

    /**
     * 导出运营数据报表
     */
    void exportBusinessData(HttpServletResponse response);

}
