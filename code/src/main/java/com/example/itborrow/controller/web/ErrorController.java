package com.example.itborrow.controller.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ErrorController {

    @GetMapping("/error/401")
    public ModelAndView unauthorized() {
        ModelAndView page = new ModelAndView("error/401");
        page.setStatus(HttpStatus.UNAUTHORIZED);
        return page;
    }

    @GetMapping("/error/400")
    public ModelAndView badRequest() {
        ModelAndView page = new ModelAndView("error/400");
        page.setStatus(HttpStatus.BAD_REQUEST);
        return page;
    }
}