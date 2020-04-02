package com.ruoyi.framework.security.service;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.exception.user.CaptchaException;
import com.ruoyi.common.exception.user.CaptchaExpireException;
import com.ruoyi.common.exception.user.UserPasswordNotMatchException;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.framework.redis.RedisCache;
import com.ruoyi.framework.security.LoginUser;

/**
 * 登录校验方法
 *
 * @author ruoyi
 */
@Component
public class SysLoginService {
    @Autowired
    private TokenService tokenService;

    @Resource
    private AuthenticationManager authenticationManager;

    // redis工具类
    @Autowired
    private RedisCache redisCache;

    /**
     * 登录验证核心server
     *
     * @param username 用户名
     * @param password 密码
     * @param captcha  验证码
     * @param uuid     唯一标识
     * @return 结果
     */
    public String login(String username, String password, String code, String uuid) {
        String verifyKey = Constants.CAPTCHA_CODE_KEY + uuid;
        // 根据拼接的验证码的name获取验证码 captcha_codes + uuid
        String captcha = redisCache.getCacheObject(verifyKey);
        // 删除验证码
        redisCache.deleteObject(verifyKey);
        if (captcha == null) {
            /** 如果redis里的验证码为空的话,记录日志
             * 关联对象com.ruoyi.framework.manager.AsyncManager对象、
             *
             * AsyncManager.me 使用单例创建对象,execute(execute就是ScheduledExecutorService对象。用来把定时任务和线程池结合使用)方法用来创建并执行一个一次性的操作，延迟10毫秒执行,这个操作是个TimerTask对象
             *
             *  AsyncFactory.recordLogininfor方法用来封装任务的异步工厂（产生任务用）也就是TimerTask对象，
             *  execute传入TimerTask对象(TimerTask是一个抽象类，它实现了Runnable接口，我们可以重写Runnable接口的run方法,设置线程任务),
             *  任务就是根据用户信息还有请求获取用户的系统、浏览器、登录状态等【打印】到日志，再封装到logininfor日志对象里，再使用自己封装的SpringUtils来获取记录日志的bean，把日志执行方法【插入】数据库。
             *
             *  接下来抛出异常出来，这个异常把错误信息传给验证码的异常封装成RuntimeException(运行期异常)，全局异常GlobalExceptionHandler会侦测到该controller的异常，根据异常类的
             *  第一个方法baseException把错误通过ajax封装成json ｛code: 500 msg: "验证码已失效"｝返回，需要注意的是封装返回对象的时候是根据传过去的key值，去i18n配置文件里读取的属性，配置在yml里
             *
             *  就这样500错误码和验证码已失效就传给前台了，前台根据request.js里定义的axios响应拦截器，获取到错误，展示出来！
             */
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire")));
            // 自动触发user方面的自定义异常,这个错误是 验证码已失效
            throw new CaptchaExpireException();
        }
        if (!code.equalsIgnoreCase(captcha)) {
            // 同上,这个错误是 验证码错误
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error")));
            throw new CaptchaException();
        }
        // 用户验证
        Authentication authentication = null;
        try {
            // 该方法会去调用UserDetailsServiceImpl.loadUserByUsername
            authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (Exception e) {
            if (e instanceof BadCredentialsException) {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
                throw new UserPasswordNotMatchException();
            } else {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, e.getMessage()));
                throw new CustomException(e.getMessage());
            }
        }
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success")));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        // 生成token
        return tokenService.createToken(loginUser);
    }
}
