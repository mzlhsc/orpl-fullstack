package com.mzlhsc.orpl.report.service.impl;

import com.mzlhsc.orpl.order.domain.dto.DateAmountDTO;
import com.mzlhsc.orpl.user.domain.dto.DateCountDTO;
import com.mzlhsc.orpl.order.domain.dto.GoodsSalesDTO;
import com.mzlhsc.orpl.order.service.OrderQueryService;
import com.mzlhsc.orpl.user.service.UserQueryService;
import com.mzlhsc.orpl.common.exception.BadRequestException;
import com.mzlhsc.orpl.common.exception.CommonException;
import com.mzlhsc.orpl.report.domain.vo.BusinessDataVO;
import com.mzlhsc.orpl.report.domain.vo.OrderReportVO;
import com.mzlhsc.orpl.report.domain.vo.SalesTop10ReportVO;
import com.mzlhsc.orpl.report.domain.vo.TurnoverReportVO;
import com.mzlhsc.orpl.report.domain.vo.UserReportVO;
import com.mzlhsc.orpl.report.service.IReportService;
import com.mzlhsc.orpl.report.service.IWorkspaceService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements IReportService {

    /** 订单状态：已完成 */
    private static final Integer ORDER_STATUS_COMPLETED = 5;

    /** 统计存量用户的起始日期 */
    private static final LocalDate USER_BASE_DATE = LocalDate.of(1970, 1, 1);

    private final OrderQueryService orderQueryService;
    private final UserQueryService userQueryService;
    private final IWorkspaceService workspaceService;

    /**
     * 统计指定时间区间内的营业额数据
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        validateRange(begin, end);
        // 从 begin 到 end 之间的每一天
        List<LocalDate> dateList = buildDateList(begin, end);

        // 订单服务返回的每日营业额，按日期建索引，区间内缺失的日期补 0
        Map<String, BigDecimal> turnoverMap = new HashMap<>();
        List<DateAmountDTO> turnoverData = orderQueryService.listTurnover(begin, end);
        if (turnoverData != null) {
            for (DateAmountDTO dto : turnoverData) {
                if (dto != null && dto.getDate() != null && dto.getAmount() != null) {
                    turnoverMap.put(dto.getDate(), dto.getAmount());
                }
            }
        }

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            BigDecimal amount = turnoverMap.get(date.toString());
            turnoverList.add(amount == null ? 0.0 : amount.doubleValue());
        }

        return TurnoverReportVO.builder()
                .dateList(joinDate(dateList))
                .turnoverList(turnoverList.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .build();
    }

    /**
     * 统计指定时间区间内的用户数据
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        validateRange(begin, end);
        List<LocalDate> dateList = buildDateList(begin, end);

        // 每日新增用户，区间内缺失的日期补 0
        Map<String, Long> newUserMap = new HashMap<>();
        List<DateCountDTO> userData = userQueryService.countUserByDate(begin, end);
        if (userData != null) {
            for (DateCountDTO dto : userData) {
                if (dto != null && dto.getDate() != null) {
                    newUserMap.put(dto.getDate(), dto.getCount() == null ? 0L : dto.getCount());
                }
            }
        }

        // 区间开始前的存量用户，累计上去得到每日总用户量
        Long baseTotal = userQueryService.countUser(USER_BASE_DATE, begin.minusDays(1));
        long runningTotal = baseTotal == null ? 0L : baseTotal;

        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            long newUser = newUserMap.getOrDefault(date.toString(), 0L);
            runningTotal += newUser;

            newUserList.add((int) newUser);
            totalUserList.add((int) runningTotal);
        }

        return UserReportVO.builder()
                .dateList(joinDate(dateList))
                .totalUserList(totalUserList.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .newUserList(newUserList.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .build();
    }

    /**
     * 统计指定时间区间内的订单数据
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        validateRange(begin, end);
        List<LocalDate> dateList = buildDateList(begin, end);

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        for (LocalDate date : dateList) {
            // 每天的订单总数
            orderCountList.add(getOrderCount(date, null));
            // 每天的有效订单数
            validOrderCountList.add(getOrderCount(date, ORDER_STATUS_COMPLETED));
        }

        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).orElse(0);
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).orElse(0);

        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        return OrderReportVO.builder()
                .dateList(joinDate(dateList))
                .orderCountList(orderCountList.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .validOrderCountList(validOrderCountList.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 统计指定时间区间内的销量排名前10
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        validateRange(begin, end);
        List<GoodsSalesDTO> salesTop10 = orderQueryService.listSalesTop10(begin, end);
        if (salesTop10 == null) {
            salesTop10 = List.of();
        }

        String nameList = salesTop10.stream()
                .map(GoodsSalesDTO::getName)
                .collect(Collectors.joining(","));

        String numberList = salesTop10.stream()
                .map(dto -> String.valueOf(dto.getNumber()))
                .collect(Collectors.joining(","));

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 导出最近 30 天的运营数据报表
     */
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        // 统计区间：最近 30 天
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        // 概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(
                LocalDateTime.of(dateBegin, LocalTime.MIN),
                LocalDateTime.of(dateEnd, LocalTime.MAX));

        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        if (in == null) {
            throw new CommonException("未找到报表模板：template/运营数据报表模板.xlsx", 500);
        }

        // 下载响应头必须在写出流之前设置，否则浏览器拿不到文件名
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=business-data.xlsx");

        try (InputStream template = in; XSSFWorkbook excel = new XSSFWorkbook(template)) {
            XSSFSheet sheet = excel.getSheet("Sheet1");

            // 时间区间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

            // 概览数据：第 4 行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            // 概览数据：第 5 行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            // 明细数据：每天一行（从第 8 行开始，共 30 行）
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                BusinessDataVO businessData = workspaceService.getBusinessData(
                        LocalDateTime.of(date, LocalTime.MIN),
                        LocalDateTime.of(date, LocalTime.MAX));

                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            // 写入响应流，由浏览器下载
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            out.flush();
        } catch (Exception e) {
            log.error("导出运营数据报表失败", e);
            throw new CommonException("导出运营数据报表失败", e, 500);
        }
    }

    /**
     * 构建 begin 到 end（含首尾）的日期列表
     */
    private List<LocalDate> buildDateList(LocalDate begin, LocalDate end) {
        validateRange(begin, end);
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate date = begin;
        while (!date.isAfter(end)) {
            dateList.add(date);
            date = date.plusDays(1);
        }
        return dateList;
    }

    /**
     * 校验统计区间：参数不能为空、开始不能晚于结束、区间不超过一年
     */
    private void validateRange(LocalDate begin, LocalDate end) {
        if (begin == null || end == null) {
            throw new BadRequestException("开始日期和结束日期不能为空");
        }
        if (begin.isAfter(end)) {
            throw new BadRequestException("开始日期不能晚于结束日期");
        }
        if (ChronoUnit.DAYS.between(begin, end) > 365) {
            throw new BadRequestException("统计区间不能超过一年");
        }
    }

    private String joinDate(List<LocalDate> dateList) {
        return dateList.stream().map(LocalDate::toString).collect(Collectors.joining(","));
    }

    /**
     * 按日期和状态统计订单数量，status 为 null 表示全部
     */
    private Integer getOrderCount(LocalDate date, Integer status) {
        Long count = orderQueryService.countOrders(date, date, status);
        return count == null ? 0 : count.intValue();
    }
}


