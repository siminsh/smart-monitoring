package com.banking.monitoring.service;

import com.banking.monitoring.model.ApiMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Service
public class AnomalyDetectionService {
    private final Map<String, DescriptiveStatistics> responseTimeStats;
    private final Map<String, DescriptiveStatistics> errorRateStats;
    private static final double Z_SCORE_THRESHOLD = 3.0; // 3 standard deviations
    private static final int WINDOW_SIZE = 100; // Rolling window size

    public AnomalyDetectionService() {
        this.responseTimeStats = new ConcurrentHashMap<>();
        this.errorRateStats = new ConcurrentHashMap<>();
    }

    public Mono<ApiMetrics> detectAnomaly(ApiMetrics metrics) {
        return Mono.fromCallable(() -> {
            String endpoint = metrics.getEndpoint();
            double responseTime = metrics.getResponseTime();
            double errorRate = metrics.getErrorRate();

            // Get or create statistics for this endpoint
            DescriptiveStatistics rtStats = responseTimeStats.computeIfAbsent(endpoint,
                    k -> new DescriptiveStatistics(WINDOW_SIZE));
            DescriptiveStatistics errStats = errorRateStats.computeIfAbsent(endpoint,
                    k -> new DescriptiveStatistics(WINDOW_SIZE));

            // Update statistics
            rtStats.addValue(responseTime);
            errStats.addValue(errorRate);

            // Calculate z-scores
            double rtZScore = calculateZScore(responseTime, rtStats);
            double errorRateZScore = calculateZScore(errorRate, errStats);

            // Determine if this is an anomaly
            boolean isAnomaly = Math.abs(rtZScore) > Z_SCORE_THRESHOLD || 
                              Math.abs(errorRateZScore) > Z_SCORE_THRESHOLD;

            // Calculate anomaly score (normalized between 0 and 1)
            double maxZScore = Math.max(Math.abs(rtZScore), Math.abs(errorRateZScore));
            double anomalyScore = 1.0 - (1.0 / (1.0 + Math.exp(maxZScore - Z_SCORE_THRESHOLD)));

            metrics.setAnomaly(isAnomaly);
            metrics.setAnomalyScore(anomalyScore);

            if (isAnomaly) {
                String reason = generateAnomalyReason(metrics, rtZScore, errorRateZScore);
                metrics.setAnomalyReason(reason);
                log.warn("Anomaly detected for endpoint {}: {}", endpoint, reason);
            }

            return metrics;
        });
    }

    private double calculateZScore(double value, DescriptiveStatistics stats) {
        double mean = stats.getMean();
        double stdDev = stats.getStandardDeviation();
        
        // Handle case where there's not enough data or no variation
        if (Double.isNaN(stdDev) || stdDev == 0) {
            return 0.0;
        }
        
        return (value - mean) / stdDev;
    }

    private String generateAnomalyReason(ApiMetrics metrics, double rtZScore, double errorRateZScore) {
        List<String> reasons = new ArrayList<>();

        if (Math.abs(rtZScore) > Z_SCORE_THRESHOLD) {
            reasons.add(String.format("Response time (%.2fms) is %.1f standard deviations from normal",
                    metrics.getResponseTime(), Math.abs(rtZScore)));
        }

        if (Math.abs(errorRateZScore) > Z_SCORE_THRESHOLD) {
            reasons.add(String.format("Error rate (%.2f%%) is %.1f standard deviations from normal",
                    metrics.getErrorRate() * 100, Math.abs(errorRateZScore)));
        }

        if (metrics.getCpuUsage() > 80) {
            reasons.add(String.format("High CPU usage (%.1f%%)", metrics.getCpuUsage()));
        }

        if (metrics.getMemoryUsage() > 80) {
            reasons.add(String.format("High memory usage (%.1f%%)", metrics.getMemoryUsage()));
        }

        if (metrics.getNetworkLatency() > 200) {
            reasons.add(String.format("High network latency (%.1fms)", metrics.getNetworkLatency()));
        }

        return String.join(", ", reasons);
    }

    public void updateBaselineStats(List<ApiMetrics> metrics) {
        // Group metrics by endpoint
        Map<String, List<ApiMetrics>> metricsByEndpoint = new HashMap<>();
        metrics.forEach(metric -> {
            metricsByEndpoint.computeIfAbsent(metric.getEndpoint(), k -> new ArrayList<>())
                    .add(metric);
        });

        // Update statistics for each endpoint
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