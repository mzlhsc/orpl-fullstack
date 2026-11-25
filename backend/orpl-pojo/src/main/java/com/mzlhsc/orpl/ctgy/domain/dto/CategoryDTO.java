package com.mzlhsc.orpl.ctgy.domain.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CategoryDTO implements Serializable {


    // @Schema(description = "主键id")
    // @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // @Schema(description = "类型   1 菜品分类 2 套餐分类")
    private Integer type;

    // @Schema(description = "分类名称")
    private String name;

    // @Schema(description = "顺序")
    private Integer sort;
}