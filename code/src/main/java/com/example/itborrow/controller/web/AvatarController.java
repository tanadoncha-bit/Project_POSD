package com.example.itborrow.controller.web;

import com.example.itborrow.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.*;

@Controller
public class AvatarController {
    private final AvatarService avatars;
    private final CurrentUser current;

    public AvatarController(AvatarService avatars, CurrentUser current) {
        this.avatars = avatars;
        this.current = current;
    }

    @PostMapping("/profile/avatar")
    public String upload(@RequestParam("image") MultipartFile image, RedirectAttributes flash) {
        try {
            avatars.save(current.require().getId(), image);
            flash.addFlashAttribute("accountMessage", "Profile picture updated.");
        } catch (IllegalArgumentException | com.example.itborrow.service.avatar.StorageException ex) {
            flash.addFlashAttribute("accountError", ex.getMessage());
        }
        return "redirect:/profile";
    }

    @GetMapping("/profile/avatar")
    @ResponseBody
    public ResponseEntity<?> image() {
        return imageFor(current.require().getId());
    }

    @GetMapping("/admin/borrower-avatar/{id}")
    @ResponseBody
    public ResponseEntity<?> borrowerImage(@PathVariable Long id) {
        return imageFor(id);
    }

    private ResponseEntity<?> imageFor(Long id) {
        String url = avatars.storedUrl(id);
        if (url != null)
            return ResponseEntity.status(302).cacheControl(CacheControl.noStore()).location(java.net.URI.create(url))
                    .build();
        return ResponseEntity.notFound().build();
    }
}
