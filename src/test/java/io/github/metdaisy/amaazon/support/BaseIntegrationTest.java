package io.github.metdaisy.amaazon.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.metdaisy.amaazon.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class BaseIntegrationTest {

  @Autowired
  protected EntityManager em;

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected ObjectMapper objectMapper;

  protected MockHttpServletRequestBuilder postJson(String url, Object body) throws Exception {
    return post(url)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body));
  }

  protected void flushAndClear() {
    em.flush();
    clear();
  }

  protected void clear() {
    em.clear();
  }

  protected <T> T persistAndFlush(T entity) {
    em.persist(entity);
    em.flush();
    return entity;
  }

  protected boolean compareInstant(Instant actual, Instant expected) {
    if (actual == null && expected == null) {
      return true;
    }
    if (actual == null || expected == null) {
      return false;
    }
    return truncateToMillis(actual).equals(truncateToMillis(expected));
  }

  private Instant truncateToMillis(Instant instant) {
    return instant.truncatedTo(ChronoUnit.MILLIS);
  }
}
