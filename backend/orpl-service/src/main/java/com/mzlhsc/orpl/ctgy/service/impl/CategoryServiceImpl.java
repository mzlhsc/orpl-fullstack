package com.mzlhsc.orpl.ctgy.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mzlhsc.orpl.dish.service.DishQueryService;
import com.mzlhsc.orpl.setmeal.service.SetmealQueryService;
import com.mzlhsc.orpl.common.exception.BizIllegalException;
import com.mzlhsc.orpl.common.result.PageDTO;
import com.mzlhsc.orpl.ctgy.domain.dto.CategoryPageQueryDTO;
import com.mzlhsc.orpl.ctgy.domain.po.Category;
import com.mzlhsc.orpl.ctgy.mapper.CategoryMapper;
import com.mzlhsc.orpl.ctgy.service.ICategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 菜品及套餐分类 服务实现类
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-06
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {

    private final DishQueryService dishQueryService;
    private final SetmealQueryService setmealQueryService;

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    @Override
    public PageDTO<Category> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        // 页码/每页条数兜底，避免前端不传时出现 current=0 的非法分页
        int pageNo = Math.max(categoryPageQueryDTO.getPage(), 1);
        int pageSize = categoryPageQueryDTO.getPageSize() < 1 ? 10 : categoryPageQueryDTO.getPageSize();
        // 交由 MyBatis-Plus 的 PaginationInnerInterceptor 做物理分页
        Page<Category> page = new Page<>(pageNo, pageSize);
        lambdaQuery()
                .like(StrUtil.isNotBlank(categoryPageQueryDTO.getName()), Category::getName, categoryPageQueryDTO.getName())
                .eq(categoryPageQueryDTO.getType() != null, Category::getType, categoryPageQueryDTO.getType())
                .orderByAsc(Category::getSort)
                .orderByDesc(Category::getCreateTime)
                .page(page);
        return PageDTO.of(page);
    }

    /**
     * 删除没有菜品的菜品分类
     * @param id
     */
    @Override
    public void removeNoDishByIds(Long id) {
        //查询当前分类是否关联了菜品，如果关联了就抛出业务异常
        Integer dishCount = dishQueryService.countByCategoryId(id);
        if (dishCount != null && dishCount > 0) {
            throw new BizIllegalException("当前分类下有菜品，不能删除");
        }

        //查询当前分类是否关联了套餐，如果关联了就抛出业务异常
        Integer setmealCount = setmealQueryService.countByCategoryId(id);
        if (setmealCount != null && setmealCount > 0) {
            throw new BizIllegalException("当前分类下有套餐，不能删除");
        }
        removeById(id);
    }
}

