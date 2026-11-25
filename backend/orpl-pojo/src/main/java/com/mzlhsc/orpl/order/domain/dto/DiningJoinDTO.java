package com.mzlhsc.orpl.order.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "加入堂食会话")
public class DiningJoinDTO implements Serializable {

    @NotNull
    @Schema(description = "会话id")
    private Long sessionId;

    @Schema(description = "昵称")
    private String nickname;
}
