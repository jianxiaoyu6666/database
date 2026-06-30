package com.myproject.entity;

import lombok.Data;

import java.util.List;

@Data
public class BackupRequest {
    private List<String> tables;
    private Boolean includeDelete = true;
}
