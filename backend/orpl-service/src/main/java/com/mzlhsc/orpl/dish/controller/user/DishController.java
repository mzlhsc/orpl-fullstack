package com.mzlhsc.orpl.dish.controller.user;

import com.mzlhsc.orpl.common.exception.BadRequestException;
import com.mzlhsc.orpl.dish.domain.vo.DishVO;
import com.mzlhsc.orpl.dish.service.IDishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜品管理（用户端）
 */
@RequestMapping("/user/dish")
@RestController("userDishController")
@RequiredArgsConstructor
@Tag(name = "菜品相关接口", description = "菜品的增删改查接口")
@Slf4j
public class DishController {

    private final IDishService dishService;

    /**
     * 根据分类id查询菜品（只返回起售中的菜品）
     */
    @GetMapping("/list")
    @Operation(summary = "根据分类id查询菜品")
    @CacheEvict(cacheNames = "dishCache" ,key = "#categoryId")
    public List<DishVO> list(Long categoryId) {
        log.info("根据分类id查询菜品: {}", categoryId);
        if (categoryId == null) {
            throw new BadRequestException("分类id不能为空");
        }
        return dishService.listWithFlavor(categoryId);
    }

}

