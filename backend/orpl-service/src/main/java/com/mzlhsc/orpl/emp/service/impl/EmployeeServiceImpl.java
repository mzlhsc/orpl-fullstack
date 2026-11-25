package com.mzlhsc.orpl.emp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;

import com.mzlhsc.orpl.common.constant.MessageConstant;
import com.mzlhsc.orpl.common.exception.BadRequestException;
import com.mzlhsc.orpl.common.exception.BizIllegalException;
import com.mzlhsc.orpl.common.exception.CommonException;
import com.mzlhsc.orpl.common.result.PageDTO;
import com.mzlhsc.orpl.emp.constant.StatusConstant;
import com.mzlhsc.orpl.emp.domain.dto.EditPasswordDTO;
import com.mzlhsc.orpl.emp.domain.dto.EmployeeLoginDTO;
import com.mzlhsc.orpl.emp.domain.dto.EmployeePageQueryDTO;
import com.mzlhsc.orpl.emp.domain.po.Employee;
import com.mzlhsc.orpl.emp.domain.vo.EmployeeLoginVO;
import com.mzlhsc.orpl.emp.mapper.EmployeeMapper;
import com.mzlhsc.orpl.emp.service.IEmployeeService;
import com.mzlhsc.orpl.auth.config.JwtProperties;
import com.mzlhsc.orpl.auth.util.JwtTool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Objects;

/**
 * <p>
 * 员工信息 服务实现类
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-07
 */
@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements IEmployeeService {
    private final EmployeeMapper employeeMapper;

    private final JwtTool jwtTool;

    private final JwtProperties jwtProperties;



    /**
     * 登录校验,并获取用户数据
     * @param employeeLoginDTO
     * @return
     */
    @Override
    public EmployeeLoginVO login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        // Employee employee = employeeMapper.getByUsername(username);
        // 2.根据用户名或手机号查询
        Employee employee = lambdaQuery().eq(Employee::getUsername, username).one();

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new CommonException(MessageConstant.ACCOUNT_NOT_FOUND, 404);
        }

        //密码比对
        // 对前端密码加密
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new BizIllegalException(MessageConstant.PASSWORD_ERROR);
        }

        if (StatusConstant.DISABLE.equals(employee.getStatus())) {
            //账号被锁定
            throw new BizIllegalException(MessageConstant.ACCOUNT_LOCKED);
        }

        //生成令牌
        // Map<String, Object> claims = new HashMap<>();
        // claims.put("id", employee.getId());
        // claims.put("name", employee.getName());
        // String token = JwtUtils.generateToken(claims);
        String token = jwtTool.empCreateToken(employee.getId(), jwtProperties.getTokenTTL());

        //登录数据
        return EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName()).
                token(token).
                build();
    }

    /**
     * 分页查询
     *
     * @param employeePageQueryDTO
     * @return
     */
    @Override
    public PageDTO<Employee> PageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        // 页码/每页条数兜底，避免前端不传时出现 current=0 的非法分页
        int pageNo = employeePageQueryDTO.getPage() < 1 ? 1 : employeePageQueryDTO.getPage();
        int pageSize = employeePageQueryDTO.getPageSize() < 1 ? 10 : employeePageQueryDTO.getPageSize();
        // 交由 MyBatis-Plus 的 PaginationInnerInterceptor 做物理分页
        Page<Employee> page = new Page<>(pageNo, pageSize);
        // 分页返回员工完整信息（文档要求返回 username/phone/sex/idNumber/status 等字段）
        employeeMapper.pageQuery(page, employeePageQueryDTO.getName());
        // 密码属于敏感字段，不返回给前端
        page.getRecords().forEach(employee -> employee.setPassword(null));
        return PageDTO.of(page);
    }

    /**
     * 修改密码
     * @param editPasswordDTO
     */
    @Override
    public void editPassword(EditPasswordDTO editPasswordDTO) {
        // 入参非空校验，避免前端漏传时在后面对 null 调方法
        if (editPasswordDTO.getEmpId() == null
                || editPasswordDTO.getOldPassword() == null
                || editPasswordDTO.getNewPassword() == null) {
            throw new BadRequestException("有参数为空");
        }

        Employee employee = getById(editPasswordDTO.getEmpId());
        if (employee == null) {
            // 员工不存在
            throw new CommonException(MessageConstant.ACCOUNT_NOT_FOUND, 404);
        }

        // 前端要校验双重保险
        if(Objects.equals(editPasswordDTO.getOldPassword(),editPasswordDTO.getNewPassword())){
            // 新密码与旧密码相同，不允许修改
            throw new BizIllegalException(MessageConstant.PASSWORD_IDENTICAL);
        }
        // 输入密码加密后认证
        editPasswordDTO.setOldPassword(DigestUtils.md5DigestAsHex(editPasswordDTO.getOldPassword().getBytes()));
        if(!Objects.equals(editPasswordDTO.getOldPassword(), employee.getPassword())){
            // 原密码错误
            throw new BizIllegalException(MessageConstant.PASSWORD_ERROR);
        }

        // 加密
        editPasswordDTO.setNewPassword(DigestUtils.md5DigestAsHex(editPasswordDTO.getNewPassword().getBytes()));

        lambdaUpdate().
                set(Employee::getPassword,editPasswordDTO.getNewPassword())
                .eq(Employee::getId,editPasswordDTO.getEmpId())
                .update();

    }





}
