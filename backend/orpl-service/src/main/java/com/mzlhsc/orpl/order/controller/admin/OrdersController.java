package com.mzlhsc.orpl.order.controller.admin;


import com.mzlhsc.orpl.common.result.PageDTO;
import com.mzlhsc.orpl.common.result.R;
import com.mzlhsc.orpl.order.domain.dto.OrdersCancelDTO;
import com.mzlhsc.orpl.order.domain.dto.OrdersConfirmDTO;
import com.mzlhsc.orpl.order.domain.dto.OrdersPageQueryDTO;
import com.mzlhsc.orpl.order.domain.dto.OrdersPaymentStatusDTO;
import com.mzlhsc.orpl.order.domain.dto.OrdersRejectionDTO;
import com.mzlhsc.orpl.order.domain.vo.OrderStatisticsVO;
import com.mzlhsc.orpl.order.domain.vo.OrderVO;
import com.mzlhsc.orpl.order.service.IOrdersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 订单表 前端控制器
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-09
 */
@RestController("adminOrdersController")
@RequestMapping("/admin/order")
@Tag(name = "订单接口", description = "管理端订单管理接口")
@RequiredArgsConstructor
@Slf4j
public class OrdersController {

    private final IOrdersService ordersService;


    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("/conditionSearch")
    @Operation(summary = "订单搜索", description = "按订单号/手机号/状态/时间分页搜索订单")
    public R<PageDTO<OrderVO>> conditionSearch(@ParameterObject OrdersPageQueryDTO ordersPageQueryDTO){
        log.info("订单搜索: {}", ordersPageQueryDTO);

        PageDTO<OrderVO> pageDTO = ordersService.conditionSearch(ordersPageQueryDTO);

        return R.ok(pageDTO);
    }

    /**
     * 订单详情
     *
     * @param id
     * @return
     */
    @GetMapping("/details/{id}")
    @Operation(summary = "查询订单详情", description = "根据订单id查询订单及明细")
    public R<OrderVO> details(@PathVariable("id") Long id) {
        log.info("查询订单详情: {}", id);
        OrderVO orderVO = ordersService.details(id);
        return R.ok(orderVO);
    }


    /**
     * 接单
     * @param ordersConfirmDTO
     * @return
     */
    @PutMapping("/confirm")
    @Operation(summary = "接单", description = "将待接单订单置为已接单")
    public R<Void> confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO){

        log.info("接单: {}",ordersConfirmDTO.getId());

        ordersService.confirm(ordersConfirmDTO.getId());

        return R.ok();
    }

    /**
     * 完成订单
     * @param id
     * @return
     */
    @PutMapping("/complete/{id}")
    @Operation(summary = "完成订单", description = "将已接单订单置为已完成，并同步支付状态为已支付")
    public R<Void> complete(@PathVariable Long id){

        log.info("完成订单: {}",id);

        ordersService.complete(id);

        return R.ok();
    }

    /**
     * 修改订单支付状态
     */
    @PutMapping("/payStatus")
    @Operation(summary = "修改订单支付状态", description = "管理端手动标记未支付/已支付；已完成订单只能为已支付")
    public R<Void> updatePayStatus(@RequestBody OrdersPaymentStatusDTO dto) {
        log.info("修改订单支付状态: {}", dto);
        ordersService.updatePayStatus(dto.getId(), dto.getPayStatus());
        return R.ok();
    }
    /**
     * 拒单
     *
     * @return
     */
    @PutMapping("/rejection")
    @Operation(summary = "拒单", description = "商家拒单并记录拒单原因")
    public R<Void> rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        log.info("拒单: {}", ordersRejectionDTO);
        ordersService.rejection(ordersRejectionDTO);
        return R.ok();
    }

    /**
     * 取消订单
     *
     * @return
     */
    @PutMapping("/cancel")
    @Operation(summary = "取消订单", description = "管理端取消订单并记录取消原因")
    public R<Void> cancel(@RequestBody OrdersCancelDTO ordersCancelDTO) throws Exception {
        log.info("取消订单: {}", ordersCancelDTO);
        ordersService.cancel(ordersCancelDTO);
        return R.ok();
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    @GetMapping("/statistics")
    @Operation(summary = "各个状态的订单数量统计", description = "统计待接单/已接单数量；派送中状态已删除")
    public R<OrderStatisticsVO> statistics() {
        log.info("各个状态的订单数量统计");
        OrderStatisticsVO orderStatisticsVO = ordersService.statistics();
        return R.ok(orderStatisticsVO);
    }








}

