package com.example.itborrow.dto.response;


public class EquipmentResponseDto {
    
    // 1. ตัวแปรของ DTO
    private Long id;
    private String name;
    private String status;

    // 2. Constructor ที่รับค่าจาก Builder
    private EquipmentResponseDto(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.status = builder.status;
    }

    // 3. Getter Methods
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getStatus() { return status; }


    // 4. เอาคลาส Builder มาวางไว้ตรงนี้เลย (ด้านในคลาสหลัก)
    public static class Builder {
        private Long id;
        private String name;
        private String status;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public EquipmentResponseDto build() {
            return new EquipmentResponseDto(this);
        }
    }
}