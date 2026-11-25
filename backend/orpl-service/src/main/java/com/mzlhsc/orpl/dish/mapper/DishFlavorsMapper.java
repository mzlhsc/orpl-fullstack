package com.mzlhsc.orpl.dish.mapper;

import com.mzlhsc.orpl.dish.domain.po.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorsMapper {

    /**
     * 口味数据批量插入
     */
    void insertBatch(@Param("flavors") List<DishFlavor> flavors);

    /**
     * 根据菜品id查询对应的口味数据
     */
    @Select("select * from dish_flavor where dish_id = #{dishId}")
    List<DishFlavor> getByDishId(@Param("dishId") Long dishId);

    /**
     * 根据菜品id删除口味
     */
    @Delete("delete from dish_flavor where dish_id = #{dishId}")
    void deleteByDishId(@Param("dishId") Long dishId);

    /**
     * 根据菜品id集合批量删除关联的口味数据
     */
    void deleteByDishIds(@Param("dishIds") List<Long> dishIds);
}
