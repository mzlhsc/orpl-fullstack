package com.mzlhsc.orpl.setmeal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mzlhsc.orpl.setmeal.domain.vo.DishItemVO;
import com.mzlhsc.orpl.setmeal.domain.dto.SetmealPageQueryDTO;
import com.mzlhsc.orpl.setmeal.domain.po.Setmeal;
import com.mzlhsc.orpl.setmeal.domain.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 套餐 Mapper 接口
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-09
 */
@Mapper
public interface SetmealMapper extends BaseMapper<Setmeal> {

    /**
     * 套餐分页查询（由 MyBatis-Plus 的 PaginationInnerInterceptor 负责物理分页）
     * @param page 分页参数
     * @param setmealPageQueryDTO 查询条件
     * @return 分页结果
     */
    List<SetmealVO> pageQuery(Page<SetmealVO> page, @Param("dto") SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 根据分类id查询套餐数量
     * @param category_id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 按状态统计套餐数量（工作台套餐总览）
     * @param map 参数：status
     */
    Integer countByMap(Map map);

    /**
     * 根据套餐id查询菜品选项
     * @param setmealId
     * @return
     */
    @Select("select sd.name, sd.copies, d.image, d.description " +
            "from setmeal_dish sd left join dish d on sd.dish_id = d.id " +
            "where sd.setmeal_id = #{setmealId}")
    List<DishItemVO> getDishItemBySetmealId(@Param("setmealId") Long setmealId);
}

