package photomarketplace.config;

import photomarketplace.security.SessionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final SessionInterceptor sessionInterceptor;

    @Autowired
    public WebMvcConfiguration(final SessionInterceptor sessionInterceptor) {
        this.sessionInterceptor = sessionInterceptor;
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(this.sessionInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/images/**");
    }
}
