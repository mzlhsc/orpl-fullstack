package com.mzlhsc.orpl.dish.service;

import com.mzlhsc.orpl.dish.mapper.DishMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 菜品统计查询服务，供分类校验、工作台等模块进程内调用。
 */
@Service
@RequiredArgsConstructor
public class DishQueryService {

    private final DishMapper dishMapper;

    /**
     * 统计某个分类下的菜品数量（分类删除前校验）
     */
    public Integer countByCategoryId(Long categoryId) {
        return dishMapper.countByCategoryId(categoryId);
    }

    /**
     * 统计某个状态的菜品数量（工作台菜品总览）
     */
    public Integer countByStatus(Integer status) {
        return dishMapper.countByStatus(status);
    }
}
