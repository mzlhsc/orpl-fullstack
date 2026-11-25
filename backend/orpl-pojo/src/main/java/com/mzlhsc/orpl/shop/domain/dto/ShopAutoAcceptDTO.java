package com.mzlhsc.orpl.shop.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 自动接单开关入参。字段为 null 时表示不修改该开关。
 */
@Data
@Schema(description = "自动接单开关")
public class ShopAutoAcceptDTO implements Serializable {

    @Schema(description = "外卖自动接单")
    private Boolean takeoutAutoAccept;

    @Schema(description = "堂食自动接单")
    private Boolean diningAutoAccept;
}
