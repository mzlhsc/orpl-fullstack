package com.mzlhsc.orpl.order.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "堂食桌台")
public class DiningTableVO implements Serializable {

    private Long id;

    private String name;

    private Integer seats;

    private String area;

    private Integer status;

    private String qrToken;

    @Schema(description = "扫码内容，前端据此生成二维码")
    private String qrContent;

    private Integer reservationEnabled;

    @Schema(description = "当前会话id")
    private Long currentSessionId;

    @Schema(description = "当前会话号")
    private String currentSessionNo;
}
