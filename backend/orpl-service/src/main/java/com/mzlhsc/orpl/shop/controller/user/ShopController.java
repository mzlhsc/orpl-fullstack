package com.mzlhsc.orpl.shop.controller.user;

import com.mzlhsc.orpl.common.result.R;
import com.mzlhsc.orpl.shop.constant.ShopConstant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * 店铺管理
 */
@RestController("userShopController")
@RequestMapping("/user/shop")
@RequiredArgsConstructor
@Tag(name = "店铺操作相关接口", description = "店铺营业状态的查询接口")
@Slf4j
public class ShopController {

    private final RedisTemplate<String, Object> redisTemplate;


    /**
     * 查询店铺营业状态
     * @return
     */
    @GetMapping("/status")
    @Operation(summary = "查询店铺营业状态")
    public R<Integer> getStatus(){
        Integer status = (Integer) redisTemplate.opsForValue().get(ShopConstant.KEY);
        // Redis 中还没有值时（首次启动）默认按打烊处理，避免自动拆箱 NPE
        if (status == null) {
            status = 0;
        }
        log.info("查询营业状态为：{}", status == 1 ? "营业中" : "打烊中");

        return R.ok(status);

    }


}

