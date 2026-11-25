package com.mzlhsc.orpl.shop.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 自动接单开关响应。
 */
@Data
@Builder
@Schema(description = "自动接单开关")
public class ShopAutoAcceptVO implements Serializable {

    @Schema(description = "外卖自动接单")
    private Boolean takeoutAutoAccept;

    @Schema(description = "堂食自动接单")
    private Boolean diningAutoAccept;
}
