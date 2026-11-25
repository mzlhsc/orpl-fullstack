package com.mzlhsc.orpl.user.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 地址簿
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("address_book")
@Schema(name = "AddressBook对象", description="地址簿")
public class AddressBook implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(name = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(name = "用户id")
    private Long userId;

    @Schema(name = "收货人")
    private String consignee;

    @Schema(name = "性别")
    private String sex;

    @Schema(name = "手机号")
    private String phone;

    @Schema(name = "省级区划编号")
    private String provinceCode;

    @Schema(name = "省级名称")
    private String provinceName;

    @Schema(name = "市级区划编号")
    private String cityCode;

    @Schema(name = "市级名称")
    private String cityName;

    @Schema(name = "区级区划编号")
    private String districtCode;

    @Schema(name = "区级名称")
    private String districtName;

    @Schema(name = "详细地址")
    private String detail;

    @Schema(name = "标签")
    private String label;

    @Schema(name = "默认 0 否 1是")
    private Boolean isDefault;


}
