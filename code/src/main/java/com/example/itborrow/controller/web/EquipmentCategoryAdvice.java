package com.example.itborrow.controller.web;

import com.example.itborrow.service.CategoryService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.Map;

@ControllerAdvice(assignableTypes = { DashboardController.class, EquipmentPageController.class })
public class EquipmentCategoryAdvice {
    private final CategoryService categories;

    public EquipmentCategoryAdvice(CategoryService categories) {
        this.categories = categories;
    }

    @ModelAttribute("categoryNames")
    public Map<Long, String> categoryNames(jakarta.servlet.http.HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!(path.equals("/") || path.equals("/Dashboard") || path.equals("/equipment")
                || path.startsWith("/equipment/")))
            return Map.of();
        return categories.names();
    }
}
