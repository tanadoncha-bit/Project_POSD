package com.example.itborrow.service.jobs;
import com.example.itborrow.repository.DeliveryJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
@Service
public class DeliveryJobProcessor {
    private final DeliveryJobRepository jobs;
    private final Map<String,DeliveryJobHandler> handlers;
    public DeliveryJobProcessor(DeliveryJobRepository jobs,List<DeliveryJobHandler> handlers) {this.jobs=jobs;this.handlers=handlers.stream().collect(Collectors.toUnmodifiableMap(DeliveryJobHandler::kind,Function.identity()));}
    public boolean emailAvailable() {var handler=handlers.get("EMAIL");return handler!=null && handler.available();}
    @Transactional public void process() {
        for(var row:jobs.lockPending(emailAvailable())) {
            long id=((Number)row.get("id")).longValue();
            try {
                var handler=handlers.get((String)row.get("kind"));
                if(handler==null) throw new IllegalArgumentException("Unsupported delivery job kind");
                if(!handler.available())continue;
                handler.execute(row);jobs.complete(id);
            } catch(RuntimeException error) {jobs.retry(id,error);}
        }
    }
}
