package com.mzlhsc.orpl.setmeal.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mzlhsc.orpl.setmeal.domain.vo.DishItemVO;
import com.mzlhsc.orpl.common.result.PageDTO;
import com.mzlhsc.orpl.setmeal.domain.dto.SetmealDTO;
import com.mzlhsc.orpl.setmeal.domain.dto.SetmealPageQueryDTO;
import com.mzlhsc.orpl.setmeal.domain.po.Setmeal;
import com.mzlhsc.orpl.setmeal.domain.vo.SetmealVO;

import java.util.List;


/**
 * <p>
 * 套餐 服务类
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-09
 */
public interface ISetmealService extends IService<Setmeal> {

    /**
     * 添加套餐和对应菜品
     * @param setmealDTO
     */
    void saveInDish(SetmealDTO setmealDTO);

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    PageDTO<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 根据id查询套餐和关联菜品
     * @param id
     * @return
     */
    List<DishItemVO> getDishItemById(Long id);

    /**
     * 根据id查询套餐及其关联的菜品关系
     * @param id
     * @return
     */
    SetmealVO getByIdWithDishes(Long id);

    /**
     * 修改套餐和对应菜品
     * @param setmealDTO
     */
    void updateInDish(SetmealDTO setmealDTO);

    /**
     * 删除套餐和关联的菜品
     * @param ids
     */
    void removeNoDishByIds(List<Long> ids);

}

