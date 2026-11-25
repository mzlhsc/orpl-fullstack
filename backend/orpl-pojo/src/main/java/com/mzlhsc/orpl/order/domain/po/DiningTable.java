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
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("dining_table")
public class DiningTable implements Serializable {

    public static final int FREE = 0;
    public static final int OCCUPIED = 1;
    public static final int WAIT_CHECKOUT = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Integer seats;

    private String area;

    /** 0 空闲 1 就餐中 2 待结账 */
    private Integer status;

    private String qrToken;

    /** 1 允许预约 */
    private Integer reservationEnabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
