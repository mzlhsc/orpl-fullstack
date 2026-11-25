package com.mzlhsc.orpl.order.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "开台")
public class DiningOpenTableDTO implements Serializable {

    @NotNull
    @Schema(description = "桌台id")
    private Long tableId;

    @NotNull
    @Min(1)
    @Schema(description = "就餐人数")
    private Integer guestCount;

    @Schema(description = "昵称，默认取登录用户")
    private String nickname;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "预约id，到店开台时可传")
    private Long reservationId;
}
