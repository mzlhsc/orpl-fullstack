package com.mzlhsc.orpl.shop.service;

import com.mzlhsc.orpl.common.exception.BadRequestException;
import com.mzlhsc.orpl.shop.constant.ShopConstant;
import com.mzlhsc.orpl.shop.domain.dto.ShopAutoAcceptDTO;
import com.mzlhsc.orpl.shop.domain.vo.ShopAutoAcceptVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 店铺运行期设置：营业状态以外的接单策略，统一存储在 Redis。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShopSettingsService {

    private final RedisTemplate<String, Object> redisTemplate;

    public ShopAutoAcceptVO getAutoAccept() {
        return ShopAutoAcceptVO.builder()
                .takeoutAutoAccept(isTakeoutAutoAccept())
                .diningAutoAccept(isDiningAutoAccept())
                .build();
    }

    public boolean isTakeoutAutoAccept() {
        return isOn(ShopConstant.AUTO_ACCEPT_TAKEOUT_KEY);
    }

    public boolean isDiningAutoAccept() {
        return isOn(ShopConstant.AUTO_ACCEPT_DINING_KEY);
    }

    /**
     * 支持只传一个字段：未传的开关保持原值。
     */
    public ShopAutoAcceptVO updateAutoAccept(ShopAutoAcceptDTO dto) {
        if (dto == null || (dto.getTakeoutAutoAccept() == null && dto.getDiningAutoAccept() == null)) {
            throw new BadRequestException("至少需要设置一个自动接单开关");
        }
        if (dto.getTakeoutAutoAccept() != null) {
            setFlag(ShopConstant.AUTO_ACCEPT_TAKEOUT_KEY, dto.getTakeoutAutoAccept());
            log.info("外卖自动接单：{}", dto.getTakeoutAutoAccept() ? "开启" : "关闭");
        }
        if (dto.getDiningAutoAccept() != null) {
            setFlag(ShopConstant.AUTO_ACCEPT_DINING_KEY, dto.getDiningAutoAccept());
            log.info("堂食自动接单：{}", dto.getDiningAutoAccept() ? "开启" : "关闭");
        }
        return getAutoAccept();
    }

    private boolean isOn(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Number number) {
            return number.intValue() == 1;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && "1".equals(String.valueOf(value));
    }

    private void setFlag(String key, Boolean enabled) {
        redisTemplate.opsForValue().set(key, enabled != null && enabled ? 1 : 0);
    }
}
