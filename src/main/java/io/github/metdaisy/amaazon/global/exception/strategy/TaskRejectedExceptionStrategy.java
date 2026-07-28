package io.github.metdaisy.amaazon.global.exception.strategy;

import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import io.github.metdaisy.amaazon.global.exception.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * TaskRejectedException 처리 전략
 */
@Slf4j
public class TaskRejectedExceptionStrategy extends AbstractExceptionResponseStrategy<TaskRejectedException> {

  @Override
  protected boolean logExceptionMessage() {
    return true;
  }

  @Override
  protected void logException(TaskRejectedException exception) {
    log.warn("배치 작업 큐 포화로 요청 거부: {}", exception.getMessage());
  }

  @Override
  protected ApiErrorResponse createErrorResponse(TaskRejectedException exception) {
    return new ApiErrorResponse("SERVICE_UNAVAILABLE",
        "현재 수집 작업이 많아 요청을 처리할 수 없습니다. 잠시 후 다시 시도해주세요.", null);
  }

  @Override
  protected HttpStatus getHttpStatus(TaskRejectedException exception) {
    return HttpStatus.SERVICE_UNAVAILABLE;
  }
}
