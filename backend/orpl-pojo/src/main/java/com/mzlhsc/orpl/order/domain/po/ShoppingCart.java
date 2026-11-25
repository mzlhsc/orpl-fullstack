package com.mzlhsc.orpl.order.domain.po;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * <p>
 * 购物车
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-13
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("shopping_cart")
@Schema(name = "ShoppingCart对象", description="购物车")
public class ShoppingCart implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(name = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(name = "商品名称")
    private String name;

    @Schema(name = "图片")
    private String image;

    @Schema(name = "主键")
    private Long userId;

    @Schema(name = "菜品id")
    private Long dishId;

    @Schema(name = "套餐id")
    private Long setmealId;

    @Schema(name = "口味")
    private String dishFlavor;

    @Schema(name = "数量")
    private Integer number;

    @Schema(name = "金额")
    private BigDecimal amount;

    @Schema(name = "堂食会话id，堂食共享购物车使用")
    private Long diningSessionId;

    @Schema(name = "创建时间")
    private LocalDateTime createTime;


}
