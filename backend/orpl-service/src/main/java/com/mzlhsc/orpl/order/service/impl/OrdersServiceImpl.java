package com.mzlhsc.orpl.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mzlhsc.orpl.user.domain.po.AddressBook;
import com.mzlhsc.orpl.user.domain.po.User;
import com.mzlhsc.orpl.user.service.IAddressBookService;
import com.mzlhsc.orpl.user.service.IUserService;


import com.mzlhsc.orpl.common.constant.MessageConstant;
import com.mzlhsc.orpl.common.context.UserContext;
import com.mzlhsc.orpl.common.exception.BadRequestException;
import com.mzlhsc.orpl.common.exception.BizIllegalException;
import com.mzlhsc.orpl.common.exception.CommonException;
import com.mzlhsc.orpl.common.exception.ForbiddenException;
import com.mzlhsc.orpl.common.exception.NotFoundException;
import com.mzlhsc.orpl.common.result.PageDTO;
import com.mzlhsc.orpl.order.domain.dto.OrdersCancelDTO;
import com.mzlhsc.orpl.order.domain.dto.OrdersPageQueryDTO;
import com.mzlhsc.orpl.order.domain.dto.OrdersRejectionDTO;
import com.mzlhsc.orpl.order.domain.dto.OrdersSubmitDTO;
import com.mzlhsc.orpl.order.domain.po.OrderDetail;
import com.mzlhsc.orpl.order.domain.po.Orders;
import com.mzlhsc.orpl.order.domain.po.ShoppingCart;
import com.mzlhsc.orpl.order.domain.vo.OrderStatisticsVO;
import com.mzlhsc.orpl.order.domain.vo.OrderSubmitVO;
import com.mzlhsc.orpl.order.domain.vo.OrderVO;
import com.mzlhsc.orpl.order.mapper.OrderDetailMapper;
import com.mzlhsc.orpl.order.mapper.OrdersMapper;
import com.mzlhsc.orpl.order.service.IOrdersService;
import com.mzlhsc.orpl.order.service.IShoppingCartService;
import com.mzlhsc.orpl.order.service.DishStockManager;
import com.mzlhsc.orpl.order.service.OrderRateLimiter;
import com.mzlhsc.orpl.shop.service.ShopSettingsService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * 订单表 服务实现类
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-09
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrdersServiceImpl extends ServiceImpl<OrdersMapper, Orders> implements IOrdersService {

    private final OrdersMapper ordersMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final IAddressBookService addressBookService;
    private final IUserService userService;
    private final IShoppingCartService shoppingCartService;
    private final DishStockManager dishStockManager;
    private final OrderRateLimiter orderRateLimiter;
    private final ShopSettingsService shopSettingsService;

    /**
     * 订单搜索
     */
    @Override
    public PageDTO<OrderVO> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        int pageNo = ordersPageQueryDTO.getPage() < 1 ? 1 : ordersPageQueryDTO.getPage();
        int pageSize = ordersPageQueryDTO.getPageSize() < 1 ? 10 : ordersPageQueryDTO.getPageSize();
        // 交由 MyBatis-Plus 的 PaginationInnerInterceptor 做物理分页
        Page<Orders> page = new Page<>(pageNo, pageSize);

        // Mapper 返回 List 时 MyBatis-Plus 只做物理分页、不会回填 records，这里手动回填
        List<Orders> records = ordersMapper.pageQuery(page, ordersPageQueryDTO);
        page.setRecords(records);

        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page.getRecords());

        return PageDTO.of(page, orderVOList);
    }

    private List<OrderVO> getOrderVOList(List<Orders> ordersList) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(getOrderDishesStr(orders));
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    /**
     * 根据订单id获取菜品信息字符串（格式：宫保鸡丁*3；）
     */
    private String getOrderDishesStr(Orders orders) {
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
        List<String> orderDishList = orderDetailList.stream()
                .map(x -> x.getName() + "*" + x.getNumber() + ";")
                .collect(Collectors.toList());
        return String.join("", orderDishList);
    }

    /**
     * 查询订单详情
     */
    @Override
    public OrderVO details(Long id) {
        Orders orders = getOwnedOrder(id);

        //查询该订单对应的菜品/套餐明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        //将该订单及其详细封装到OrderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 接单（管理端）
     */
    @Override
    public void confirm(Long id) {
        Orders ordersDB = getById(id);
        if (ordersDB == null) {
            throw new NotFoundException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 订单只有存在且状态为2（待接单）才可以接单
        if (!Objects.equals(ordersDB.getStatus(), Orders.TO_BE_CONFIRMED)) {
            throw new BizIllegalException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 接单后订单状态为“已接单(3)”，可直接完成，不再经过派送中状态
        lambdaUpdate()
                .set(Orders::getStatus, Orders.CONFIRMED)
                .eq(Orders::getId, id)
                .update();
    }

    /**
     * 完成订单
     */
    @Override
    public void complete(Long id) {
        Orders ordersDB = getById(id);
        if (ordersDB == null) {
            throw new NotFoundException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 已接单订单直接完成；堂食订单须由整桌结账完成，避免提前标记已支付
        if (Objects.equals(ordersDB.getOrderType(), Orders.TYPE_DINING)) {
            throw new BizIllegalException("堂食订单请通过整桌结账完成，不能单笔完成");
        }
        if (!Objects.equals(ordersDB.getStatus(), Orders.CONFIRMED)) {
            throw new BizIllegalException(MessageConstant.ORDER_NOT_CONFIRMED);
        }
        LocalDateTime now = LocalDateTime.now();
        lambdaUpdate()
                .set(Orders::getStatus, Orders.COMPLETED)
                .set(Orders::getPayStatus, Orders.PAID)
                .set(Orders::getDeliveryTime, now)
                .set(Orders::getCheckoutTime, now)
                .eq(Orders::getId, id)
                .update();
    }

    /**
     * 修改订单支付状态（管理端）
     */
    @Override
    public void updatePayStatus(Long id, Integer payStatus) {
        if (id == null) {
            throw new BadRequestException("订单id不能为空");
        }
        if (payStatus == null || (!Objects.equals(payStatus, Orders.UN_PAID)
                && !Objects.equals(payStatus, Orders.PAID))) {
            throw new BadRequestException("支付状态只能为 0未支付 或 1已支付");
        }
        Orders ordersDB = getOwnedOrder(id);
        if (Objects.equals(ordersDB.getStatus(), Orders.CANCELLED)) {
            throw new BizIllegalException("已取消订单不能修改支付状态");
        }
        if (Objects.equals(ordersDB.getPayStatus(), Orders.REFUND)) {
            throw new BizIllegalException("已退款订单不能修改支付状态");
        }
        // 已完成订单必须保持已支付，避免出现“完成但未支付”的脏数据
        if (Objects.equals(ordersDB.getStatus(), Orders.COMPLETED)
                && Objects.equals(payStatus, Orders.UN_PAID)) {
            throw new BizIllegalException("已完成订单的支付状态必须为已支付");
        }
        lambdaUpdate()
                .set(Orders::getPayStatus, payStatus)
                .eq(Orders::getId, id)
                .update();
    }

    /**
     * 拒单
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        if (ordersRejectionDTO == null || ordersRejectionDTO.getId() == null) {
            throw new BadRequestException("订单id不能为空");
        }
        if (!StringUtils.hasText(ordersRejectionDTO.getRejectionReason())) {
            throw new BadRequestException("拒单原因不能为空");
        }
        Orders ordersDB = getById(ordersRejectionDTO.getId());
        if (ordersDB == null) {
            throw new NotFoundException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 订单只有存在且状态为2（待接单）才可以拒单
        if (!Objects.equals(ordersDB.getStatus(), Orders.TO_BE_CONFIRMED)) {
            throw new BizIllegalException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 拒单需要退款（当前项目未接入支付，仅标记退款状态）
        lambdaUpdate()
                .set(Orders::getStatus, Orders.CANCELLED)
                .set(Orders::getPayStatus, Orders.REFUND)
                .set(Orders::getRejectionReason, ordersRejectionDTO.getRejectionReason())
                .set(Orders::getCancelTime, LocalDateTime.now())
                .eq(Orders::getId, ordersDB.getId())
                .update();
        restoreOrderStock(ordersDB.getId());
    }

    /**
     * 取消订单（管理端可取消任意未完成订单，用户端只能取消自己的订单）
     */
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        if (ordersCancelDTO == null || ordersCancelDTO.getId() == null) {
            throw new BadRequestException("订单id不能为空");
        }
        Orders ordersDB = getOwnedOrder(ordersCancelDTO.getId());

        // 已完成或已取消的订单不能再次取消
        if (Objects.equals(ordersDB.getStatus(), Orders.COMPLETED)
                || Objects.equals(ordersDB.getStatus(), Orders.CANCELLED)) {
            throw new BizIllegalException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 管理端/用户端取消订单，已支付的需要退款（当前项目未接入支付，仅标记退款状态）
        String reason = ordersCancelDTO.getCancelReason();
        if (!StringUtils.hasText(reason)) {
            reason = UserContext.isAdmin() ? "管理员取消订单" : "用户取消订单";
        }
        lambdaUpdate()
                .set(Orders::getStatus, Orders.CANCELLED)
                .set(Orders::getCancelReason, reason)
                .set(Orders::getCancelTime, LocalDateTime.now())
                .set(Orders::getPayStatus, Objects.equals(ordersDB.getPayStatus(), Orders.PAID)
                        ? Orders.REFUND : ordersDB.getPayStatus())
                .eq(Orders::getId, ordersDB.getId())
                .update();
        restoreOrderStock(ordersDB.getId());
    }

    /**
     * 各个状态的订单数量统计
     */
    @Override
    public OrderStatisticsVO statistics() {
        // 派送中状态已删除，只统计待接单与已接单
        Integer toBeConfirmed = ordersMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = ordersMapper.countStatus(Orders.CONFIRMED);

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        return orderStatisticsVO;
    }

    /**
     * 用户下单。
     * <p>
     * 单体事务：下单过程中会本地查询用户地址簿，
     * 一旦后续步骤失败，本地 @Transactional 会回滚订单/订单明细/购物车变更。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        Long userId = requireLogin();
        orderRateLimiter.check(userId);
        if (ordersSubmitDTO == null || ordersSubmitDTO.getAddressBookId() == null) {
            throw new BizIllegalException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        AddressBook addressBook = addressBookService.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new NotFoundException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        if (addressBook.getUserId() != null && !Objects.equals(addressBook.getUserId(), userId)) {
            throw new ForbiddenException("收货地址不属于当前用户");
        }

        //查询当前用户的购物车数据
        List<ShoppingCart> shoppingCartList = shoppingCartService.lambdaQuery()
                .eq(ShoppingCart::getUserId, userId)
                .isNull(ShoppingCart::getDiningSessionId)
                .list();
        if (CollectionUtils.isEmpty(shoppingCartList)) {
            throw new BizIllegalException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //构造订单数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, order);
        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setUserId(userId);
        // 项目未接入微信支付：外卖下单即视为已支付；开启外卖自动接单时直接进入已接单
        order.setStatus(shopSettingsService.isTakeoutAutoAccept()
                ? Orders.CONFIRMED : Orders.TO_BE_CONFIRMED);
        order.setPayStatus(Orders.PAID);
        order.setOrderTime(LocalDateTime.now());
        order.setCheckoutTime(LocalDateTime.now());
        if (order.getPayMethod() == null || order.getPayMethod() <= 0) {
            order.setPayMethod(1);
        }
        // 打包费不允许为负数，避免前端传负数把订单金额算小
        if (order.getPackAmount() == null || order.getPackAmount() < 0) {
            order.setPackAmount(0);
        }
        // 用户昵称，查不到不影响下单
        try {
            User user = userService.getById(userId);
            if (user != null) {
                order.setUserName(user.getName());
            }
        } catch (Exception e) {
            log.warn("查询用户信息失败，跳过 userName 填充: {}", e.getMessage());
        }
        // 金额以服务端购物车为准，避免前端篡改
        order.setAmount(calcAmount(shoppingCartList, order.getPackAmount()));
        order.setOrderType(1);

        // 外卖订单同样受菜品库存限制：先扣库存，失败直接拦截下单
        List<DishStockManager.StockItem> stockItems = shoppingCartList.stream()
                .filter(cart -> cart.getDishId() != null && cart.getNumber() != null && cart.getNumber() > 0)
                .map(cart -> new DishStockManager.StockItem(cart.getDishId(), cart.getNumber(), cart.getName()))
                .toList();
        dishStockManager.deductDishes(stockItems);

        try {
            //向订单表插入1条数据
            save(order);

            //订单明细数据
            List<OrderDetail> orderDetailList = new ArrayList<>();
            for (ShoppingCart cart : shoppingCartList) {
                OrderDetail orderDetail = new OrderDetail();
                BeanUtils.copyProperties(cart, orderDetail);
                orderDetail.setId(null);
                orderDetail.setOrderId(order.getId());
                orderDetail.setDiningRound(1);
                orderDetailList.add(orderDetail);
            }

            //向明细表插入n条数据
            orderDetailMapper.insertBatch(orderDetailList);

            //清理购物车中的数据
            shoppingCartService.lambdaUpdate()
                    .eq(ShoppingCart::getUserId, userId)
                    .isNull(ShoppingCart::getDiningSessionId)
                    .remove();
        } catch (RuntimeException e) {
            stockItems.forEach(item -> dishStockManager.restore(item.dishId(), item.number()));
            throw e;
        }

        //封装返回结果
        return OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();
    }

    /**
     * 用户端订单分页查询（历史订单）
     */
    @Override
    public PageDTO<OrderVO> pageQuery4User(int page, int pageSize, Integer status) {
        Long userId = requireLogin();
        int pageNo = page < 1 ? 1 : page;
        int size = pageSize < 1 ? 10 : pageSize;
        // 设置分页（MyBatis-Plus 物理分页）
        Page<Orders> mpPage = new Page<>(pageNo, size);

        // 封装查询条件：当前登录用户 + 订单状态（状态为空则查全部）
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(userId);
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setPage(pageNo);
        ordersPageQueryDTO.setPageSize(size);

        // 分页条件查询
        List<Orders> records = ordersMapper.pageQuery(mpPage, ordersPageQueryDTO);
        mpPage.setRecords(records);

        // 查询出订单明细，并封装入OrderVO进行响应
        List<OrderVO> orderVOList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(mpPage.getRecords())) {
            for (Orders orders : mpPage.getRecords()) {
                // 查询订单明细
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetailList);

                orderVOList.add(orderVO);
            }
        }

        return PageDTO.of(mpPage, orderVOList);
    }

    /**
     * 再来一单
     */
    @Override
    public void repetition(Long id) {
        Long userId = requireLogin();
        Orders order = getOwnedOrder(id);

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(order.getId());
        if (CollectionUtils.isEmpty(orderDetailList)) {
            throw new NotFoundException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        shoppingCartService.saveBatch(shoppingCartList);
    }

    /**
     * 用户催单
     */
    @Override
    public void reminder(Long id) {
        // 查询订单是否存在，且属于当前用户
        Orders orders = getOwnedOrder(id);

        //基于WebSocket实现催单 TODO 项目暂未接入 WebSocketServer，先记录日志
        Map<String, Object> map = new HashMap<>();
        map.put("type", 2);//2代表用户催单
        map.put("orderId", id);
        map.put("content", "订单号: " + orders.getNumber());
        log.info("用户催单: {}", JSON.toJSONString(map));
    }


    /**
     * 取消/拒单后回补库存，只从非终态流转一次，避免重复回补。
     */
    private void restoreOrderStock(Long orderId) {
        List<OrderDetail> details = orderDetailMapper.getByOrderId(orderId);
        if (CollectionUtils.isEmpty(details)) {
            return;
        }
        for (OrderDetail detail : details) {
            dishStockManager.restore(detail.getDishId(), detail.getNumber());
        }
    }

    private BigDecimal calcAmount(List<ShoppingCart> carts, Integer packAmount) {
        BigDecimal total = BigDecimal.ZERO;
        for (ShoppingCart cart : carts) {
            BigDecimal price = cart.getAmount() == null ? BigDecimal.ZERO : cart.getAmount();
            int number = cart.getNumber() == null ? 0 : cart.getNumber();
            total = total.add(price.multiply(BigDecimal.valueOf(number)));
        }
        if (packAmount != null) {
            total = total.add(BigDecimal.valueOf(packAmount));
        }
        return total;
    }

    private Long requireLogin() {
        Long userId = UserContext.getCurrentId();
        if (userId == null) {
            throw new BadRequestException(MessageConstant.USER_NOT_LOGIN);
        }
        return userId;
    }

    /**
     * 查询订单并校验权限：管理端可操作任意订单，用户端只能操作自己的订单。
     */
    private Orders getOwnedOrder(Long id) {
        if (id == null) {
            throw new BadRequestException("订单id不能为空");
        }
        Orders orders = getById(id);
        if (orders == null) {
            throw new NotFoundException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (!UserContext.isAdmin()) {
            Long userId = UserContext.getCurrentId();
            if (userId == null || !Objects.equals(orders.getUserId(), userId)) {
                throw new ForbiddenException("无权操作该订单");
            }
        }
        return orders;
    }

}





