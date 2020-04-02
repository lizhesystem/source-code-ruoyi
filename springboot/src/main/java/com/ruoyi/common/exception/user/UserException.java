package com.ruoyi.common.exception.user;

import com.ruoyi.common.exception.BaseException;

/**
 * 用户信息异常类
 *
 * @author ruoyi
 */
public class UserException extends BaseException {
    private static final long serialVersionUID = 1L;

    /**
     * 继续调用父类的构造传入 模块user、code提示语
     * @param code
     * @param args
     */
    public UserException(String code, Object[] args) {
        super("user", code, args, null);
    }
}
