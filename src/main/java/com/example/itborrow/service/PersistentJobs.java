package com.example.itborrow.service;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import com.example.itborrow.service.avatar.ImageStorage;
@Service
public class PersistentJobs {
 private final JdbcTemplate jdbc; private final ObjectProvider<JavaMailSender> mail; private final ImageStorage storage;
 private final String from,host;
 public PersistentJobs(JdbcTemplate jdbc,ObjectProvider<JavaMailSender> mail,@org.springframework.beans.factory.annotation.Qualifier("equipmentStorage") ImageStorage storage,@Value("${app.mail.from:}") String from,@Value("${spring.mail.host:}") String host) { this.jdbc=jdbc;this.mail=mail;this.storage=storage;this.from=from;this.host=host; }
 public boolean mailConfigured() { return !from.isBlank() && !host.isBlank() && mail.getIfAvailable()!=null; }
 @Transactional public void email(String recipient,String subject,String body) { jdbc.update("INSERT INTO delivery_jobs(kind,recipient,subject,payload) VALUES ('EMAIL',?,?,?)",recipient,subject,body); }
 @Transactional(propagation=Propagation.REQUIRES_NEW) public void cleanup(String path) { jdbc.update("INSERT INTO delivery_jobs(kind,recipient,subject,payload) VALUES ('STORAGE_DELETE','','',?)",path); }
 @Transactional public void process() {
  var rows=jdbc.queryForList("SELECT * FROM delivery_jobs WHERE completed=false AND attempts<10 AND next_attempt<=CURRENT_TIMESTAMP AND (kind<>'EMAIL' OR ?=true) ORDER BY id LIMIT 5 FOR UPDATE SKIP LOCKED",mailConfigured());
  for(var row:rows) {
   long id=((Number)row.get("id")).longValue(); String kind=(String)row.get("kind");
   if(kind.equals("EMAIL") && !mailConfigured()) continue;
   try {
    if(kind.equals("EMAIL")) { var message=new SimpleMailMessage(); message.setFrom(from); message.setTo((String)row.get("recipient")); message.setSubject((String)row.get("subject")); message.setText((String)row.get("payload")); mail.getObject().send(message); }
    else { String path=(String)row.get("payload"); if(jdbc.queryForObject("SELECT count(*) FROM borrow_items WHERE snapshot_image_url=?",Long.class,"/images/equipment/"+path)==0 && jdbc.queryForObject("SELECT count(*) FROM equipment WHERE image_url=?",Long.class,"/images/equipment/"+path)==0) storage.delete(path); }
    jdbc.update("UPDATE delivery_jobs SET completed=true,payload='',last_error=NULL WHERE id=?",id);
   } catch(RuntimeException error) { jdbc.update("UPDATE delivery_jobs SET attempts=attempts+1,next_attempt=?,last_error=? WHERE id=?",java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(60)),error.getClass().getSimpleName(),id); }
  }
 }
}
