package com.mzlhsc.orpl.user.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mzlhsc.orpl.auth.config.JwtProperties;
import com.mzlhsc.orpl.auth.util.JwtTool;
import com.mzlhsc.orpl.common.constant.MessageConstant;
import com.mzlhsc.orpl.common.exception.BadRequestException;
import com.mzlhsc.orpl.common.exception.BizIllegalException;
import com.mzlhsc.orpl.common.properties.WeChatProperties;
import com.mzlhsc.orpl.common.utils.HttpClientUtil;
import com.mzlhsc.orpl.user.domain.dto.UserLoginDTO;
import com.mzlhsc.orpl.user.domain.po.User;
import com.mzlhsc.orpl.user.domain.vo.UserLoginVo;
import com.mzlhsc.orpl.user.mapper.UserMapper;
import com.mzlhsc.orpl.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 用户信息 服务实现类
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-12
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    //微信服务接口地址
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    private final WeChatProperties weChatProperties;

    private final JwtTool jwtTool;

    private final JwtProperties jwtProperties;

    @Override
    public UserLoginVo login(UserLoginDTO userLoginDTO) {
        if (userLoginDTO == null || !StringUtils.hasText(userLoginDTO.getCode())) {
            throw new BadRequestException("微信登录 code 不能为空");
        }
        //获取微信用户openid
        String openid = getOpenid(userLoginDTO.getCode());

        //判断openid是否为空,如果为空表示登录失败,抛出业务异常
        if (!StringUtils.hasText(openid)) {
            throw new BizIllegalException(MessageConstant.LOGIN_FAILED);
        }
        //判断当前用户是否为新用户
        User user = lambdaQuery().eq(User::getOpenid, openid).one();
        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    .build();
            save(user);
        }
        //为用户生成令牌
        String token = jwtTool.userCreateToken(user.getId(), jwtProperties.getTokenTTL());

        return UserLoginVo.builder()
                .id(user.getId())
                .openid(openid)
                .token(token)
                .build();
    }

    /**
     * 调用微信接口服务,获取微信用户openid。
     * <p>
     * 本地开发没有配置 appid/secret 时，直接把 code 当作 openid 使用，
     * 方便在没有微信小程序环境的情况下联调；生产环境必须配置真实密钥。
     */
    private String getOpenid(String code) {
        String appid = weChatProperties.getAppid();
        String secret = weChatProperties.getSecret();
        if (!StringUtils.hasText(appid) || !StringUtils.hasText(secret)) {
            log.warn("未配置微信 appid/secret，本地联调模式：直接使用 code 作为 openid");
            return code;
        }

        Map<String, String> map = new HashMap<>();
        map.put("appid", appid);
        map.put("secret", secret);
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String json;
        try {
            json = HttpClientUtil.doGet(WX_LOGIN, map);
        } catch (Exception e) {
            log.error("调用微信登录接口失败", e);
            return null;
        }

        if (!StringUtils.hasText(json)) {
            return null;
        }
        JSONObject jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return null;
        }
        if (jsonObject.getInteger("errcode") != null && jsonObject.getInteger("errcode") != 0) {
            log.error("微信登录失败: errcode={}, errmsg={}",
                    jsonObject.getInteger("errcode"), jsonObject.getString("errmsg"));
            return null;
        }
        return jsonObject.getString("openid");
    }
}
