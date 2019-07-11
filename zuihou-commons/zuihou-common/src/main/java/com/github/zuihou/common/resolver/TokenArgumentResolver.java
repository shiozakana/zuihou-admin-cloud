package com.github.zuihou.common.resolver;

import com.github.zuihou.base.R;
import com.github.zuihou.common.annotation.LoginUser;
import com.github.zuihou.common.feign.UserQuery;
import com.github.zuihou.common.feign.UserResolveApi;
import com.github.zuihou.common.model.SysUser;
import com.github.zuihou.context.BaseContextHandler;
import com.github.zuihou.utils.NumberHelper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Token转化SysUser
 *
 * @author zuihou
 * @date 2018/12/21
 */
@Slf4j
public class TokenArgumentResolver implements HandlerMethodArgumentResolver {

    private UserResolveApi userResolveApi;

    public TokenArgumentResolver(UserResolveApi userResolveApi) {
        this.userResolveApi = userResolveApi;
    }

    /**
     * 入参筛选
     *
     * @param mp 参数集合
     * @return 格式化后的参数
     */
    @Override
    public boolean supportsParameter(MethodParameter mp) {
        return mp.hasParameterAnnotation(LoginUser.class) && mp.getParameterType().equals(SysUser.class);
    }

    /**
     * @param methodParameter       入参集合
     * @param modelAndViewContainer model 和 view
     * @param nativeWebRequest      web相关
     * @param webDataBinderFactory  入参解析
     * @return 包装对象
     */
    @Override
    public Object resolveArgument(MethodParameter methodParameter,
                                  ModelAndViewContainer modelAndViewContainer,
                                  NativeWebRequest nativeWebRequest,
                                  WebDataBinderFactory webDataBinderFactory) {
        Long userId = BaseContextHandler.getUserId();
        String account = BaseContextHandler.getAccount();
        String name = BaseContextHandler.getName();
        Long orgId = BaseContextHandler.getOrgId();
        Long stationId = BaseContextHandler.getStationId();

        //以下代码为 根据 @LoginUser 注解来注入 SysUser 对象
        SysUser user = SysUser.builder()
                .id(userId)
                .account(account)
                .name(name)
                .orgId(orgId)
                .stationId(stationId)
                .build();

        LoginUser loginUser = methodParameter.getParameterAnnotation(LoginUser.class);
        boolean isFull = loginUser.isFull();

        if (isFull || loginUser.isStation() || loginUser.isOrg() || loginUser.isRoles()) {
            R<SysUser> result = userResolveApi.getById(NumberHelper.longValueOf0(userId),
                    UserQuery.builder()
                            .full(isFull)
                            .org(loginUser.isOrg())
                            .station(loginUser.isStation())
                            .roles(loginUser.isRoles())
                            .build());
            if (result.getIsSuccess() && result.getData() != null) {
                return result.getData();
            }
        }

        return user;
    }
}