package com.mzlhsc.orpl.report.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReportVO implements Serializable {

    //日期列表，以逗号分隔
    private String dateList;

    //总用户量列表，以逗号分隔
    private String totalUserList;

    //新增用户数列表，以逗号分隔
    private String newUserList;
}
