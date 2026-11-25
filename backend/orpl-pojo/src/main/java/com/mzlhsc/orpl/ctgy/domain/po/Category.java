package com.mzlhsc.orpl.ctgy.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 菜品及套餐分类
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-06
 */
@Data //构造方法
@Builder //例: Dish d = Dish.builder().name("水煮牛肉").status(1).build();
@EqualsAndHashCode(callSuper = false) //生成 `equals()` 和 `hashCode()`，`callSuper` 决定**要不要把父类的字段也算进相等判断。
@Accessors(chain = true) //例: dish.setName("水煮牛肉").setPrice(new BigDecimal("58")).setStatus(1);
@TableName("category") // 指定实体对应哪张表。
@NoArgsConstructor // 无参
@AllArgsConstructor // 有参
@Schema(description ="Category对象",defaultValue = "菜品及套餐分类")
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "类型   1 菜品分类 2 套餐分类")
    private Integer type;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "顺序")
    private Integer sort;

    @Schema(description = "分类状态 0:禁用，1:启用")
    private Integer status;

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
