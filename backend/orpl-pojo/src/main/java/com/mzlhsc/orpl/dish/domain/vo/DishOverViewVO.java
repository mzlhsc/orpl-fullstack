package com.mzlhsc.orpl.dish.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 菜品总览（工作台）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishOverViewVO implements Serializable {

    /** 已启售数量 */
    private Integer sold;

    /** 已停售数量 */
    private Integer discontinued;
}

