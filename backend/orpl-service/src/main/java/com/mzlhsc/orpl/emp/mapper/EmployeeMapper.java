package com.mzlhsc.orpl.emp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mzlhsc.orpl.emp.domain.po.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 员工信息 Mapper 接口
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-07
 */
@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {

    /**
     * 分页查询（由 MyBatis-Plus 的 PaginationInnerInterceptor 负责物理分页）
     * @param page 分页参数
     * @param name 员工姓名，模糊匹配
     * @return 分页结果
     */
    Page<Employee> pageQuery(Page<Employee> page, @Param("name") String name);



}
