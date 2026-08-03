package io.github.metdaisy.amaazon.global.exception.strategy;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import io.github.metdaisy.amaazon.global.exception.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 예외 유형별 전략을 관리하는 팩토리
 */
@Slf4j
@Component
public class ExceptionStrategyFactory {

  private final Map<Class<?>, ExceptionResponseStrategy<?>> strategyMap = new HashMap<>();

  public ExceptionStrategyFactory() {
    initializeStrategies();
  }

  private void initializeStrategies() {
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
    provider.addIncludeFilter(new AssignableTypeFilter(ExceptionResponseStrategy.class));

    String basePackage = this.getClass().getPackage().getName();
    provider.findCandidateComponents(basePackage).forEach(this::registerStrategy);
  }

  private void registerStrategy(BeanDefinition component) {
    try {
      Class<?> clazz = Class.forName(component.getBeanClassName());
      
      if (Modifier.isAbstract(clazz.getModifiers()) || clazz.isInterface()) {
        return;
      }

      ExceptionResponseStrategy<?> strategy = (ExceptionResponseStrategy<?>) clazz.getDeclaredConstructor().newInstance();
      Class<?>[] typeArgs = GenericTypeResolver.resolveTypeArguments(clazz, ExceptionResponseStrategy.class);
      
      if (typeArgs != null && typeArgs.length > 0) {
        Class<?> exceptionClass = typeArgs[0];
        strategyMap.put(exceptionClass, strategy);
        log.debug("Registered Strategy: {} for Exception: {}", clazz.getSimpleName(), exceptionClass.getSimpleName());
      }
    } catch (Exception e) {
      log.error("Failed to initialize ExceptionResponseStrategy: {}", component.getBeanClassName(), e);
      throw new IllegalStateException("전략 클래스 초기화 실패", e);
    }
  }

  @SuppressWarnings("unchecked")
  public <T extends Exception> ExceptionResponseStrategy<T> getStrategy(Class<? extends Exception> exceptionClass) {
    Class<?> currentClass = exceptionClass;
    while (currentClass != null && currentClass != Throwable.class) {
      if (strategyMap.containsKey(currentClass)) {
        return (ExceptionResponseStrategy<T>) strategyMap.get(currentClass);
      }
      currentClass = currentClass.getSuperclass();
    }
    return (ExceptionResponseStrategy<T>) strategyMap.get(Exception.class);
  }

  /**
   * HttpServletRequest를 사용하여 NoResourceFoundException 응답을 생성합니다.
   */
  public ResponseEntity<ApiErrorResponse> buildNoResourceFoundResponse(
      NoResourceFoundException exception,
      HttpServletRequest request) {
    return ((NoResourceFoundExceptionStrategy) strategyMap.get(NoResourceFoundException.class))
        .buildResponse(exception, request);
  }
}
