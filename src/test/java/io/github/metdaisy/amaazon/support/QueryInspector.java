package io.github.metdaisy.amaazon.support;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryInspector implements StatementInspector {

  private static final Logger log = LoggerFactory.getLogger(QueryInspector.class);

  private final ThreadLocal<Integer> count = ThreadLocal.withInitial(() -> 0);
  private final ThreadLocal<List<String>> queries = ThreadLocal.withInitial(ArrayList::new);

  @Override
  public String inspect(String sql) {
    count.set(count.get() + 1);
    queries.get().add(sql);
    return sql;
  }

  public int getCount() {
    return count.get();
  }

  public void clear() {
    count.remove();
    queries.remove();
  }

  public List<String> getQueries() {
    return queries.get();
  }

  public void logQueries() {
    List<String> collectedQueries = queries.get();
    if (collectedQueries.isEmpty()) {
      log.info("실행된 쿼리가 없습니다.");
      return;
    }

    log.info("========== 총 실행 쿼리 수: {} ==========", count.get());
    for (int index = 0; index < collectedQueries.size(); index++) {
      String formattedSql = FormatStyle.BASIC.getFormatter().format(collectedQueries.get(index));
      log.info("Query [{}]: {}", index + 1, formattedSql);
    }
    log.info("=========================================");
  }
}
