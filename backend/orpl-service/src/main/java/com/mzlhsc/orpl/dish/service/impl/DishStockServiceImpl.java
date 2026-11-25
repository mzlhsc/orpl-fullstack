package com.mzlhsc.orpl.dish.service.impl;

import com.mzlhsc.orpl.common.exception.BadRequestException;
import com.mzlhsc.orpl.common.exception.BizIllegalException;
import com.mzlhsc.orpl.common.exception.NotFoundException;
import com.mzlhsc.orpl.dish.domain.po.Dish;
import com.mzlhsc.orpl.dish.mapper.DishMapper;
import com.mzlhsc.orpl.dish.service.IDishService;
import com.mzlhsc.orpl.dish.service.IDishStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DishStockServiceImpl implements IDishStockService {

    private static final String STOCK_KEY = "dish:stock:";
    private static final String LOCK_KEY = "dish:stock:lock:";

    private final IDishService dishService;
    private final DishMapper dishMapper;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public int getStock(Long dishId) {
        Dish dish = requireDish(dishId);
        Integer cached = readCache(dishId);
        if (cached != null) {
            return cached;
        }
        int dbStock = dish.getStock() == null ? 0 : dish.getStock();
        writeCache(dishId, dbStock);
        return dbStock;
    }

    @Override
    public void deduct(Long dishId, int number) {
        if (number <= 0) {
            throw new BadRequestException("扣减库存数量必须大于0");
        }
        Dish dish = requireDish(dishId);
        RLock lock = redissonClient.getLock(LOCK_KEY + dishId);
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 8, TimeUnit.SECONDS);
            if (!locked) {
                throw new BizIllegalException("库存正在被其他订单占用，请稍后重试");
            }
            int dbStock = dish.getStock() == null ? 0 : dish.getStock();
            int cached = readCacheOrInit(dishId, dbStock);
            if (cached < number) {
                throw new BizIllegalException("菜品【" + dish.getName() + "】库存不足，仅剩 " + cached + " 份");
            }
            // 数据库条件更新是最终防线：stock >= number 才扣减
            int rows = dishMapper.deductStock(dishId, number);
            if (rows == 0) {
                writeCache(dishId, Math.max(0, cached));
                throw new BizIllegalException("菜品【" + dish.getName() + "】库存不足");
            }
            writeCache(dishId, Math.max(0, cached - number));
            log.info("库存扣减成功 dishId={} number={} remain={}", dishId, number, cached - number);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizIllegalException("库存扣减被中断，请重试");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public void restore(Long dishId, int number) {
        if (number <= 0) {
            return;
        }
        Dish dish = requireDish(dishId);
        RLock lock = redissonClient.getLock(LOCK_KEY + dishId);
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 8, TimeUnit.SECONDS);
            if (!locked) {
                throw new BizIllegalException("库存回补繁忙，请稍后重试");
            }
            dishMapper.restoreStock(dishId, number);
            Dish latest = dishService.getById(dishId);
            writeCache(dishId, latest == null || latest.getStock() == null ? 0 : latest.getStock());
            log.info("库存回补成功 dishId={} number={}", dishId, number);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizIllegalException("库存回补被中断，请重试");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public void resetStock(Long dishId, int stock) {
        if (stock < 0) {
            throw new BadRequestException("库存不能为负数");
        }
        Dish dish = requireDish(dishId);
        RLock lock = redissonClient.getLock(LOCK_KEY + dishId);
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 8, TimeUnit.SECONDS);
            if (!locked) {
                throw new BizIllegalException("库存正在使用中，请稍后重试");
            }
            dishMapper.updateStock(dishId, stock);
            writeCache(dishId, stock);
            log.info("库存重置成功 dishId={} stock={}", dishId, stock);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizIllegalException("库存重置被中断，请重试");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private Dish requireDish(Long dishId) {
        if (dishId == null) {
            throw new BadRequestException("菜品id不能为空");
        }
        Dish dish = dishService.getById(dishId);
        if (dish == null) {
            throw new NotFoundException("菜品不存在");
        }
        return dish;
    }

    private Integer readCache(Long dishId) {
        Object value = redisTemplate.opsForValue().get(STOCK_KEY + dishId);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private int readCacheOrInit(Long dishId, int dbStock) {
        Integer cached = readCache(dishId);
        if (cached != null) {
            return cached;
        }
        writeCache(dishId, dbStock);
        return dbStock;
    }

    private void writeCache(Long dishId, int stock) {
        redisTemplate.opsForValue().set(STOCK_KEY + dishId, stock);
    }
}
