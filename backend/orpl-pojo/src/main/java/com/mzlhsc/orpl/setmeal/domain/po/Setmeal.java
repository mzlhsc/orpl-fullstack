package com.mzlhsc.orpl.setmeal.domain.po;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
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
 * 套餐
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-09
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("setmeal")
@Schema(description="套餐")
public class Setmeal implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "菜品分类id")
    private Long categoryId;

    @Schema(description = "套餐名称")
    private String name;

    @Schema(description = "套餐价格")
    private BigDecimal price;

    @Schema(description = "售卖状态 0:停售 1:起售")
    private Integer status;

    @Schema(description = "描述信息")
    private String description;

    @Schema(description = "图片")
    private String image;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @Schema(description = "修改人")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;


}
