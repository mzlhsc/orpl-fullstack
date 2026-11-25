package com.mzlhsc.orpl.dish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mzlhsc.orpl.dish.domain.dto.DishPageQueryDTO;
import com.mzlhsc.orpl.dish.domain.po.Dish;
import com.mzlhsc.orpl.dish.domain.vo.DishVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {

    /**
     * 菜品分页查询（第一个参数是 IPage 时由 MyBatis-Plus 自动物理分页）
     * XML 里的条件表达式用 #{dto.name} / #{dto.categoryId} / #{dto.status}
     */
    IPage<DishVO> pageQuery(Page<DishVO> page, @Param("dto") DishPageQueryDTO dto);


    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(@Param("categoryId") Long categoryId);


    /**
     * 根据状态查询菜品数量
     * @param status
     * @return
     */
    @Select("select count(id) from dish where status = #{status}")
    Integer countByStatus(@Param("status") Integer status);

    /**
     * 条件扣减库存：只有剩余库存足够时才扣，返回受影响行数。
     */
    @Update("update dish set stock = stock - #{number}, update_time = now() " +
            "where id = #{dishId} and stock >= #{number}")
    int deductStock(@Param("dishId") Long dishId, @Param("number") int number);

    /**
     * 回补库存。
     */
    @Update("update dish set stock = stock + #{number}, update_time = now() where id = #{dishId}")
    int restoreStock(@Param("dishId") Long dishId, @Param("number") int number);

    /**
     * 设置绝对库存。
     */
    @Update("update dish set stock = #{stock}, update_time = now() where id = #{dishId}")
    int updateStock(@Param("dishId") Long dishId, @Param("stock") int stock);

}
