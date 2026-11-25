package com.mzlhsc.orpl.order.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "并桌")
public class DiningMergeDTO implements Serializable {

    @NotEmpty
    @Schema(description = "被并入的会话id列表")
    private List<Long> sourceSessionIds;

    @Schema(description = "备注")
    private String remark;
}
