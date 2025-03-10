package com.banking.monitoring.service;

import com.banking.monitoring.model.ApiMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

@Slf4j
@Service
public class AnomalyDetectionService {
    private final Map<String, DescriptiveStatistics> responseTimeStats;
    private final Map<String, DescriptiveStatistics> errorRateStats;
    private static final double Z_SCORE_THRESHOLD = 2.0; // 2 standard deviations
    private static final int WINDOW_SIZE = 100; // Rolling window size
    private static final double CPU_THRESHOLD = 80.0;
    private static final double MEMORY_THRESHOLD = 80.0;
    private static final double NETWORK_LATENCY_THRESHOLD = 200.0;

    public AnomalyDetectionService() {
        this.responseTimeStats = new ConcurrentHashMap<>();
        this.errorRateStats = new ConcurrentHashMap<>();
    }

    public Mono<ApiMetrics> detectAnomaly(ApiMetrics metrics) {
        return Mono.fromCallable(() -> {
            String endpoint = metrics.getEndpoint();
            double responseTime = metrics.getResponseTime();
            double errorRate = metrics.getErrorRate();

            DescriptiveStatistics rtStats = responseTimeStats.computeIfAbsent(endpoint,
                    k -> new DescriptiveStatistics(WINDOW_SIZE));
            DescriptiveStatistics errStats = errorRateStats.computeIfAbsent(endpoint,
                    k -> new DescriptiveStatistics(WINDOW_SIZE));

            rtStats.addValue(responseTime);
            errStats.addValue(errorRate);

            double rtZScore = calculateZScore(responseTime, rtStats);
            double errorRateZScore = calculateZScore(errorRate, errStats);

            boolean isAnomaly = false;
            List<String> reasons = new ArrayList<>();

            // Check response time anomaly
            if (Math.abs(rtZScore) > Z_SCORE_THRESHOLD) {
                isAnomaly = true;
                reasons.add(String.format("response time (%.2fms) is %.1f standard deviations from normal",
                        responseTime, Math.abs(rtZScore)));
            }

            // Check error rate anomaly
            if (Math.abs(errorRateZScore) > Z_SCORE_THRESHOLD) {
                isAnomaly = true;
                reasons.add(String.format("error rate (%.2f%%) is %.1f standard deviations from normal",
                        errorRate * 100, Math.abs(errorRateZScore)));
            }

            // Check system metrics
            if (metrics.getCpuUsage() > CPU_THRESHOLD || metrics.getMemoryUsage() > MEMORY_THRESHOLD || 
                metrics.getNetworkLatency() > NETWORK_LATENCY_THRESHOLD) {
                isAnomaly = true;
                List<String> systemReasons = new ArrayList<>();
                if (metrics.getCpuUsage() > CPU_THRESHOLD) {
                    systemReasons.add(String.format("High CPU usage (%.1f%%)", metrics.getCpuUsage()));
                }
                if (metrics.getMemoryUsage() > MEMORY_THRESHOLD) {
                    systemReasons.add(String.format("High memory usage (%.1f%%)", metrics.getMemoryUsage()));
                }
                if (metrics.getNetworkLatency() > NETWORK_LATENCY_THRESHOLD) {
                    systemReasons.add(String.format("High network latency (%.1fms)", metrics.getNetworkLatency()));
                }
                reasons.add("system metrics: " + String.join(", ", systemReasons));
            }

            double maxZScore = Math.max(Math.abs(rtZScore), Math.abs(errorRateZScore));
            double anomalyScore = isAnomaly ? 1.0 - (1.0 / (1.0 + Math.exp(maxZScore - Z_SCORE_THRESHOLD))) : 0.0;

            metrics.setAnomaly(isAnomaly);
            metrics.setAnomalyScore(anomalyScore);

            if (isAnomaly) {
                String reason = String.join(", ", reasons);
                metrics.setAnomalyReason(reason);
                log.warn("Anomaly detected for endpoint {}: {}", endpoint, reason);
            }

            return metrics;
        });
    }

    private double calculateZScore(double value, DescriptiveStatistics stats) {
        if (stats.getN() < 2) {
            // For small sample sizes, use a simple threshold-based approach
            double mean = stats.getMean();
            if (mean == 0) {
                return value > 0 ? 1.0 : 0.0;
            }
            // For small samples, use a more lenient threshold
            return Math.abs((value - mean) / mean) > 0.5 ? 2.0 : 0.0;
        }

        double mean = stats.getMean();
        double stdDev = stats.getStandardDeviation();
        
        if (Double.isNaN(stdDev) || stdDev == 0) {
            // If no variation, use a threshold-based approach
            return Math.abs((value - mean) / mean) > 0.5 ? 2.0 : 0.0;
        }
        
        return (value - mean) / stdDev;
    }

    public void updateBaselineStats(List<ApiMetrics> metrics) {
        Map<String, List<ApiMetrics>> metricsByEndpoint = new HashMap<>();
        metrics.forEach(metric -> {
            metricsByEndpoint.computeIfAbsent(metric.getEndpoint(), k -> new ArrayList<>())
                    .add(metric);
        });

        metricsByEndpoint.forEach((endpoint, endpointMetrics) -> {
            DescriptiveStatistics rtStats = responseTimeStats.computeIfAbsent(endpoint,
                    k -> new DescriptiveStatistics(WINDOW_SIZE));
            DescriptiveStatistics errStats = errorRateStats.computeIfAbsent(endpoint,
                    k -> new DescriptiveStatistics(WINDOW_SIZE));

            endpointMetrics.forEach(metric -> {
                rtStats.addValue(metric.getResponseTime());
                errStats.addValue(metric.getErrorRate());
            });

            log.info("Updated baseline stats for endpoint {}: avg response time = {:.2f}ms, error rate = {:.2%}",
                    endpoint, rtStats.getMean(), errStats.getMean());
        });
    }
} 