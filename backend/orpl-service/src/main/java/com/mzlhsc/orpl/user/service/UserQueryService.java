package com.mzlhsc.orpl.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mzlhsc.orpl.user.domain.dto.DateCountDTO;
import com.mzlhsc.orpl.user.domain.po.User;
import com.mzlhsc.orpl.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户统计查询服务。
 *
 * <p>报表直接进程内调用，不经过 /inner HTTP 接口。</p>
 */
@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserMapper userMapper;

    /**
     * 按日期统计每日新增用户
     */
    public List<DateCountDTO> countUserByDate(LocalDate begin, LocalDate end) {
        return userMapper.countByDate(begin.atStartOfDay(), end.plusDays(1).atStartOfDay());
    }

    /**
     * 统计时间区间内新增用户总数
     */
    public Long countUser(LocalDate begin, LocalDate end) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .ge(User::getCreateTime, begin.atStartOfDay())
                .lt(User::getCreateTime, end.plusDays(1).atStartOfDay()));
    }
}
