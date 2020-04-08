package com.ruoyi.framework.security.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.IdUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.AddressUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.framework.redis.RedisCache;
import com.ruoyi.framework.security.LoginUser;

import eu.bitwalker.useragentutils.UserAgent;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * token验证处理
 *
 * @author ruoyi
 */
@Component
public class TokenService {
    // 令牌自定义标识,包括下面的秘钥和有效时间从配置文件读取。
    @Value("${token.header}")
    private String header;

    // 令牌秘钥
    @Value("${token.secret}")
    private String secret;

    // 令牌有效期（默认30分钟）
    @Value("${token.expireTime}")
    private int expireTime;

    protected static final long MILLIS_SECOND = 1000;

    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    private static final Long MILLIS_MINUTE_TEN = 20 * 60 * 1000L;

    @Autowired
    private RedisCache redisCache;

    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    public LoginUser getLoginUser(HttpServletRequest request) {
        // 获取请求携带的令牌
        String token = getToken(request);
        if (StringUtils.isNotEmpty(token)) {
            Claims claims = parseToken(token);
            // 解析对应的权限以及用户信息
            String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
            String userKey = getTokenKey(uuid);
            LoginUser user = redisCache.getCacheObject(userKey);
            return user;
        }
        return null;
    }

    /**
     * 设置用户身份信息
     */
    public void setLoginUser(LoginUser loginUser) {
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNotEmpty(loginUser.getToken())) {
            refreshToken(loginUser);
        }
    }

    /**
     * 删除用户身份信息
     */
    public void delLoginUser(String token) {
        if (StringUtils.isNotEmpty(token)) {
            String userKey = getTokenKey(token);
            redisCache.deleteObject(userKey);
        }
    }

    /**
     * 创建令牌（总方法）
     * 总结：该方法主要功能是封装loginUser对象，返回jwt的token。
     *      各个方法都封装成小的方法再去调用，比如获取浏览器信息,获取ip,再根据ip获取地址等。
     *      封装loginUser对象后就把对象放到redis里了，还有就是生成jwt的token
     *
     * @param loginUser 用户信息
     * @return 令牌
     */
    public String createToken(LoginUser loginUser) {
        // ID生成器工具类，获取随机UUID，使用性能更好的ThreadLocalRandom生成UUID,封装到loginUser对象里
        String token = IdUtils.fastUUID();
        loginUser.setToken(token);
        // 设置用户代理信息，下面的setUserAgent方法，里面主要获取浏览器信息，ip，地理位置等信息。封装到loginUser对象中，看方法注释
        setUserAgent(loginUser);
        // 设置登录和过期时间，从对象拿UUID,生成login_tokens开头的key,把loginUser对象存进去。设置过期时间30分钟
        refreshToken(loginUser);

        Map<String, Object> claims = new HashMap<>();
        // put数据：login_user_key:uuid，把这个map传给jwt生成工具，根据随机的字母(配置文件里)  生成jwt的token返回。。
        claims.put(Constants.LOGIN_USER_KEY, token);
        return createToken(claims);
    }

    /**
     * 验证令牌有效期，相差不足20分钟，自动刷新缓存
     *
     * @param token 令牌
     * @return 令牌
     */
    public void verifyToken(LoginUser loginUser) {
        long expireTime = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireTime - currentTime <= MILLIS_MINUTE_TEN) {
            refreshToken(loginUser);
        }
    }

    /**
     * 刷新令牌有效期
     *
     * @param loginUser 登录信息
     */
    public void refreshToken(LoginUser loginUser) {
        /**
         * 1：设置loginUser对象里的登录的时间，从服务器获取当前时间。
         * 2：设置过期时间
         * 3：从对象里拿token + 以login_tokens开头，把整个loginUser对象放到redis里，30分钟有效期。
         */
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * MILLIS_MINUTE);
        // 根据uuid将loginUser缓存,结果是login_tokens + uuid(之前生成的)
        String userKey = getTokenKey(loginUser.getToken());
        redisCache.setCacheObject(userKey, loginUser, expireTime, TimeUnit.MINUTES);
    }

    /**
     * 设置用户代理信息
     *
     * @param loginUser 登录信息
     */
    public void setUserAgent(LoginUser loginUser) {
        /**
         *
         *  1： UserAgent对象用来解析客户端操作系统、浏览器等。在pom里UserAgentUtils，UserAgent.parseUserAgentString方法用来把 获取浏览器的信息 转成UserAgent对象，使用getRequest获取请求头
         *  2：使用ipUtils工具类去根据请求头获取IP（简单获取）  com.ruoyi.common.utils.ip.IpUtils
         *  3：使用AddressUtils 根据ip获取地理位置  com.ruoyi.common.utils.ip.AddressUtils，这里面使用java代码根据IP去请求第三方去获取地理位置，里面比较复杂。！
         */
        UserAgent userAgent = UserAgent.parseUserAgentString(ServletUtils.getRequest().getHeader("User-Agent"));
        loginUser.setBrowser(userAgent.getBrowser().getName());
        loginUser.setOs(userAgent.getOperatingSystem().getName());
        String ip = IpUtils.getIpAddr(ServletUtils.getRequest());
        loginUser.setIpaddr(ip);
        loginUser.setLoginLocation(AddressUtils.getRealAddressByIP(ip));
    }

    /**
     * 从数据声明生成令牌，秘钥根据配置文件里的secret随机生成
     *
     * @param claims 数据声明
     * @return 令牌  使用Rs256签名
     */
    private String createToken(Map<String, Object> claims) {
        String token = Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, secret).compact();
        return token;
    }

    /**
     * 从令牌中获取数据声明
     *
     * @param token 令牌
     * @return 数据声明
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从令牌中获取用户名
     *
     * @param token 令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 获取请求token
     *
     * @param request
     * @return token
     */
    private String getToken(HttpServletRequest request) {
        String token = request.getHeader(header);
        // token: Bearer eyJhbGciOiJIUzUxMiJ9.eyJsb2dpbl91c2VyX2tleSI6Ijg5ZDhiOWVmLWQ4MGQtNDY0ZS1hMjJhLTVjNmE2NTczYTUzZiJ9.TpNGJiv8eR-a7i-L5lxelW4oF0D8JQy9UOm7XmRSrPTp2OVl8AX2NVcMlYBg_JXjhAytf8_k0fHmby4AfnuZEg
        // bearer是生成jwt的时候创建的
        if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX)) {
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }
        return token;
    }

    private String getTokenKey(String uuid) {
        //登录用户的redis key  login_tokens
        return Constants.LOGIN_TOKEN_KEY + uuid;
    }
}
