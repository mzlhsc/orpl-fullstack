package com.mzlhsc.orpl.user.domain.po;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户信息
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user")
@Schema(description="User对象",name = "用户信息")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(name = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(name = "微信用户唯一标识")
    private String openid;

    @Schema(name = "姓名")
    private String name;

    @Schema(name = "手机号")
    private String phone;

    @Schema(name = "性别")
    private String sex;

    @Schema(name = "身份证号")
    private String idNumber;

    @Schema(name = "头像")
    private String avatar;

    @Schema(name = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;


}
