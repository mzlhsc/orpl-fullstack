package com.mzlhsc.orpl.dish.domain.dto;

import com.mzlhsc.orpl.dish.domain.po.DishFlavor;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class DishDTO implements Serializable {



    private Long id;
    //菜品名称
    private String name;
    //菜品分类id
    private Long categoryId;
    //菜品价格
    private BigDecimal price;
    //图片
    private String image;
    //描述信息
    private String description;

    //状态 1起售 0停售（不传默认起售）
    private Integer status;

    //库存，不传默认 100
    private Integer stock;

    //口味（value 支持数组，如 {"name":"甜度","value":["无糖","少糖"]}）
    private List<DishFlavor> flavors = new ArrayList<>();

}
