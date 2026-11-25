package com.mzlhsc.orpl.dish.domain.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonDeserialize;

import com.mzlhsc.orpl.dish.json.FlavorValueDeserializer;

import java.io.Serializable;
import java.util.Collection;

/**
 * 菜品口味
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishFlavor implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    //菜品id
    private Long dishId;

    //口味名称
    private String name;

    /**
     * 口味数据，数据库中以英文逗号分隔保存。
     * <p>
     * setter 参数特意声明为 Object：同时兼容前端传字符串 "无糖,少糖"
     * 和数组 ["无糖","少糖"]，也不依赖 Jackson 2/3 的具体注解。
     */
    @JsonDeserialize(using = FlavorValueDeserializer.class)
    private String value;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDishId() {
        return dishId;
    }

    public void setDishId(Long dishId) {
        this.dishId = dishId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(Object value) {
        if (value == null) {
            this.value = null;
        } else if (value instanceof Collection<?> collection) {
            this.value = collection.stream()
                    .map(String::valueOf)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
        } else {
            this.value = String.valueOf(value);
        }
    }
}
