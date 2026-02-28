package com.impactanalyzer.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiCallRecord {
    private String url;
    private String httpMethod;
    private long   durationMs;
    private String callerFrame;
}
