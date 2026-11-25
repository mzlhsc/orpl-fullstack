package com.mzlhsc.orpl.order.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "堂食预约")
public class DiningReservationDTO implements Serializable {

    @NotNull
    @Schema(description = "桌台id")
    private Long tableId;

    @NotNull
    @Schema(description = "到店时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reserveTime;

    @NotNull
    @Min(1)
    @Schema(description = "就餐人数")
    private Integer guestCount;

    @NotBlank
    @Schema(description = "联系人")
    private String contactName;

    @NotBlank
    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "备注")
    private String remark;
}
