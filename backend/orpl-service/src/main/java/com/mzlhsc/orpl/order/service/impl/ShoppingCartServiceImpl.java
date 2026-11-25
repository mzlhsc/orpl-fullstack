package com.mzlhsc.orpl.order.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mzlhsc.orpl.dish.domain.po.Dish;
import com.mzlhsc.orpl.setmeal.domain.po.Setmeal;
import com.mzlhsc.orpl.dish.service.IDishService;
import com.mzlhsc.orpl.setmeal.service.ISetmealService;
import com.mzlhsc.orpl.common.constant.MessageConstant;
import com.mzlhsc.orpl.common.context.UserContext;
import com.mzlhsc.orpl.common.exception.BadRequestException;
import com.mzlhsc.orpl.common.exception.BizIllegalException;
import com.mzlhsc.orpl.common.exception.NotFoundException;
import com.mzlhsc.orpl.order.domain.dto.ShoppingCartDTO;
import com.mzlhsc.orpl.order.domain.po.ShoppingCart;
import com.mzlhsc.orpl.order.mapper.ShoppingCartMapper;
import com.mzlhsc.orpl.order.service.IShoppingCartService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 购物车 服务实现类
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-13
 */
@Service
@RequiredArgsConstructor
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements IShoppingCartService {

    private final IDishService dishService;
    private final ISetmealService setmealService;

    /**
     * 添加购物车
     */
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        Long userId = requireLogin();
        validateGoods(shoppingCartDTO);

        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        //只能查询自己的购物车数据
        shoppingCart.setUserId(userId);

        //判断当前商品是否在购物车中（菜品/套餐、口味为空时不能拼 = null 条件）
        List<ShoppingCart> shoppingCartList = lambdaQuery()
                .eq(ShoppingCart::getUserId, userId)
                .isNull(ShoppingCart::getDiningSessionId)
                .eq(shoppingCart.getDishId() != null, ShoppingCart::getDishId, shoppingCart.getDishId())
                .eq(shoppingCart.getSetmealId() != null, ShoppingCart::getSetmealId, shoppingCart.getSetmealId())
                .eq(shoppingCart.getDishFlavor() != null, ShoppingCart::getDishFlavor, shoppingCart.getDishFlavor())
                .list();

        if (shoppingCartList.size() == 1) {
            //如果已经存在,就更新数量,数量加一
            ShoppingCart exist = shoppingCartList.get(0);
            int number = exist.getNumber() == null ? 1 : exist.getNumber() + 1;
            lambdaUpdate()
                    .set(ShoppingCart::getNumber, number)
                    .eq(ShoppingCart::getId, exist.getId())
                    .update();
            return;
        }
        if (shoppingCartList.size() > 1) {
            // 同一商品出现多条记录，说明历史数据有问题，取第一条并清理多余的
            ShoppingCart exist = shoppingCartList.get(0);
            int number = exist.getNumber() == null ? 1 : exist.getNumber() + 1;
            lambdaUpdate()
                    .set(ShoppingCart::getNumber, number)
                    .eq(ShoppingCart::getId, exist.getId())
                    .update();
            shoppingCartList.subList(1, shoppingCartList.size())
                    .forEach(item -> removeById(item.getId()));
            return;
        }

        //如果不存在,插入数据,数量为一
        Long dishId = shoppingCartDTO.getDishId();
        if (dishId != null) {
            //添加到购物车的是菜品
            Dish dish = dishService.getById(dishId);
            if (dish == null) {
                throw new NotFoundException(MessageConstant.DISH_NOT_FOUND);
            }
            shoppingCart.setName(dish.getName());
            shoppingCart.setImage(dish.getImage());
            shoppingCart.setAmount(dish.getPrice());
        } else {
            //添加到购物车的是套餐
            Setmeal setmeal = setmealService.getById(shoppingCartDTO.getSetmealId());
            if (setmeal == null) {
                throw new NotFoundException(MessageConstant.SETMEAL_NOT_FOUND);
            }
            shoppingCart.setName(setmeal.getName());
            shoppingCart.setImage(setmeal.getImage());
            shoppingCart.setAmount(setmeal.getPrice());
        }
        shoppingCart.setNumber(1);
        shoppingCart.setCreateTime(LocalDateTime.now());
        save(shoppingCart);
    }

    /**
     * 删除购物车中一个商品
     */
    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        Long userId = requireLogin();
        validateGoods(shoppingCartDTO);

        ShoppingCart query = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, query);
        //只能查询自己的购物车数据
        query.setUserId(userId);

        //判断当前商品是否在购物车中
        List<ShoppingCart> shoppingCartList = lambdaQuery()
                .eq(ShoppingCart::getUserId, userId)
                .isNull(ShoppingCart::getDiningSessionId)
                .eq(query.getDishId() != null, ShoppingCart::getDishId, query.getDishId())
                .eq(query.getSetmealId() != null, ShoppingCart::getSetmealId, query.getSetmealId())
                .eq(query.getDishFlavor() != null, ShoppingCart::getDishFlavor, query.getDishFlavor())
                .list();
        if (shoppingCartList.isEmpty()) {
            throw new BizIllegalException(MessageConstant.SHOPPING_CART_NOT_FOUND);
        }

        ShoppingCart shoppingCart = shoppingCartList.get(0);
        if (shoppingCart.getNumber() == null || shoppingCart.getNumber() <= 0) {
            throw new BizIllegalException(MessageConstant.SHOPPING_CART_NOT_FOUND);
        }
        shoppingCart.setNumber(shoppingCart.getNumber() - 1);
        if (shoppingCart.getNumber() == 0) {
            // 数量减到 0，直接删除该条购物车记录
            removeById(shoppingCart.getId());
        } else {
            lambdaUpdate()
                    .set(ShoppingCart::getNumber, shoppingCart.getNumber())
                    .eq(ShoppingCart::getId, shoppingCart.getId())
                    .update();
        }
    }

    private Long requireLogin() {
        Long userId = UserContext.getCurrentId();
        if (userId == null) {
            throw new BadRequestException(MessageConstant.USER_NOT_LOGIN);
        }
        return userId;
    }

    private void validateGoods(ShoppingCartDTO dto) {
        if (dto == null) {
            throw new BadRequestException("购物车参数不能为空");
        }
        boolean hasDish = dto.getDishId() != null;
        boolean hasSetmeal = dto.getSetmealId() != null;
        if (hasDish == hasSetmeal) {
            throw new BadRequestException("菜品id和套餐id必须且只能传一个");
        }
    }

}

