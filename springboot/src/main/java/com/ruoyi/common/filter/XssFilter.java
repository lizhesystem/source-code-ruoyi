package com.ruoyi.common.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.utils.StringUtils;

/**
 * 防止XSS攻击的过滤器，由request动作触发拦截请求，拦截的参数在FilterConfig里根据配置文件获取。
 *
 * @author ruoyi
 */
public class XssFilter implements Filter {
    /**
     * 参数排除链接
     */
    public List<String> excludes = new ArrayList<>();

    /**
     * xss过滤开关
     */
    public boolean enabled = false;

    // init方法的参数，可获得代表当前filter配置信息的FilterConfig对象。
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 获取排除的请求链接以及开关的参数。
        String tempExcludes = filterConfig.getInitParameter("excludes");
        String tempEnabled = filterConfig.getInitParameter("enabled");
        // 如果不为空设置到ArrayList数组里
        if (StringUtils.isNotEmpty(tempExcludes)) {
            String[] url = tempExcludes.split(",");
            for (int i = 0; url != null && i < url.length; i++) {
                excludes.add(url[i]);
            }
        }
        // 设置释放开启的属性
        if (StringUtils.isNotEmpty(tempEnabled)) {
            enabled = Boolean.valueOf(tempEnabled);
        }
    }

    // 拦截请求
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        // 处理排除URL
        /**
         * 总结：是否开启xxs最大，如果没开启直接直接return true了。下面不执行了。
         *      如果开启了，那么就判断设置需要过滤的路径，如果没设置也直接跳过了。
                如果开启了，并且路径也设置了，就要正则匹配请求路径，如果能匹配上也就跳过。
          其余的都走false。
         */
        if (handleExcludeURL(req, resp)) {
            chain.doFilter(request, response);
            return;
        }
        // 走到这代表，请求的路径需要进行xxs的拦截过滤，处理完成继续往下走，这里只是把xxs注入的玩意给消除的方法。
        XssHttpServletRequestWrapper xssRequest = new XssHttpServletRequestWrapper((HttpServletRequest) request);
        chain.doFilter(xssRequest, response);
    }

    private boolean handleExcludeURL(HttpServletRequest request, HttpServletResponse response) {
        if (!enabled) {
            return true;
        }
        if (excludes == null || excludes.isEmpty()) {
            return false;
        }
        // 获取请求路径，正则匹配。
        String url = request.getServletPath();
        for (String pattern : excludes) {
            // 把正则表达式的规则编译成模式对象, 代表以什么开头的正则例子： ^/user/
            Pattern p = Pattern.compile("^" + pattern);
            // 通过模式对象得到匹配器对象
            Matcher m = p.matcher(url);
            // 调用匹配器对象的功能,通过find方法就是查找有没有满足条件的子串。
            if (m.find()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {

    }
}
