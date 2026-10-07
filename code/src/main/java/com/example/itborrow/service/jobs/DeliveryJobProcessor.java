package com.example.itborrow.service.jobs;

import com.example.itborrow.repository.DeliveryJobRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryJobProcessor implements DeliveryJobExecutor {
    private static final Logger log = LoggerFactory.getLogger(DeliveryJobProcessor.class);
    private final DeliveryJobRepository jobs;
    private final Map<String, DeliveryJobHandler> handlers;
    public DeliveryJobProcessor(DeliveryJobRepository jobs, List<DeliveryJobHandler> handlers) {
        this.jobs = jobs;
        this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(DeliveryJobHandler::kind, Function.identity()));
    }
    public boolean emailAvailable() {
        var handler = handlers.get("EMAIL");
        return handler != null && handler.available();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void process() {
        for (int count = 0; count < 5; count++) {
            var row = jobs.claim(emailAvailable());
            if (row.isEmpty()) break;
            long id = ((Number) row.get("id")).longValue();
            String lease = (String) row.get("lease_token");
            try {
                var handler = handlers.get((String) row.get("kind"));
                if (handler == null || !handler.available()) throw new IllegalStateException("Delivery handler unavailable");
                handler.execute(row); // No database transaction is held during network I/O.
                jobs.complete(id, lease);
            } catch (RuntimeException error) {
                int attempt = ((Number) row.get("attempts")).intValue();
                jobs.retry(id, lease, attempt, error);
                log.warn("Delivery job {} attempt {} failed ({}){}", id, attempt,
                        error.getClass().getSimpleName(), attempt >= 10 ? "; manual review required" : "");
            }
        }
    }
}
