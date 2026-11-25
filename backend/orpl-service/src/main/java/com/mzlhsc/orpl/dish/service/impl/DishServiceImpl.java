package com.mzlhsc.orpl.dish.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mzlhsc.orpl.setmeal.service.SetmealQueryService;
import com.mzlhsc.orpl.common.constant.MessageConstant;
import com.mzlhsc.orpl.common.exception.BadRequestException;
import com.mzlhsc.orpl.common.exception.BizIllegalException;
import com.mzlhsc.orpl.common.exception.NotFoundException;
import com.mzlhsc.orpl.common.result.PageDTO;
import com.mzlhsc.orpl.dish.constant.StatusConstant;
import com.mzlhsc.orpl.dish.domain.dto.DishDTO;
import com.mzlhsc.orpl.dish.domain.dto.DishPageQueryDTO;
import com.mzlhsc.orpl.dish.domain.po.Dish;
import com.mzlhsc.orpl.dish.domain.po.DishFlavor;
import com.mzlhsc.orpl.dish.domain.vo.DishVO;
import com.mzlhsc.orpl.dish.mapper.DishFlavorsMapper;
import com.mzlhsc.orpl.dish.mapper.DishMapper;
import com.mzlhsc.orpl.dish.service.IDishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements IDishService {

    private final DishFlavorsMapper dishFlavorsMapper;
    private final DishMapper dishMapper;
    private final SetmealQueryService setmealQueryService;

    /**
     * 新增菜品和对应的口味
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveWithFlavor(DishDTO dishDTO) {
        validateDish(dishDTO);

        //除口味外参数拷贝到菜品表
        Dish dish = BeanUtil.copyProperties(dishDTO, Dish.class);
        //新增时 id 必须由数据库生成，防止前端传 id 覆盖已有数据
        dish.setId(null);
        // 前端不传状态时默认起售
        if (dish.getStatus() == null) {
            dish.setStatus(StatusConstant.ENABLE);
        }
        // 不传库存时默认 100
        if (dish.getStock() == null) {
            dish.setStock(100);
        }
        save(dish);

        //获取添加表的id
        Long dishId = dish.getId();
        saveFlavors(dishId, dishDTO.getFlavors());
    }

    /**
     * 分页查询菜品
     */
    @Override
    public PageDTO<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        int pageNo = dishPageQueryDTO.getPage() < 1 ? 1 : dishPageQueryDTO.getPage();
        int pageSize = dishPageQueryDTO.getPageSize() < 1 ? 10 : dishPageQueryDTO.getPageSize();
        // MyBatis-Plus 物理分页：Mapper 方法第一个参数是 IPage 时自动拼 count / limit
        Page<DishVO> page = new Page<>(pageNo, pageSize);

        // 自定义查询：关联 category 表，返回带 categoryName 的 DishVO
        dishMapper.pageQuery(page, dishPageQueryDTO);

        return PageDTO.of(page);
    }

    /**
     * 根据id查询菜品
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        if (id == null) {
            throw new BadRequestException("菜品id不能为空");
        }
        //根据id查询菜品数据
        Dish dish = this.getById(id);
        if (dish == null) {
            throw new NotFoundException(MessageConstant.DISH_NOT_FOUND);
        }
        //根据id查询菜品对应的口味数据
        List<DishFlavor> dishFlavors = dishFlavorsMapper.getByDishId(id);
        //将查询到的数据封装到VO中
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 修改菜品
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWithFlavor(DishDTO dishDTO) {
        if (dishDTO == null || dishDTO.getId() == null) {
            throw new BadRequestException("菜品id不能为空");
        }
        if (dishDTO.getName() != null && !StringUtils.hasText(dishDTO.getName())) {
            throw new BadRequestException("菜品名称不能为空");
        }
        Dish oldDish = getById(dishDTO.getId());
        if (oldDish == null) {
            throw new NotFoundException(MessageConstant.DISH_NOT_FOUND);
        }

        Dish dish = BeanUtil.copyProperties(dishDTO, Dish.class);
        dish.setUpdateTime(LocalDateTime.now());
        //修改菜品
        this.updateById(dish);

        //删除原有的口味数据
        dishFlavorsMapper.deleteByDishId(dishDTO.getId());

        //重新插入口味数据
        saveFlavors(dishDTO.getId(), dishDTO.getFlavors());
    }

    /**
     * 批量删除菜品
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithFlavor(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BadRequestException("菜品id不能为空");
        }

        //判断当前菜品是否能删除-是否存在起售中的菜品
        List<Dish> dishes = listByIds(ids);
        if (dishes.size() != ids.size()) {
            throw new NotFoundException(MessageConstant.DISH_NOT_FOUND);
        }
        for (Dish dish : dishes) {
            if (Objects.equals(dish.getStatus(), StatusConstant.ENABLE)) {
                //当前菜品处于售中,不能删除
                throw new BizIllegalException(MessageConstant.DISH_ON_SALE);
            }
        }
        //判断当前菜品是否能删除-是否被套餐关联
        Integer count = setmealQueryService.countByDishIds(ids);
        if (count != null && count > 0) {
            throw new BizIllegalException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //根据菜品id集合批量删除菜品数据
        removeBatchByIds(ids);

        //根据菜品id集合批量删除关联的口味数据
        dishFlavorsMapper.deleteByDishIds(ids);
    }

    /**
     * 用户端根据分类查询菜品（只返回起售中的菜品）
     */
    @Override
    public List<DishVO> listWithFlavor(Long categoryId) {
        // 查询起售中的菜品
        List<Dish> dishes = lambdaQuery()
                .eq(Dish::getCategoryId, categoryId)
                .eq(Dish::getStatus, StatusConstant.ENABLE)
                .list();

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish dish : dishes) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish, dishVO);

            //查询口味
            List<DishFlavor> flavors = dishFlavorsMapper.getByDishId(dish.getId());
            dishVO.setFlavors(flavors);

            dishVOList.add(dishVO);
        }
        return dishVOList;
    }

    private void validateDish(DishDTO dishDTO) {
        if (dishDTO == null || !StringUtils.hasText(dishDTO.getName())) {
            throw new BadRequestException("菜品名称不能为空");
        }
        if (dishDTO.getCategoryId() == null) {
            throw new BadRequestException("菜品分类不能为空");
        }
        if (dishDTO.getPrice() == null || dishDTO.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("菜品价格不合法");
        }
        if (dishDTO.getStatus() != null
                && (dishDTO.getStatus() != StatusConstant.ENABLE && dishDTO.getStatus() != StatusConstant.DISABLE)) {
            throw new BadRequestException("菜品状态不合法");
        }
        if (dishDTO.getStock() != null && dishDTO.getStock() < 0) {
            throw new BadRequestException("菜品库存不能为负数");
        }
    }

    private void saveFlavors(Long dishId, List<DishFlavor> flavors) {
        if (CollectionUtils.isEmpty(flavors)) {
            return;
        }
        flavors.forEach(dishFlavor -> {
            dishFlavor.setId(null);
            dishFlavor.setDishId(dishId);
            if (!StringUtils.hasText(dishFlavor.getName())) {
                throw new BadRequestException("口味名称不能为空");
            }
        });
        dishFlavorsMapper.insertBatch(flavors);
    }
}

