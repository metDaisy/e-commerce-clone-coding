package io.github.metdaisy.amaazon.global.web.config;

import io.github.metdaisy.amaazon.global.web.constant.WebConstants;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.addPathPrefix(WebConstants.SERVLET_PREFIX,
            HandlerTypePredicate.forAnnotation(RestController.class));
  }
}
