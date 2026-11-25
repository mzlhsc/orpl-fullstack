package com.mzlhsc.orpl.setmeal.controller.user;


import com.mzlhsc.orpl.setmeal.domain.vo.DishItemVO;
import com.mzlhsc.orpl.common.result.R;
import com.mzlhsc.orpl.setmeal.constant.StatusConstant;
import com.mzlhsc.orpl.setmeal.domain.po.Setmeal;
import com.mzlhsc.orpl.setmeal.service.ISetmealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 套餐 前端控制器
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-09
 */
@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
@RequiredArgsConstructor
@Tag(name = "套餐接口" ,description = "")
@Slf4j
public class SetmealController {

    private final ISetmealService setmealService;


    /**
     * 根据套餐id查询包含的菜品
     * @param id
     * @return
     */
    @GetMapping("/dish/{id}")
    @Operation(summary = "根据套餐id查询包含的菜品")
    public R<List<DishItemVO>> getByidWithSetmea(@PathVariable Long id){
        log.info("根据套餐id查询包含的菜品: {}", id);

        List<DishItemVO> list = setmealService.getDishItemById(id);
        return R.ok(list);
    }
        /**
     * 根据分类id查询套餐
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "根据分类id查询套餐")
    public R<List<Setmeal>> list(Long categoryId) {
        if (categoryId == null) {
            throw new com.mzlhsc.orpl.common.exception.BadRequestException("分类id不能为空");
        }
        List<Setmeal> list = setmealService.lambdaQuery()
                .eq(Setmeal::getCategoryId, categoryId)
                .eq(Setmeal::getStatus, StatusConstant.ENABLE)
                .list();

        return R.ok(list);
    }









}


