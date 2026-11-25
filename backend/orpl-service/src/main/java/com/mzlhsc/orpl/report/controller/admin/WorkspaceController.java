package com.mzlhsc.orpl.report.controller.admin;

import com.mzlhsc.orpl.dish.domain.vo.DishOverViewVO;
import com.mzlhsc.orpl.order.domain.vo.OrderOverViewVO;
import com.mzlhsc.orpl.setmeal.domain.vo.SetmealOverViewVO;
import com.mzlhsc.orpl.common.result.R;
import com.mzlhsc.orpl.report.domain.vo.BusinessDataVO;
import com.mzlhsc.orpl.report.service.IWorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 工作台相关接口
 */
@RestController
@RequestMapping("/admin/workspace")
@Tag(name = "工作台接口", description = "今日运营数据、订单、菜品、套餐总览")
@RequiredArgsConstructor
@Slf4j
public class WorkspaceController {

    private final IWorkspaceService workspaceService;

    /**
     * 查询今日运营数据
     */
    @GetMapping("/businessData")
    @Operation(summary = "查询今日运营数据", description = "查询当天的营业额、订单、用户等运营数据")
    public R<BusinessDataVO> businessData() {
        // 当天的开始时间与结束时间
        LocalDateTime begin = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime end = LocalDateTime.now().with(LocalTime.MAX);

        log.info("查询今日运营数据：{},{}", begin, end);
        return R.ok(workspaceService.getBusinessData(begin, end));
    }

    /**
     * 查询订单管理数据
     */
    @GetMapping("/overviewOrders")
    @Operation(summary = "查询订单管理数据", description = "查询当天的订单管理数据")
    public R<OrderOverViewVO> orderOverView() {
        log.info("查询订单管理数据");
        return R.ok(workspaceService.getOrderOverView());
    }

    /**
     * 查询菜品总览
     */
    @GetMapping("/overviewDishes")
    @Operation(summary = "查询菜品总览", description = "查询菜品起售与停售数量")
    public R<DishOverViewVO> dishOverView() {
        log.info("查询菜品总览");
        return R.ok(workspaceService.getDishOverView());
    }

    /**
     * 查询套餐总览
     */
    @GetMapping("/overviewSetmeals")
    @Operation(summary = "查询套餐总览", description = "查询套餐起售与停售数量")
    public R<SetmealOverViewVO> setmealOverView() {
        log.info("查询套餐总览");
        return R.ok(workspaceService.getSetmealOverView());
    }
}

