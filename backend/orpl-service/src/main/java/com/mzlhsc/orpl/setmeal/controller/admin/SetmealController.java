package com.mzlhsc.orpl.setmeal.controller.admin;


import com.mzlhsc.orpl.common.result.PageDTO;
import com.mzlhsc.orpl.common.result.R;
import com.mzlhsc.orpl.common.exception.BadRequestException;
import com.mzlhsc.orpl.setmeal.domain.dto.SetmealDTO;
import com.mzlhsc.orpl.setmeal.domain.dto.SetmealPageQueryDTO;
import com.mzlhsc.orpl.setmeal.domain.po.Setmeal;
import com.mzlhsc.orpl.setmeal.domain.vo.SetmealVO;
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
@RestController("adminSetmealController")
@RequestMapping("/admin/setmeal")
@RequiredArgsConstructor
@Tag(name = "套餐接口" ,description = "")
@Slf4j
public class SetmealController {

    private final ISetmealService setmealService;


    /**
     * 添加套餐
     * @param setmealDTO
     * @return
     */
    @PostMapping
    @Operation(description = "添加套餐")
    public R<Void> save(@RequestBody SetmealDTO setmealDTO){
        log.info("添加套餐: {}",setmealDTO);

        //添加套餐和对应菜品
        setmealService.saveInDish(setmealDTO);

        return R.ok();
    }


    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @Operation(description ="套餐分页查询")
    public R<PageDTO<SetmealVO>> page(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("套餐分页查询:{}", setmealPageQueryDTO);
        PageDTO<SetmealVO> pageResult = setmealService.pageQuery(setmealPageQueryDTO);

        return R.ok(pageResult);
    }
    /**
     * 根据id查询套餐
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询套餐")
    public R<SetmealVO> list(@PathVariable Long id){
        log.info("根据id查询套餐: {}", id);

        SetmealVO setmealVO = setmealService.getByIdWithDishes(id);
        return R.ok(setmealVO);
    }


    /**
     * 修改菜品分类
     * @param setmealDTO
     * @return
     */
    @PutMapping
    @Operation(summary = "修改菜品分类")
    public R<Void> upate(@RequestBody SetmealDTO setmealDTO){
        log.info("修改菜品分类: {}", setmealDTO);

        //修改套餐和对应菜品
        setmealService.updateInDish(setmealDTO);

        return R.ok();
    }


    /**
     * 根据id批量删除
     * @param ids
     * @return
     */
    @DeleteMapping
    @Operation(summary = "根据id批量删除")
    public R<Void> deletes(@RequestParam List<Long> ids){
        log.info("根据id批量删除: {}", ids);

        //删除套餐和关联的菜品
        setmealService.removeNoDishByIds(ids);

        return R.ok();
    }

    /**
     * 启用、禁用分类
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @Operation(summary = "启用、禁用套餐")
    public R<Boolean> status(@PathVariable Integer status, @RequestParam Long id) {
        log.info("启用、禁用套餐: {} {}", status, id);
        if (id == null) {
            throw new BadRequestException("套餐id不能为空");
        }
        if (status == null || status != 0 && status != 1) {
            throw new BadRequestException("套餐状态不合法");
        }
        if (setmealService.getById(id) == null) {
            throw new BadRequestException("套餐不存在");
        }
        return R.ok(setmealService.updateById(Setmeal.builder().id(id).status(status).build()));
    }


}

