package com.mzlhsc.orpl.order.domain.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("dining_session")
public class DiningSession implements Serializable {

    public static final int OPEN = 1;
    public static final int WAIT_CHECKOUT = 2;
    public static final int CHECKED_OUT = 3;
    public static final int CANCELLED = 4;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionNo;

    private Long tableId;

    private String tableName;

    private Long orderId;

    private Long ownerUserId;

    private Integer guestCount;

    /** 1 就餐中 2 待结账 3 已结账 4 已取消 */
    private Integer status;

    private BigDecimal totalAmount;

    private Integer aaEnabled;

    private String remark;

    private LocalDateTime openTime;

    private LocalDateTime checkoutTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
