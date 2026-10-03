package com.example.itborrow.service;
import com.example.itborrow.repository.*;
import com.example.itborrow.domain.enums.*;
import com.example.itborrow.exception.*;
import com.example.itborrow.common.event.BorrowWorkflowEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.ApplicationEventPublisher;
import java.math.BigDecimal;
import java.util.Map;
@Service
public class WorkflowOperations {
 private final BorrowRequestRepository requests;
 private final ReturnRecordRepository returns;
 private final EquipmentRepository equipment;
 private final CurrentUser current;
 private final JdbcTemplate jdbc;
 private final ApplicationEventPublisher events;
 public WorkflowOperations(BorrowRequestRepository requests, ReturnRecordRepository returns, EquipmentRepository equipment, CurrentUser current, JdbcTemplate jdbc, ApplicationEventPublisher events) {
  this.requests=requests; this.returns=returns; this.equipment=equipment; this.current=current; this.jdbc=jdbc; this.events=events;
 }
 private String required(String value) {
  if(value==null || value.isBlank() || value.length()>500) throw new IllegalArgumentException("Provide a reason/reference of at most 500 characters.");
  return value.trim();
 }
 @Transactional public void reject(Long id, String reason) {
  var request=requests.findLockedById(id).orElseThrow(() -> ResourceNotFoundException.of("Request",id));
  current.requireIndependentOperator(request);
  if(request.getStatus()!=BorrowStatus.PENDING) throw new InvalidBorrowStateException("Only pending requests can be rejected.");
  request.setRejectionReason(required(reason)); request.setStatus(BorrowStatus.CANCELLED);
  events.publishEvent(new BorrowWorkflowEvent(id,"REJECTED"));
 }
 @Transactional(readOnly=true) public Map<String,Object> settlement(Long id) {
  var request=requests.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Request",id)); current.requireOwnerOrOperator(request);
  var record=returns.findByBorrowRequestId(id).orElseThrow(() -> ResourceNotFoundException.of("Return",id));
  var payments=jdbc.queryForList("SELECT amount,reference,paid_at FROM settlements WHERE request_id=?",id);
  return Map.of("total",record.getFineAmount().add(record.getDamageAmount()),"finalized",request.getStatus()==BorrowStatus.RETURNED,"paid",!payments.isEmpty(),"payments",payments);
 }
 @Transactional public void settle(Long id, String reference, BigDecimal expectedAmount) {
  var request=requests.findLockedById(id).orElseThrow(() -> ResourceNotFoundException.of("Request",id)); current.requireIndependentOperator(request);
  if(request.getStatus()!=BorrowStatus.RETURNED) throw new InvalidBorrowStateException("Complete all returns before settlement.");
  var record=returns.findByBorrowRequestId(id).orElseThrow(); var total=record.getFineAmount().add(record.getDamageAmount());
  if(total.signum()<=0 || expectedAmount==null || total.compareTo(expectedAmount)!=0) throw new IllegalArgumentException("Verify the outstanding amount before recording payment.");
  if(jdbc.queryForObject("SELECT count(*) FROM settlements WHERE request_id=?",Long.class,id)>0) throw new InvalidBorrowStateException("Payment is already recorded.");
  jdbc.update("INSERT INTO settlements(request_id,amount,reference,actor_username) VALUES (?,?,?,?)",id,total,required(reference),current.require().getUsername());
  events.publishEvent(new BorrowWorkflowEvent(id,"PAYMENT_RECORDED"));
 }
 @Transactional public void repair(Long id, String note) {
  current.requireOperator(); var asset=equipment.findLockedById(id).orElseThrow(() -> ResourceNotFoundException.of("Equipment",id));
  if(asset.getStatus()!=EquipmentStatus.MAINTENANCE) throw new InvalidBorrowStateException("Only equipment under maintenance can be repaired.");
  jdbc.update("INSERT INTO equipment_repairs(equipment_id,actor_username,note) VALUES (?,?,?)",id,current.require().getUsername(),required(note));
  asset.setStatus(EquipmentStatus.AVAILABLE);
 }
}
