package com.hugoboard.auth;

import java.util.List;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class AuthWebConfig implements WebMvcConfigurer {

	static {
		SpringDocUtils.getConfig().addRequestWrapperToIgnore(CurrentUser.class);
	}

	private final RoleInterceptor roleInterceptor;

	private final CurrentUserArgumentResolver currentUserArgumentResolver;

	AuthWebConfig(RoleInterceptor roleInterceptor, CurrentUserArgumentResolver currentUserArgumentResolver) {
		this.roleInterceptor = roleInterceptor;
		this.currentUserArgumentResolver = currentUserArgumentResolver;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(roleInterceptor).addPathPatterns("/api/**");
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(currentUserArgumentResolver);
	}

}
