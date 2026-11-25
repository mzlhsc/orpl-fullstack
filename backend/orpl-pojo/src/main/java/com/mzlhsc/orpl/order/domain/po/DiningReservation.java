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
@TableName("dining_reservation")
public class DiningReservation implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tableId;

    private Long userId;

    private String contactName;

    private String contactPhone;

    private LocalDateTime reserveTime;

    private Integer guestCount;

    /** 1 待确认 2 已确认 3 已到店 4 已取消 5 已过期 */
    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
