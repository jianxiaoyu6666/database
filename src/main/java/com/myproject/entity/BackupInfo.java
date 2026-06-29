package com.myproject.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BackupInfo {
    private String fileName;
    private Long size;
    private LocalDateTime createdAt;
}
