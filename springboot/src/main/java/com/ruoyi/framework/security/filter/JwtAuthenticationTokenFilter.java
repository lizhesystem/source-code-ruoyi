package com.ruoyi.framework.security.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.security.LoginUser;
import com.ruoyi.framework.security.service.TokenService;

/**
 * token过滤器 验证token有效性
 * 如果想让一个Filter在一次请求处理的过程中只被应用一次，就让这个Filter继承自OncePerRequestFilter。
 *
 * （比如某个JSP页面中使用 include包含了另外一个JSP片段(比如页面统一的头部或者底部区域),在这样的JSP页面被请求时，因为这些include指令，该页面请求的的处理中，
 * 同一个Filter也会被应用多次，也就是它的 doFilter()方法会被调用多次。）
 *
 * @author ruoyi
 * @Component注解:用于标注数据访问层，也可以说用于标注数据访问组件，即DAO组件，就是说当我们的类不属于各种归类的时候（不属于@Controller、@Services等的时候），我们就可以使用@Component来标注这个类
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    @Autowired
    private TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 根据token，获取loginUser对象，在jwt里解密再从redis里拿
        LoginUser loginUser = tokenService.getLoginUser(request);
        // StringUtils.isNull(SecurityUtils.getAuthentication()) 为空代表当前不是登录，登录没必要走这个filter。
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNull(SecurityUtils.getAuthentication())) {
            // 验证令牌有效期，相差不足20分钟，自动刷新缓存,设置开始和过期时间重新放到redis里
            tokenService.verifyToken(loginUser);
            // 将验证信息放入SecurityContextHolder中，UsernamePasswordAuthenticationToken是Security验证账号密码的工具类
            // 每次获取用户,或者验证token都会走这个filter，来设置登录上下文
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        }
        chain.doFilter(request, response);
    }
}
