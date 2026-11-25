package com.mzlhsc.orpl.dish.controller.admin;

import com.mzlhsc.orpl.common.exception.BadRequestException;
import com.mzlhsc.orpl.dish.domain.dto.StockResetDTO;
import com.mzlhsc.orpl.dish.service.IDishStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/dish/stock")
@Tag(name = "菜品库存管理")
public class DishStockController {

    private final IDishStockService stockService;

    @GetMapping("/{dishId}")
    @Operation(summary = "查询菜品库存")
    @CacheEvict(cacheNames = "dishCache" ,allEntries = true)
    public Integer getStock(@PathVariable Long dishId) {
        return stockService.getStock(dishId);
    }

    @PutMapping
    @Operation(summary = "设置菜品库存")
    @CacheEvict(cacheNames = "dishCache" ,allEntries = true)
    public void reset(@RequestBody StockResetDTO dto) {
        if (dto == null || dto.getDishId() == null || dto.getStock() == null) {
            throw new BadRequestException("菜品id和库存不能为空");
        }
        stockService.resetStock(dto.getDishId(), dto.getStock());
    }
}
