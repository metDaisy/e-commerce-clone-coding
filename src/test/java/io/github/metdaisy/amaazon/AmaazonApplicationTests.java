package io.github.metdaisy.amaazon;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AmaazonApplicationTests {

  @Test
  void contextLoads() {
    // 컨텍스트 정상 로드 확인
  }

}
