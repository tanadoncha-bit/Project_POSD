package com.example.itborrow.common.event;
import com.example.itborrow.repository.BorrowRequestRepository;
import com.example.itborrow.service.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
@Component
public class WorkflowNotification {
 private final BorrowRequestRepository requests; private final NotificationService notifications;
 public WorkflowNotification(BorrowRequestRepository requests,NotificationService notifications) { this.requests=requests;this.notifications=notifications; }
 @TransactionalEventListener(phase=TransactionPhase.BEFORE_COMMIT) public void notify(BorrowWorkflowEvent event) {
  if(event.action().equals("OVERDUE")) return;
  requests.findById(event.requestId()).ifPresent(r -> notifications.send(r.getUser(),"Request #"+r.getId()+": "+event.action().replace('_',' ')+(r.getRejectionReason()==null ? "" : ". Reason: "+r.getRejectionReason())));
 }
}
