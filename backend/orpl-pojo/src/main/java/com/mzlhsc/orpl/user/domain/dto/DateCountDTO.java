package com.mzlhsc.orpl.user.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 日期 + 数量（报表统计用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateCountDTO implements Serializable {

    /** 日期，yyyy-MM-dd */
    private String date;

    /** 数量 */
    private Long count;
}

