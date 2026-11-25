package com.mzlhsc.orpl.order.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 商品销量统计（销量排名 top10 使用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsSalesDTO implements Serializable {

    /** 商品名称 */
    private String name;

    /** 销量 */
    private Integer number;
}

