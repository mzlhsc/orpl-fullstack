package com.mzlhsc.orpl.common.utils;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.mzlhsc.orpl.common.context.UserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 添修操作
 * 获取时间,用户
 * 统一工具类
 */

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "createUser", Long.class, UserContext.getCurrentId());
        this.strictInsertFill(metaObject, "updateUser", Long.class, UserContext.getCurrentId());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateUser", Long.class, UserContext.getCurrentId());
    }
}
