package com.mzlhsc.orpl.ctgy.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mzlhsc.orpl.common.result.PageDTO;
import com.mzlhsc.orpl.ctgy.domain.dto.CategoryPageQueryDTO;
import com.mzlhsc.orpl.ctgy.domain.po.Category;


/**
 * <p>
 * 菜品及套餐分类 服务类
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-06
 */
public interface ICategoryService extends IService<Category> {

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    PageDTO<Category> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);


    /**
     * 删除没有菜品的菜品分类
     * @param id
     */
    void removeNoDishByIds(Long id);
}
