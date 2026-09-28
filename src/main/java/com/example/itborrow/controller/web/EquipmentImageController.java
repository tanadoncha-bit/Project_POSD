package com.example.itborrow.controller.web;

import com.example.itborrow.service.EquipmentImageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@RestController
public class EquipmentImageController {
    private final EquipmentImageService images;
    public EquipmentImageController(EquipmentImageService images) { this.images=images; }
    @PostMapping("/api/v1/equipment/{id}/image")
    public Map<String,String> upload(@PathVariable Long id,@RequestParam("image") MultipartFile image) {
        return Map.of("imageUrl",images.save(id,image));
    }
    @GetMapping("/images/equipment/{id}/{filename}")
    public ResponseEntity<Void> read(@PathVariable Long id,@PathVariable String filename) {
        return ResponseEntity.status(302).header("Location",images.read(id,filename))
            .header("Cache-Control","private, max-age=1800").build();
    }
}
