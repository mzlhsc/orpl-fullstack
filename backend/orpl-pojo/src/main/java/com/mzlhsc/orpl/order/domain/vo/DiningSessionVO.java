package com.mzlhsc.orpl.order.domain.vo;

import com.mzlhsc.orpl.order.domain.po.DiningSessionMember;
import com.mzlhsc.orpl.order.domain.po.Orders;
import com.mzlhsc.orpl.order.domain.po.ShoppingCart;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "堂食会话详情")
public class DiningSessionVO implements Serializable {

    private Long id;

    private String sessionNo;

    private Long tableId;

    private String tableName;

    private Integer status;

    private Integer guestCount;

    private Long ownerUserId;

    private Integer aaEnabled;

    private String remark;

    private BigDecimal totalAmount;

    private BigDecimal paidAmount;

    private BigDecimal unpaidAmount;

    private BigDecimal perPersonAmount;

    private Integer memberCount;

    private LocalDateTime openTime;

    private LocalDateTime checkoutTime;

    private List<String> tableNames = new ArrayList<>();

    private List<DiningSessionMember> members = new ArrayList<>();

    private List<ShoppingCart> cartItems = new ArrayList<>();

    private List<Orders> orders = new ArrayList<>();

    @Schema(description = "当前堂食订单按第几次下单分组的菜品")
    private List<DiningOrderRoundVO> orderRounds = new ArrayList<>();

    @Schema(description = "当前堂食订单id")
    private Long currentOrderId;

    @Schema(description = "当前堂食订单号")
    private String currentOrderNo;

    private BigDecimal cartAmount;
}
