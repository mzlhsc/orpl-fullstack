package com.mzlhsc.orpl.emp.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mzlhsc.orpl.common.result.PageDTO;
import com.mzlhsc.orpl.emp.domain.dto.EditPasswordDTO;
import com.mzlhsc.orpl.emp.domain.dto.EmployeeLoginDTO;
import com.mzlhsc.orpl.emp.domain.dto.EmployeePageQueryDTO;
import com.mzlhsc.orpl.emp.domain.po.Employee;
import com.mzlhsc.orpl.emp.domain.vo.EmployeeLoginVO;


/**
 * <p>
 * 员工信息 服务类
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-07
 */
public interface IEmployeeService extends IService<Employee> {

    /**
     * //登录校验,并获取用户数据
     * @param employeeLoginDTO
     * @return
     */
    EmployeeLoginVO login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 分页查询
     *
     * @param employeePageQueryDTO
     * @return
     */
    PageDTO<Employee> PageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 修改密码
     * @param editPasswordDTO
     */
    void editPassword(EditPasswordDTO editPasswordDTO);

}
