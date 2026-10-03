package com.example.itborrow.service.impl;
import com.example.itborrow.domain.entity.User;
import com.example.itborrow.service.NotificationService;
import com.example.itborrow.service.PersistentJobs;
import org.springframework.stereotype.Service;
@Service
public class NotificationServiceimpl implements NotificationService {
 private final PersistentJobs jobs;
 public NotificationServiceimpl(PersistentJobs jobs) { this.jobs=jobs; }
 public void send(User user,String message) { jobs.email(user.getEmail(),"LeadIT notification",message); }
}
