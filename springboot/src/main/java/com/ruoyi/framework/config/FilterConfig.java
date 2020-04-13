package com.ruoyi.framework.config;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.DispatcherType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.ruoyi.common.filter.RepeatableFilter;
import com.ruoyi.common.filter.XssFilter;
import com.ruoyi.common.utils.StringUtils;

/**
 * Filter配置
 * Spring boot中，我们需要FilterRegistrationBean来统一管理设置过滤器，然后指定url的匹配模式，设置过滤器名称和执行顺序。
 * 这个过程和在web.xml中配置其实没什么区别，只是形式不同而已。
 * <p>
 * 这个filter配置类就是用来管理过滤器的。
 *
 * @author ruoyi
 */
@Configuration
public class FilterConfig {
    @Value("${xss.enabled}")
    private String enabled;

    @Value("${xss.excludes}")
    private String excludes;

    // 设置需要进行xss拦截的请求路径/system/*,/monitor/*,/tool/*
    @Value("${xss.urlPatterns}")
    private String urlPatterns;

    /**
     *  配置第一个过滤器,处理xss注入: 【xssFilter】
     * @return
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    // 上述注解是jse提供的注解。作用是屏蔽一些无关紧要的警告。使开发者能看到一些他们真正关心的警告。从而提高开发者的效率
    // 使用的话 是这样suppressWarnings 禁止显示警告
    @Bean
    public FilterRegistrationBean xssFilterRegistration() {
        // 新建FilterRegistrationBean来统一管理设置过滤器对象
        FilterRegistrationBean registration = new FilterRegistrationBean();
        // 设置过滤器的分类,默认就是REQUEST-该类过滤器由request动作触发，其他类型参考https://www.devsong.org/article/22
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        // 首先设置第一个过滤器,记住XssFilter需要继承Filter
        registration.setFilter(new XssFilter());
        // 设置拦截路径
        registration.addUrlPatterns(StringUtils.split(urlPatterns, ","));
        // 设置filter的名字
        registration.setName("xssFilter");
        // 设置当前filter为最高优先级，看其他资料这里可以设置数组，比如越小优先级越高。
        registration.setOrder(FilterRegistrationBean.HIGHEST_PRECEDENCE);
        Map<String, String> initParameters = new HashMap<String, String>();
        // 不需要拦截的路径和释放启用拦截器（设置这俩配置）
        initParameters.put("excludes", excludes);
        initParameters.put("enabled", enabled);
        // 设置参数返回
        registration.setInitParameters(initParameters);
        return registration;
    }

    /**
     *  配置第二个过滤器:【RepeatableFilter 】
     * @return
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Bean
    public FilterRegistrationBean someFilterRegistration() {
        // 和上面类似，创建对象设置
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new RepeatableFilter());
        registration.addUrlPatterns("/*");
        registration.setName("repeatableFilter");
        registration.setOrder(FilterRegistrationBean.LOWEST_PRECEDENCE);
        return registration;
    }

}
