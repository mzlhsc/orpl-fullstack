package com.mzlhsc.orpl.user.controller.user;


import com.mzlhsc.orpl.common.result.R;
import com.mzlhsc.orpl.user.domain.dto.UserLoginDTO;
import com.mzlhsc.orpl.user.domain.vo.UserLoginVo;
import com.mzlhsc.orpl.user.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 用户信息 前端控制器
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-12
 */
@RestController
@RequestMapping("/user/user")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "用户接口",description = "登录,退出")
public class UserController {

    private final IUserService userService;


    /**
     * 登录
     * @param userLoginDTO
     * @return
     */
    @PostMapping("/login")
    @Operation(operationId = "登录")
    public R<UserLoginVo> login(@RequestBody UserLoginDTO userLoginDTO){
        log.info("登录: {}",userLoginDTO);
        return R.ok(userService.login(userLoginDTO));
    }

    /**
     * 退出
     *
     * @return
     */
    @GetMapping("/logout")
    @Operation(operationId = "退出")
    public R logout(){
        return R.ok();
    }




}
