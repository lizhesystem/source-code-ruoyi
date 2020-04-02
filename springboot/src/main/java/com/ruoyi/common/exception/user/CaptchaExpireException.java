package com.ruoyi.common.exception.user;

/**
 * 验证码失效异常类，继承user异常类，
 *
 * @author ruoyi
 */
public class CaptchaExpireException extends UserException {
    private static final long serialVersionUID = 1L;

    public CaptchaExpireException() {
        /**
         *  构造方法调用父类userException的方法 传入 '验证码已失效'
         *  父类接受参数，并且构造函数里再调用他父类的方法，传入module、code、args、message。创建异常类
         *  userException代表用户模块，登录相关的异常都走这个类统一传给BaseException
         */
        super("user.jcaptcha.expire", null);
    }
}
