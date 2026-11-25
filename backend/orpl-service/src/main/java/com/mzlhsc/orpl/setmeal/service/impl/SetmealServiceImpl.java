package com.mzlhsc.orpl.setmeal.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mzlhsc.orpl.setmeal.domain.vo.DishItemVO;
import com.mzlhsc.orpl.common.constant.MessageConstant;
import com.mzlhsc.orpl.common.exception.BadRequestException;
import com.mzlhsc.orpl.common.exception.BizIllegalException;
import com.mzlhsc.orpl.common.exception.NotFoundException;
import com.mzlhsc.orpl.common.result.PageDTO;
import com.mzlhsc.orpl.setmeal.constant.StatusConstant;
import com.mzlhsc.orpl.setmeal.domain.dto.SetmealDTO;
import com.mzlhsc.orpl.setmeal.domain.dto.SetmealPageQueryDTO;
import com.mzlhsc.orpl.setmeal.domain.po.Setmeal;
import com.mzlhsc.orpl.setmeal.domain.po.SetmealDish;
import com.mzlhsc.orpl.setmeal.domain.vo.SetmealVO;
import com.mzlhsc.orpl.setmeal.mapper.SetmealDishMapper;
import com.mzlhsc.orpl.setmeal.mapper.SetmealMapper;
import com.mzlhsc.orpl.setmeal.service.ISetmealService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 套餐 服务实现类
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-09
 */
@Service
@RequiredArgsConstructor
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements ISetmealService {

    private final SetmealMapper setmealMapper;
    private final SetmealDishMapper setmealDishMapper;

    /**
     * 添加套餐和对应菜品
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveInDish(SetmealDTO setmealDTO) {
        validateSetmeal(setmealDTO);

        // 必须接收 copyProperties 的返回值，否则套餐对象是空的
        Setmeal setmeal = BeanUtil.copyProperties(setmealDTO, Setmeal.class);
        setmeal.setId(null);
        if (setmeal.getStatus() == null) {
            setmeal.setStatus(StatusConstant.DISABLE);
        }
        //添加套餐
        save(setmeal);

        //获取套餐id
        Long setmealId = setmeal.getId();

        //添加套餐菜品
        insertBatch(setmealDTO.getSetmealDishes(), setmealId);
    }

    /**
     * 套餐分页查询
     */
    @Override
    public PageDTO<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        int pageNo = setmealPageQueryDTO.getPage() < 1 ? 1 : setmealPageQueryDTO.getPage();
        int pageSize = setmealPageQueryDTO.getPageSize() < 1 ? 10 : setmealPageQueryDTO.getPageSize();
        // 第一个参数为 IPage 时，MyBatis-Plus 分页拦截器自动拼接 count 与 limit
        Page<SetmealVO> page = new Page<>(pageNo, pageSize);
        // Mapper 返回 List 时 MyBatis-Plus 只做物理分页、不会回填 records，这里手动回填
        List<SetmealVO> records = setmealMapper.pageQuery(page, setmealPageQueryDTO);
        page.setRecords(records);
        return PageDTO.of(page);
    }

    /**
     * 修改套餐和对应菜品
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInDish(SetmealDTO setmealDTO) {
        if (setmealDTO == null || setmealDTO.getId() == null) {
            throw new BadRequestException("套餐id不能为空");
        }
        if (getById(setmealDTO.getId()) == null) {
            throw new NotFoundException(MessageConstant.SETMEAL_NOT_FOUND);
        }

        // 必须接收 copyProperties 的返回值
        Setmeal setmeal = BeanUtil.copyProperties(setmealDTO, Setmeal.class);
        //修改套餐
        updateById(setmeal);

        //获取套餐id
        Long setmealId = setmeal.getId();

        //删除套餐菜品
        setmealDishMapper.deleteBySetmealId(setmealId);

        //添加套餐菜品
        insertBatch(setmealDTO.getSetmealDishes(), setmealId);
    }

    /**
     * 删除套餐和关联的菜品
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeNoDishByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BadRequestException("套餐id不能为空");
        }
        //判断当前套餐是否能删除--是否存在在售套餐
        List<Setmeal> setmeals = listByIds(ids);
        if (setmeals.size() != ids.size()) {
            throw new NotFoundException(MessageConstant.SETMEAL_NOT_FOUND);
        }
        for (Setmeal setmeal : setmeals) {
            if (Objects.equals(setmeal.getStatus(), StatusConstant.ENABLE)) {
                //当前套餐处于售中,不能删除
                throw new BizIllegalException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        //根据套餐id批量删除套餐
        removeByIds(ids);

        //根据套餐id集合批量删除关联的菜品数据
        setmealDishMapper.deleteBySetmealIds(ids);
    }

    /**
     * 根据套餐id查询菜品选项
     */
    @Override
    public List<DishItemVO> getDishItemById(Long id) {
        if (id == null) {
            throw new BadRequestException("套餐id不能为空");
        }
        return setmealMapper.getDishItemBySetmealId(id);
    }

    /**
     * 根据id查询套餐及其关联的菜品关系
     */
    @Override
    public SetmealVO getByIdWithDishes(Long id) {
        if (id == null) {
            throw new BadRequestException("套餐id不能为空");
        }
        Setmeal setmeal = this.getById(id);
        if (setmeal == null) {
            throw new NotFoundException(MessageConstant.SETMEAL_NOT_FOUND);
        }
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishMapper.getBySetmealId(id));
        return setmealVO;
    }

    /**
     * 添加套餐菜品
     */
    public void insertBatch(List<SetmealDish> dishes, Long setmealId) {
        if (CollectionUtils.isEmpty(dishes)) {
            return;
        }
        dishes.forEach(setmealDish -> {
            setmealDish.setId(null);
            setmealDish.setSetmealId(setmealId);
        });
        //向菜品表插入n条数据
        setmealDishMapper.insertBatch(dishes);
    }

    private void validateSetmeal(SetmealDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getName())) {
            throw new BadRequestException("套餐名称不能为空");
        }
        if (dto.getCategoryId() == null) {
            throw new BadRequestException("套餐分类不能为空");
        }
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("套餐价格不合法");
        }
        if (CollectionUtils.isEmpty(dto.getSetmealDishes())) {
            throw new BadRequestException("套餐菜品不能为空");
        }
    }

}

