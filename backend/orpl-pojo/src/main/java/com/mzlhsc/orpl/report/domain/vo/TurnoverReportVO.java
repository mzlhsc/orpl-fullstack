package com.mzlhsc.orpl.report.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 营业额统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnoverReportVO implements Serializable {

    //日期列表，以逗号分隔
    private String dateList;

    //营业额列表，以逗号分隔
    private String turnoverList;
}
