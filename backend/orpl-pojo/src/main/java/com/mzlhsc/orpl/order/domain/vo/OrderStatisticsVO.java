package com.mzlhsc.orpl.order.domain.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderStatisticsVO implements Serializable {
    //待接单数量
    private Integer toBeConfirmed;

    //已接单数量（待处理）
    private Integer confirmed;
}
