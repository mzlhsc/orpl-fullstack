package com.mzlhsc.orpl.emp.controller.admin;

import cn.hutool.core.bean.BeanUtil;
import com.mzlhsc.orpl.common.exception.BadRequestException;
import com.mzlhsc.orpl.common.exception.BizIllegalException;
import com.mzlhsc.orpl.common.exception.NotFoundException;
import com.mzlhsc.orpl.common.constant.MessageConstant;
import com.mzlhsc.orpl.common.result.PageDTO;
import com.mzlhsc.orpl.emp.constant.PasswordConstant;
import com.mzlhsc.orpl.emp.constant.StatusConstant;
import com.mzlhsc.orpl.emp.domain.dto.EditPasswordDTO;
import com.mzlhsc.orpl.emp.domain.dto.EmployeeDTO;
import com.mzlhsc.orpl.emp.domain.dto.EmployeeLoginDTO;
import com.mzlhsc.orpl.emp.domain.dto.EmployeePageQueryDTO;
import com.mzlhsc.orpl.emp.domain.po.Employee;
import com.mzlhsc.orpl.emp.domain.vo.EmployeeLoginVO;
import com.mzlhsc.orpl.emp.service.IEmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 员工信息 前端控制器
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-07
 */
@RestController
@RequestMapping("/admin/employee")
@RequiredArgsConstructor
@Tag(name = "员工接口", description = "")
@Slf4j
public class EmployeeController {

    private final IEmployeeService employeeService;

    /**
     * 登录接口
     */
    @PostMapping("/login")
    @Operation(summary = "登录接口", description = "账户密码")
    public EmployeeLoginVO login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("用户登录: {}", employeeLoginDTO == null ? null : employeeLoginDTO.getUsername());
        if (employeeLoginDTO == null
                || !StringUtils.hasText(employeeLoginDTO.getUsername())
                || !StringUtils.hasText(employeeLoginDTO.getPassword())) {
            throw new BadRequestException("用户名或密码不能为空");
        }
        return employeeService.login(employeeLoginDTO);
    }

    /**
     * 退出
     */
    @PostMapping("/logout")
    @Operation(summary = "员工退出")
    public String logout() {
        return null;
    }

    /**
     * 添加员工
     */
    @PostMapping
    @Operation(summary = "添加员工")
    public void save(@RequestBody EmployeeDTO employeeDTO) {
        log.info("添加员工: {}", employeeDTO);
        if (employeeDTO == null
                || !StringUtils.hasText(employeeDTO.getName())
                || !StringUtils.hasText(employeeDTO.getUsername())
                || !StringUtils.hasText(employeeDTO.getPhone())
                || !StringUtils.hasText(employeeDTO.getSex())
                || !StringUtils.hasText(employeeDTO.getIdNumber())) {
            throw new BadRequestException("姓名、用户名、手机号、性别和身份证号不能为空");
        }

        Employee employee = BeanUtil.copyProperties(employeeDTO, Employee.class)
                .setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()))
                .setStatus(StatusConstant.ENABLE);

        // 用户名唯一，提前给出友好提示，避免直接抛数据库唯一索引异常
        if (employeeService.lambdaQuery().eq(Employee::getUsername, employee.getUsername()).exists()) {
            throw new BizIllegalException(MessageConstant.ACCOUNT_EXIStS);
        }

        employeeService.save(employee);
    }

    /**
     * 分页查询
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public PageDTO page(EmployeePageQueryDTO employeePageQueryDTO) {
        log.info("分页查询 {}", employeePageQueryDTO);
        return employeeService.PageQuery(employeePageQueryDTO);
    }

    /**
     * 根据id查询员工
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询员工")
    public Employee getById(@PathVariable Long id) {
        log.info("根据id查询员工 {}", id);
        Employee employee = employeeService.getById(id);
        if (employee == null) {
            throw new NotFoundException("员工不存在");
        }
        employee.setPassword(null);
        return employee;
    }

    /**
     * 修改员工信息
     */
    @PutMapping
    @Operation(summary = "修改员工信息")
    public void update(@RequestBody EmployeeDTO employeeDTO) {
        log.info("修改员工信息: {}", employeeDTO);
        if (employeeDTO == null || employeeDTO.getId() == null) {
            throw new BadRequestException("员工id不能为空");
        }
        if (employeeService.getById(employeeDTO.getId()) == null) {
            throw new NotFoundException("员工不存在");
        }
        if (employeeDTO.getUsername() != null && !StringUtils.hasText(employeeDTO.getUsername())) {
            throw new BadRequestException("用户名不能为空");
        }
        // 用户名唯一：改成了别人正在用的用户名时提前拒绝，避免数据库唯一索引报 500
        if (StringUtils.hasText(employeeDTO.getUsername())
                && employeeService.lambdaQuery()
                .eq(Employee::getUsername, employeeDTO.getUsername())
                .ne(Employee::getId, employeeDTO.getId())
                .exists()) {
            throw new BizIllegalException(MessageConstant.ACCOUNT_EXIStS);
        }
        // 编辑员工只更新文档中列出的字段，不能顺带改密码和状态
        Employee employee = BeanUtil.copyProperties(employeeDTO, Employee.class);
        // 防止前端误传密码/状态，编辑接口不做这两项的修改
        employee.setPassword(null);
        employee.setStatus(null);

        employeeService.updateById(employee);
    }

    /**
     * 修改员工密码
     */
    @PutMapping("/editPassword")
    @Operation(summary = "修改员工密码")
    public void editPassword(@RequestBody EditPasswordDTO editPasswordDTO) {
        log.info("修改员工密码: {}", editPasswordDTO);
        if (editPasswordDTO == null || editPasswordDTO.getEmpId() == null
                || !StringUtils.hasText(editPasswordDTO.getNewPassword())) {
            throw new BadRequestException("员工id和新密码不能为空");
        }
        employeeService.editPassword(editPasswordDTO);
    }

    /**
     * 员工启禁用
     */
    @PostMapping("/status/{status}")
    @Operation(summary = "员工启禁用")
    public void status(@PathVariable Integer status, @RequestParam Long id) {
        log.info("员工启禁用: id={}, status={}", id, status);
        if (id == null) {
            throw new BadRequestException("员工id不能为空");
        }
        if (status == null || (status != StatusConstant.ENABLE && status != StatusConstant.DISABLE)) {
            throw new BadRequestException("员工状态不合法");
        }
        if (employeeService.getById(id) == null) {
            throw new NotFoundException("员工不存在");
        }

        employeeService.lambdaUpdate()
                .set(Employee::getStatus, status)
                .eq(Employee::getId, id)
                .update();
    }

}
