package com.mzlhsc.orpl.emp.domain.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class EditPasswordDTO implements Serializable {

    //员工id（与接口文档保持一致，字段名必须是 empId）
    private Long empId;

    private String newPassword;

    private String oldPassword;

}
