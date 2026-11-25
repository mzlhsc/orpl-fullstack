package com.mzlhsc.orpl.common.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 对象拷贝工具类（集合批量拷贝）
 **/
public class BeanUtils {

    /**
     * 批量把集合中的对象拷贝为指定类型
     *
     * @param source 源集合
     * @param clazz  目标类型
     * @return 目标类型集合
     */
    public static <T, R> List<T> copyList(Collection<R> source, Class<T> clazz) {
        return copyList(source, clazz, null);
    }

    /**
     * 批量把集合中的对象拷贝为指定类型，并支持自定义转换
     *
     * @param source  源集合
     * @param clazz   目标类型
     * @param convert 自定义转换逻辑
     * @return 目标类型集合
     */
    public static <T, R> List<T> copyList(Collection<R> source, Class<T> clazz, Convert<R, T> convert) {
        if (CollUtils.isEmpty(source)) {
            return new ArrayList<>();
        }
        List<T> list = new ArrayList<>(source.size());
        for (R r : source) {
            T t = newInstance(clazz);
            org.springframework.beans.BeanUtils.copyProperties(r, t);
            if (convert != null) {
                convert.convert(r, t);
            }
            list.add(t);
        }
        return list;
    }

    private static <T> T newInstance(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new com.mzlhsc.orpl.common.exception.CommonException(
                    "对象实例化失败：" + clazz.getName(), e, 500);
        }
    }
}
