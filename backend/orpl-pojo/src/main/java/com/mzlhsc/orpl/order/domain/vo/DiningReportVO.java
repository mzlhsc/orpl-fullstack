package com.mzlhsc.orpl.order.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "堂食经营报表")
public class DiningReportVO implements Serializable {

    private Long sessionCount;

    private BigDecimal totalRevenue;

    private Long totalGuests;

    private BigDecimal avgAmount;

    private BigDecimal avgGuests;

    @Schema(description = "桌台周转率 = 已结账会话数 / 启用桌台数")
    private BigDecimal turnoverRate;

    private Integer tableCount;

    private List<Map<String, Object>> daily = new ArrayList<>();
}
