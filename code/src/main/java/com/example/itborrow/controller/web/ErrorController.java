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

    @GetMapping("/error/403")
    public ModelAndView forbidden() {
        ModelAndView page = new ModelAndView("error/403");
        page.setStatus(HttpStatus.FORBIDDEN);
        return page;
    }

    @GetMapping("/error/404")
    public ModelAndView notFound() {
        ModelAndView page = new ModelAndView("error/404");
        page.setStatus(HttpStatus.NOT_FOUND);
        return page;
    }

    @GetMapping("/error/500")
    public ModelAndView internalServerError() {

        ModelAndView page = new ModelAndView("error/500");

        page.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);

        return page;
    }
}