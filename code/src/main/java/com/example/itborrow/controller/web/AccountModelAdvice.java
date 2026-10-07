package com.example.itborrow.controller.web;

import com.example.itborrow.service.AccountViewQuery;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import java.security.Principal;

@ControllerAdvice(assignableTypes = { DashboardController.class, EquipmentPageController.class, AccountController.class,
        UserManagementController.class })
public class AccountModelAdvice {
    private final AccountViewQuery accounts;

    public AccountModelAdvice(AccountViewQuery accounts) {
        this.accounts = accounts;
    }

    @ModelAttribute
    public void account(Principal principal, Model model, jakarta.servlet.http.HttpServletRequest request) {
        if (principal != null)
            model.addAllAttributes(accounts.account(principal.getName(),
                    (request.getRequestURI().equals(request.getContextPath() + "/profile")
                            || request.getRequestURI().equals(request.getContextPath() + "/profile/setup-login"))));
    }
}
