package com.banking.monitoring.model;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ApiRequest {
    private String id;
    private String endpoint;
    private String method;
    private String requestBody;
    private String responseBody;
    private int statusCode;
    private long responseTime;
    private LocalDateTime timestamp;
    private boolean isAnomaly;
    private double anomalyScore;
    private String anomalyReason;
    private String errorMessage;
} 