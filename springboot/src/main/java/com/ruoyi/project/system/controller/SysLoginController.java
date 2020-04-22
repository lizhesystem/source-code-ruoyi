package com.ruoyi.project.system.controller;

import java.util.List;
import java.util.Set;

import com.ruoyi.project.system.domain.vo.RouterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.framework.security.LoginUser;
import com.ruoyi.framework.security.service.SysLoginService;
import com.ruoyi.framework.security.service.SysPermissionService;
import com.ruoyi.framework.security.service.TokenService;
import com.ruoyi.framework.web.domain.AjaxResult;
import com.ruoyi.project.system.domain.SysMenu;
import com.ruoyi.project.system.domain.SysUser;
import com.ruoyi.project.system.service.ISysMenuService;

/**
 * 登录验证, 1:登录，2：获取信息 3：获取路由
 *
 * @author ruoyi
 */
// 在Spring4里用新注解@RestController可以看做是 @Controller 和@ResponseBody两个注解的组合。
@RestController
public class SysLoginController {
    @Autowired
    private SysLoginService loginService;

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private TokenService tokenService;

    /**
     * 登录方法
     *
     * @param username 用户名
     * @param password 密码
     * @param captcha  验证码
     * @param uuid     唯一标识
     * @return 结果
     */
    @PostMapping("/login")
    public AjaxResult login(String username, String password, String code, String uuid) {
        // 封装的返回结果类，成功失败的等等都涵盖在内。
        AjaxResult ajax = AjaxResult.success();
        // 生成令牌，跳转到com.ruoyi.framework.security.service.SysLoginService里,专门用来处理登录
        String token = loginService.login(username, password, code, uuid);
        // token返回,登录的接口完成，登录成功后接下来前端跳转路由会拉取用户信息。走下面的getInfo
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    /**
     * 获取用户信息
     * 获取信息之前会先走2个filter,一个是读取inputStream的request的，一个是security的JwtAuthenticationTokenFilter。
     * 后者主要是用来判断token的，如果登录成功后有token，带着这个token请求该接口的时候走这个filter进行验证。
     *
     * @return 用户信息
     */
    @GetMapping("getInfo")
    public AjaxResult getInfo() {
        /**
         * ServletUtils.getRequest()得到一个HttpServletRequest对象,再根据token获取LoginUser对象。
         * filter会拦截获取一下。重新刷新令牌失效时间并且把登录用户对象放到security的上下文中
         *
         * 这里再根据token从redis里获取一次。用来获取用户权限和角色集合。
         */
        LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        SysUser user = loginUser.getUser();
        // 该用户角色集合 common
        Set<String> roles = permissionService.getRolePermission(user);
        // 该用户所属角色的权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);
        return ajax;
    }

    /**
     * 获取路由信息
     *
     * @return 生成路由信息
     */
    @GetMapping("getRouters")
    public AjaxResult getRouters() {
        // 依然先通过filter获取信息后，这里再获取一次。
        LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        // 用户信息
        SysUser user = loginUser.getUser();
        // 根据用户所属的角色获取菜单,然后再生产前端需要的格式的路由，RouterVo路由对象
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(user.getUserId());
        List<RouterVo> routerVos = menuService.buildMenus(menus);
        return AjaxResult.success(routerVos);
    }
}
