package com.mzlhsc.orpl.dish.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mzlhsc.orpl.common.result.PageDTO;
import com.mzlhsc.orpl.dish.domain.dto.DishDTO;
import com.mzlhsc.orpl.dish.domain.dto.DishPageQueryDTO;
import com.mzlhsc.orpl.dish.domain.po.Dish;
import com.mzlhsc.orpl.dish.domain.vo.DishVO;


import java.util.List;

public interface IDishService extends IService<Dish> {

    /**
     * 新增菜品和对应的口味
     * @param dishDTO
     */
    void saveWithFlavor(DishDTO dishDTO);


    /**
     * 分页查询菜品
     * @param dishPageQueryDTO
     * @return
     */
    PageDTO<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);


    /**
     * 根据id查询菜品及口味
     * @param id
     * @return
     */
    DishVO getByIdWithFlavor(Long id);


    /**
     * 修改菜品
     * @param dishDTO
     */
    void updateWithFlavor(DishDTO dishDTO);

    /**
     * 批量删除菜品
     * @param ids
     */
    void deleteWithFlavor(List<Long> ids);


    /**
     * 用户端根据分类id查询菜品
     * @param categoryId
     * @return
     */
    List<DishVO> listWithFlavor(Long categoryId);
}
