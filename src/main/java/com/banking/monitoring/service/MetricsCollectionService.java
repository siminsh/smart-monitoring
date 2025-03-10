package com.banking.monitoring.service;

import com.banking.monitoring.model.ApiMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsCollectionService {
    private final PredictiveAnalyticsService predictiveAnalyticsService;
    private final ConcurrentHashMap<String, AtomicInteger> requestCounters;
    private final ConcurrentHashMap<String, AtomicInteger> errorCounters;
    private final ConcurrentHashMap<String, AtomicLong> totalResponseTime;
    private final ConcurrentHashMap<String, Double> cpuUsage;
    private final ConcurrentHashMap<String, Double> memoryUsage;
    private final ConcurrentHashMap<String, Double> networkLatency;

    public void recordRequest(String endpoint, long responseTime, boolean isError) {
        requestCounters.computeIfAbsent(endpoint, k -> new AtomicInteger(0)).incrementAndGet();
        if (isError) {
            errorCounters.computeIfAbsent(endpoint, k -> new AtomicInteger(0)).incrementAndGet();
        }
        totalResponseTime.computeIfAbsent(endpoint, k -> new AtomicLong(0)).addAndGet(responseTime);
    }

    @Scheduled(fixedRate = 60000) // Collect metrics every minute
    public void collectMetrics() {
        Flux.fromIterable(requestCounters.keySet())
                .flatMap(this::generateMetrics)
                .flatMap(predictiveAnalyticsService::predictFailure)
                .doOnNext(this::logMetrics)
                .subscribe();
    }

    private Mono<ApiMetrics> generateMetrics(String endpoint) {
        return Mono.fromCallable(() -> {
            int requests = requestCounters.getOrDefault(endpoint, new AtomicInteger(0)).get();
            int errors = errorCounters.getOrDefault(endpoint, new AtomicInteger(0)).get();
            long totalTime = totalResponseTime.getOrDefault(endpoint, new AtomicLong(0)).get();
            
            double avgResponseTime = requests > 0 ? (double) totalTime / requests : 0;
            double errorRate = requests > 0 ? (double) errors / requests : 0;
            double throughput = requests / 60.0; // requests per second

            // Reset counters
            requestCounters.getOrDefault(endpoint, new AtomicInteger(0)).set(0);
            errorCounters.getOrDefault(endpoint, new AtomicInteger(0)).set(0);
            totalResponseTime.getOrDefault(endpoint, new AtomicLong(0)).set(0);

            return ApiMetrics.builder()
                    .id(UUID.randomUUID().toString())
                    .endpoint(endpoint)
                    .responseTime(avgResponseTime)
                    .requestCount(requests)
                    .errorCount(errors)
                    .errorRate(errorRate)
                    .throughput(throughput)
                    .cpuUsage(cpuUsage.getOrDefault(endpoint, 0.0))
                    .memoryUsage(memoryUsage.getOrDefault(endpoint, 0.0))
                    .networkLatency(networkLatency.getOrDefault(endpoint, 0.0))
                    .timestamp(LocalDateTime.now())
                    .build();
        });
    }

    private void logMetrics(ApiMetrics metrics) {
        if (metrics.isPredictedFailure()) {
            log.warn("Failure predicted for endpoint {}: {} (Probability: {})",
                    metrics.getEndpoint(),
                    metrics.getFailureReason(),
                    metrics.getPredictedFailureProbability());
        }
    }

    public void updateSystemMetrics(String endpoint, double cpu, double memory, double latency) {
        cpuUsage.put(endpoint, cpu);
        memoryUsage.put(endpoint, memory);
        networkLatency.put(endpoint, latency);
        log.debug("Updated system metrics for endpoint {}: CPU={}%, Memory={}%, Latency={}ms",
                endpoint, cpu, memory, latency);
    }
} 