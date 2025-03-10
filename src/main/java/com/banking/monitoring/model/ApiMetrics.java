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
public class ApiMetrics {
    private String id;
    private String endpoint;
    private double responseTime;
    private int requestCount;
    private int errorCount;
    private double errorRate;
    private double throughput;
    private double cpuUsage;
    private double memoryUsage;
    private double networkLatency;
    private LocalDateTime timestamp;
    private double predictedFailureProbability;
    private boolean predictedFailure;
    private String failureReason;
    private boolean isAnomaly;
    private double anomalyScore;
    private String anomalyReason;
}