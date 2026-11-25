package com.mzlhsc.orpl.user.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mzlhsc.orpl.user.domain.dto.UserLoginDTO;
import com.mzlhsc.orpl.user.domain.po.User;
import com.mzlhsc.orpl.user.domain.vo.UserLoginVo;

/**
 * <p>
 * 用户信息 服务类
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-12
 */
public interface IUserService extends IService<User> {

    UserLoginVo login(UserLoginDTO userLoginDTO);
}
