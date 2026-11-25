package com.mzlhsc.orpl.user.controller.user;

import com.mzlhsc.orpl.common.constant.MessageConstant;
import com.mzlhsc.orpl.common.context.UserContext;
import com.mzlhsc.orpl.common.exception.BadRequestException;
import com.mzlhsc.orpl.common.exception.ForbiddenException;
import com.mzlhsc.orpl.common.exception.NotFoundException;
import com.mzlhsc.orpl.common.result.R;
import com.mzlhsc.orpl.user.domain.po.AddressBook;
import com.mzlhsc.orpl.user.service.IAddressBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 地址簿 前端控制器
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-13
 */
@RestController
@RequestMapping("/addressBook")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "地址簿接口", description = "地址簿的增删改查接口")
public class AddressBookController {

    private final IAddressBookService addressBookService;

    /**
     * 查询当前登录用户的所有地址信息
     */
    @GetMapping("/list")
    @Operation(summary = "查询当前登录用户的所有地址信息")
    public R<List<AddressBook>> list() {
        Long userId = requireLogin();
        log.info("查询当前登录用户的所有地址信息: {}", userId);
        List<AddressBook> list = addressBookService.lambdaQuery()
                .eq(AddressBook::getUserId, userId)
                .list();
        return R.ok(list);
    }

    /**
     * 新增地址
     */
    @PostMapping
    @Operation(summary = "新增地址")
    public R<Void> save(@RequestBody AddressBook addressBook) {
        log.info("新增地址: {}", addressBook);
        Long userId = requireLogin();
        if (addressBook == null || !StringUtils.hasText(addressBook.getConsignee())
                || !StringUtils.hasText(addressBook.getPhone())
                || !StringUtils.hasText(addressBook.getDetail())) {
            throw new BadRequestException("收货人、手机号和详细地址不能为空");
        }
        // 地址归属只能是当前登录用户，防止越权写别人的地址
        addressBook.setId(null);
        addressBook.setUserId(userId);
        // 新增时不直接设为默认，默认地址只能通过 /addressBook/default 设置，避免出现多个默认
        addressBook.setIsDefault(false);
        addressBookService.save(addressBook);
        return R.ok();
    }

    /**
     * 根据id查询地址
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询地址")
    public R<AddressBook> getById(@PathVariable Long id) {
        log.info("根据id查询地址: {}", id);
        return R.ok(getOwnedAddress(id));
    }

    /**
     * 根据id修改地址
     */
    @PutMapping
    @Operation(summary = "根据id修改地址")
    public R<Void> update(@RequestBody AddressBook addressBook) {
        log.info("根据id修改地址: {}", addressBook);
        if (addressBook == null || addressBook.getId() == null) {
            throw new BadRequestException("地址id不能为空");
        }
        AddressBook owned = getOwnedAddress(addressBook.getId());
        // 禁止修改地址归属、创建信息；默认地址只能走 /addressBook/default，避免出现多个默认
        addressBook.setUserId(owned.getUserId());
        addressBook.setIsDefault(null);
        addressBookService.updateById(addressBook);
        return R.ok();
    }

    /**
     * 设置默认地址
     */
    @PutMapping("/default")
    @Operation(summary = "设置默认地址")
    public R<Void> setDefault(@RequestBody AddressBook addressBook) {
        if (addressBook == null || addressBook.getId() == null) {
            throw new BadRequestException("地址id不能为空");
        }
        log.info("设置默认地址: {}", addressBook.getId());
        Long userId = requireLogin();
        getOwnedAddress(addressBook.getId());

        // 先把当前用户的所有地址置为非默认，再把目标地址置为默认，顺序不能反
        addressBookService.lambdaUpdate()
                .set(AddressBook::getIsDefault, false)
                .eq(AddressBook::getUserId, userId)
                .update();
        addressBookService.lambdaUpdate()
                .set(AddressBook::getIsDefault, true)
                .eq(AddressBook::getId, addressBook.getId())
                .update();
        return R.ok();
    }

    /**
     * 根据id删除地址
     */
    @DeleteMapping
    @Operation(summary = "根据id删除地址")
    public R<Void> deleteById(@RequestParam Long id) {
        log.info("根据id删除地址: {}", id);
        getOwnedAddress(id);
        addressBookService.removeById(id);
        return R.ok();
    }

    /**
     * 查询默认地址
     */
    @GetMapping("/default")
    @Operation(summary = "查询默认地址")
    public R<AddressBook> getDefault() {
        Long userId = requireLogin();
        List<AddressBook> list = addressBookService.lambdaQuery()
                .eq(AddressBook::getUserId, userId)
                .eq(AddressBook::getIsDefault, true)
                .list();

        if (!list.isEmpty()) {
            return R.ok(list.get(0));
        }
        return R.error("没有查询到默认地址");
    }

    private Long requireLogin() {
        Long userId = UserContext.getCurrentId();
        if (userId == null) {
            throw new BadRequestException(MessageConstant.USER_NOT_LOGIN);
        }
        return userId;
    }

    private AddressBook getOwnedAddress(Long id) {
        if (id == null) {
            throw new BadRequestException("地址id不能为空");
        }
        AddressBook addressBook = addressBookService.getById(id);
        if (addressBook == null) {
            throw new NotFoundException("地址不存在");
        }
        Long userId = UserContext.getCurrentId();
        if (userId == null || !Objects.equals(addressBook.getUserId(), userId)) {
            throw new ForbiddenException("无权操作该地址");
        }
        return addressBook;
    }
}
