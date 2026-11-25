package com.mzlhsc.orpl.order.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 日期 + 金额（营业额统计用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateAmountDTO implements Serializable {

    /** 日期，yyyy-MM-dd */
    private String date;

    /** 金额 */
    private BigDecimal amount;
}

