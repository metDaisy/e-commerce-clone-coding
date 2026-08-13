package io.github.metdaisy.amaazon;

import io.github.metdaisy.amaazon.support.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("애플리케이션 통합 테스트")
class AmaazonApplicationTests extends BaseIntegrationTest {

  @Test
  @DisplayName("애플리케이션 컨텍스트: 테스트 프로필과 PostgreSQL 스키마로 기동한다")
  void contextLoads() {
    // 컨텍스트 정상 로드 확인
  }

}
