package com.mzlhsc.orpl.setmeal.service;

import com.mzlhsc.orpl.setmeal.mapper.SetmealDishMapper;
import com.mzlhsc.orpl.setmeal.mapper.SetmealMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 套餐统计查询服务，供分类/菜品校验、工作台等模块进程内调用。
 */
@Service
@RequiredArgsConstructor
public class SetmealQueryService {

    private final SetmealMapper setmealMapper;
    private final SetmealDishMapper setmealDishMapper;

    /**
     * 统计某个分类下的套餐数量（分类删除前校验）
     */
    public Integer countByCategoryId(Long categoryId) {
        return setmealMapper.countByCategoryId(categoryId);
    }

    /**
     * 统计这些菜品被多少个套餐引用（菜品删除前校验）
     */
    public Integer countByDishIds(List<Long> dishIds) {
        if (dishIds == null || dishIds.isEmpty()) {
            return 0;
        }
        return new HashSet<>(setmealDishMapper.getSetmealIdsByDishIds(dishIds)).size();
    }

    /**
     * 统计某个状态的套餐数量（工作台套餐总览）
     */
    public Integer countByStatus(Integer status) {
        Map<String, Object> map = new HashMap<>(2);
        map.put("status", status);
        return setmealMapper.countByMap(map);
    }
}
