package com.mzlhsc.orpl.order.controller.user;


import com.mzlhsc.orpl.common.result.PageDTO;
import com.mzlhsc.orpl.common.result.R;
import com.mzlhsc.orpl.order.domain.dto.OrdersCancelDTO;
import com.mzlhsc.orpl.order.domain.dto.OrdersSubmitDTO;
import com.mzlhsc.orpl.order.domain.vo.OrderSubmitVO;
import com.mzlhsc.orpl.order.domain.vo.OrderVO;
import com.mzlhsc.orpl.order.service.IOrdersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 订单表 前端控制器
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-09
 */
@RestController("userOrdersController")
@RequestMapping("/user/order")
@Tag(name = "订单接口", description = "用户端订单接口")
@RequiredArgsConstructor
@Slf4j
public class OrdersController {

    private final IOrdersService orderService;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    @Operation(summary = "用户下单", description = "提交订单并清空购物车")
    public R<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("用户下单：{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return R.ok(orderSubmitVO);
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
        OrderVO orderVO = orderService.details(id);
        return R.ok(orderVO);
    }

    /**
     * 取消订单
     *
     * @return
     */
    @PutMapping("/cancel")
    @Operation(summary = "取消订单", description = "用户取消自己的订单")
    public R<Void> cancel(@RequestBody OrdersCancelDTO ordersCancelDTO) throws Exception {
        log.info("取消订单: {}", ordersCancelDTO);
        orderService.cancel(ordersCancelDTO);
        return R.ok();
    }


    /**
     * 历史订单查询
     *
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    @GetMapping("/historyOrders")
    @Operation(summary = "历史订单查询", description = "分页查询当前用户的历史订单")
    public R<PageDTO<OrderVO>> page(int page, int pageSize, Integer status) {
        log.info("历史订单查询: {}, {}, {}", page, pageSize, status);

        PageDTO<OrderVO> pageDTO = orderService.pageQuery4User(page, pageSize, status);

        return R.ok(pageDTO);
    }


    /**
     * 再来一单
     *
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    @Operation(summary = "再来一单", description = "将历史订单明细重新加入购物车")
    public R<Void> repetition(@PathVariable("id") Long id) {
        log.info("再来一单: {}", id);

        orderService.repetition(id);

        return R.ok();
    }


    /**
     * 用户催单
     *
     * @param id
     * @return
     */
    @GetMapping("/reminder/{id}")
    @Operation(summary = "用户催单", description = "用户对订单催单")
    public R<Void> reminder(@PathVariable("id") Long id) {
        log.info("用户催单: {}", id);

        orderService.reminder(id);

        return R.ok();
    }


}

