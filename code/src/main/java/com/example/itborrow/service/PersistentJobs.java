package com.example.itborrow.service;
import com.example.itborrow.repository.DeliveryJobRepository;
import com.example.itborrow.service.jobs.DeliveryJobProcessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
/** Entry point for durable job submission; execution delegates to registered handlers. */
@Service
public class PersistentJobs {
    private final DeliveryJobRepository jobs;
    private final DeliveryJobProcessor processor;
    public PersistentJobs(DeliveryJobRepository jobs,DeliveryJobProcessor processor) {this.jobs=jobs;this.processor=processor;}
    public boolean mailConfigured() {return processor.emailAvailable();}
    @Transactional public void email(String recipient,String subject,String body) {jobs.enqueue("EMAIL",recipient,subject,body);}
    @Transactional(propagation=Propagation.REQUIRES_NEW) public void cleanup(String path) {jobs.enqueue("STORAGE_DELETE","","",path);}
    public void process() {processor.process();}
}
