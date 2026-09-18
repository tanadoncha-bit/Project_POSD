package com.example.itborrow.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping({"/", "Dashboard"})
    public String showDashboard() {
        return "dashboard/index";
    }
    
    
}
