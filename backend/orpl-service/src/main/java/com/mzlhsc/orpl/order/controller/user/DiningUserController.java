package com.mzlhsc.orpl.order.controller.user;

import com.mzlhsc.orpl.common.result.R;
import com.mzlhsc.orpl.order.domain.dto.DiningAaPayDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningCartDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningJoinDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningOpenTableDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningReservationDTO;
import com.mzlhsc.orpl.order.domain.po.DiningReservation;
import com.mzlhsc.orpl.order.domain.po.ShoppingCart;
import com.mzlhsc.orpl.order.domain.vo.DiningSessionVO;
import com.mzlhsc.orpl.order.domain.vo.DiningTableVO;
import com.mzlhsc.orpl.order.domain.vo.OrderSubmitVO;
import com.mzlhsc.orpl.order.service.IDiningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 堂食用户端：扫码、开台/加入、共享购物车、加菜下单、AA 结账、预约。
 */
@RestController
@RequestMapping("/user/dining")
@Tag(name = "堂食用户接口", description = "用户端堂食扫码/开台/点餐/AA/预约")
@RequiredArgsConstructor
@Slf4j
public class DiningUserController {

    private final IDiningService diningService;

    @GetMapping("/table/list")
    @Operation(summary = "桌台列表（预约选桌）")
    public R<List<DiningTableVO>> listTables() {
        return R.ok(diningService.listTables(null, null));
    }

    @GetMapping("/scan/{qrToken}")
    @Operation(summary = "扫码获取桌台与当前会话")
    public R<Map<String, Object>> scan(@PathVariable String qrToken) {
        return R.ok(diningService.scanTable(qrToken));
    }

    @PostMapping("/open")
    @Operation(summary = "扫码开台")
    public R<DiningSessionVO> open(@RequestBody @Valid DiningOpenTableDTO dto) {
        return R.ok(diningService.openTable(dto));
    }

    @PostMapping("/join")
    @Operation(summary = "加入同桌会话")
    public R<DiningSessionVO> join(@RequestBody @Valid DiningJoinDTO dto) {
        return R.ok(diningService.joinSession(dto));
    }

    @GetMapping("/session/mine")
    @Operation(summary = "我的进行中堂食会话")
    public R<DiningSessionVO> mySession() {
        return R.ok(diningService.myActiveSession());
    }

    @GetMapping("/session/{id}")
    @Operation(summary = "会话详情（同桌共享）")
    public R<DiningSessionVO> detail(@PathVariable Long id) {
        return R.ok(diningService.detail(id));
    }

    @GetMapping("/cart/{sessionId}")
    @Operation(summary = "共享购物车列表")
    public R<List<ShoppingCart>> cart(@PathVariable Long sessionId) {
        return R.ok(diningService.listCart(sessionId));
    }

    @PostMapping("/cart")
    @Operation(summary = "共享购物车加菜/减菜")
    public R<Void> addCart(@RequestBody @Valid DiningCartDTO dto) {
        diningService.addCartItem(dto);
        return R.ok();
    }

    @DeleteMapping("/cart/{sessionId}/{cartId}")
    @Operation(summary = "删除购物车项")
    public R<Void> removeCart(@PathVariable Long sessionId, @PathVariable Long cartId) {
        diningService.removeCartItem(sessionId, cartId);
        return R.ok();
    }

    @PostMapping("/cart/{sessionId}/clear")
    @Operation(summary = "清空共享购物车")
    public R<Void> clearCart(@PathVariable Long sessionId) {
        diningService.clearCart(sessionId);
        return R.ok();
    }

    @PostMapping("/session/{sessionId}/submit")
    @Operation(summary = "堂食加菜下单（扣库存、生成堂食订单）")
    public R<OrderSubmitVO> submit(@PathVariable Long sessionId) {
        return R.ok(diningService.submitDiningOrder(sessionId));
    }

    @PostMapping("/session/{sessionId}/checkout-request")
    @Operation(summary = "申请结账")
    public R<DiningSessionVO> requestCheckout(@PathVariable Long sessionId) {
        return R.ok(diningService.requestCheckout(sessionId));
    }

    @PostMapping("/session/{sessionId}/aa-enable")
    @Operation(summary = "开启 AA 结账")
    public R<Void> enableAa(@PathVariable Long sessionId) {
        diningService.enableAa(sessionId);
        return R.ok();
    }

    @PostMapping("/aa-pay")
    @Operation(summary = "AA 分摊支付")
    public R<Map<String, Object>> aaPay(@RequestBody @Valid DiningAaPayDTO dto) {
        return R.ok(diningService.aaPay(dto));
    }

    @PostMapping("/reservation")
    @Operation(summary = "提交堂食预约")
    public R<DiningReservation> createReservation(@RequestBody @Valid DiningReservationDTO dto) {
        return R.ok(diningService.createReservation(dto));
    }

    @GetMapping("/reservation/mine")
    @Operation(summary = "我的预约")
    public R<List<DiningReservation>> myReservations() {
        return R.ok(diningService.myReservations());
    }

    @PostMapping("/reservation/{id}/cancel")
    @Operation(summary = "取消我的预约")
    public R<Void> cancelReservation(@PathVariable Long id) {
        diningService.cancelReservation(id);
        return R.ok();
    }
}
