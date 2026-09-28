package com.example.itborrow.controller.api;
import com.example.itborrow.service.ManagementRequestQuery;
import org.springframework.web.bind.annotation.*;
@RestController
public class ManagementRequestController {
 private final ManagementRequestQuery query;
 public ManagementRequestController(ManagementRequestQuery query){this.query=query;}
 @GetMapping("/api/v1/borrow-requests/management")
 public java.util.Map<String,Object> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="ALL") String status){return query.load(page,status);}
}
