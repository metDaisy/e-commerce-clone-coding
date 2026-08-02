package io.github.metdaisy.amaazon.global.config;

import java.util.Map;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

public class MdcTaskDecorator implements TaskDecorator {

  @Override
  public Runnable decorate(Runnable runnable) {
    Map<String, String> mdcContextMap = MDC.getCopyOfContextMap();

    return () -> {
      try {
        MDC.setContextMap(mdcContextMap);
        runnable.run();
      } finally {
        MDC.clear();
      }
    };
  }
}
