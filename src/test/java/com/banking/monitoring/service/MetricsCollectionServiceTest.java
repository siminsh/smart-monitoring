package com.banking.monitoring.service;

import com.banking.monitoring.model.ApiMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MetricsCollectionServiceTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @Mock
    private PredictiveAnalyticsService predictiveAnalyticsService;

    @InjectMocks
    private MetricsCollectionService metricsCollectionService;

    private ConcurrentHashMap<String, AtomicInteger> requestCounters;
    private ConcurrentHashMap<String, AtomicInteger> errorCounters;
    private ConcurrentHashMap<String, AtomicLong> totalResponseTime;
    private ConcurrentHashMap<String, Double> cpuUsage;
    private ConcurrentHashMap<String, Double> memoryUsage;
    private ConcurrentHashMap<String, Double> networkLatency;

    @BeforeEach
    void setUp() {
        requestCounters = new ConcurrentHashMap<>();
        errorCounters = new ConcurrentHashMap<>();
        totalResponseTime = new ConcurrentHashMap<>();
        cpuUsage = new ConcurrentHashMap<>();
        memoryUsage = new ConcurrentHashMap<>();
        networkLatency = new ConcurrentHashMap<>();

        metricsCollectionService = new MetricsCollectionService(
                predictiveAnalyticsService,
                requestCounters,
                errorCounters,
                totalResponseTime,
                cpuUsage,
                memoryUsage,
                networkLatency
        );
    }

    @Test
    void recordRequest_Success() {
        String endpoint = "/api/test";
        long responseTime = 100L;
        boolean isError = false;

        requestCounters.put(endpoint, new AtomicInteger(0));
        errorCounters.put(endpoint, new AtomicInteger(0));
        totalResponseTime.put(endpoint, new AtomicLong(0));

        metricsCollectionService.recordRequest(endpoint, responseTime, isError);

        assertEquals(1, requestCounters.get(endpoint).get());
        assertEquals(0, errorCounters.get(endpoint).get());
        assertEquals(responseTime, totalResponseTime.get(endpoint).get());
    }

    @Test
    void recordRequest_WithError() {
        String endpoint = "/api/test";
        long responseTime = 100L;
        boolean isError = true;

        requestCounters.put(endpoint, new AtomicInteger(0));
        errorCounters.put(endpoint, new AtomicInteger(0));
        totalResponseTime.put(endpoint, new AtomicLong(0));

        metricsCollectionService.recordRequest(endpoint, responseTime, isError);

        assertEquals(1, requestCounters.get(endpoint).get());
        assertEquals(1, errorCounters.get(endpoint).get());
        assertEquals(responseTime, totalResponseTime.get(endpoint).get());
    }

    @Test
    void collectMetrics_Success() {
        String endpoint = "/api/test";
        requestCounters.put(endpoint, new AtomicInteger(0));
        errorCounters.put(endpoint, new AtomicInteger(0));
        totalResponseTime.put(endpoint, new AtomicLong(0));

        metricsCollectionService.recordRequest(endpoint, 100L, false);
        metricsCollectionService.recordRequest(endpoint, 200L, true);
        metricsCollectionService.recordRequest(endpoint, 300L, false);

        cpuUsage.put(endpoint, 50.0);
        memoryUsage.put(endpoint, 60.0);
        networkLatency.put(endpoint, 100.0);

        ApiMetrics expectedMetrics = ApiMetrics.builder()
                .endpoint(endpoint)
                .responseTime(200.0)
                .requestCount(3)
                .errorCount(1)
                .errorRate(1.0 / 3.0)
                .throughput(3.0 / 60.0)
                .cpuUsage(50.0)
                .memoryUsage(60.0)
                .networkLatency(100.0)
                .build();

        when(predictiveAnalyticsService.predictFailure(any())).thenReturn(Mono.just(expectedMetrics));

        metricsCollectionService.collectMetrics();

        assertEquals(0, requestCounters.get(endpoint).get());
        assertEquals(0, errorCounters.get(endpoint).get());
        assertEquals(0, totalResponseTime.get(endpoint).get());
    }

    @Test
    void collectMetrics_NoData() {
        String endpoint = "/api/test";
        metricsCollectionService.collectMetrics();

        assertNull(requestCounters.get(endpoint));
        assertNull(errorCounters.get(endpoint));
        assertNull(totalResponseTime.get(endpoint));
    }

    @Test
    void collectMetrics_WithPredictedFailure() {
        String endpoint = "/api/test";
        requestCounters.put(endpoint, new AtomicInteger(0));
        errorCounters.put(endpoint, new AtomicInteger(0));
        totalResponseTime.put(endpoint, new AtomicLong(0));

        metricsCollectionService.recordRequest(endpoint, 100L, false);

        ApiMetrics metricsWithFailure = ApiMetrics.builder()
                .endpoint(endpoint)
                .responseTime(100.0)
                .requestCount(1)
                .errorCount(0)
                .errorRate(0.0)
                .throughput(1.0 / 60.0)
                .predictedFailure(true)
                .predictedFailureProbability(0.8)
                .failureReason("High response time")
                .build();

        when(predictiveAnalyticsService.predictFailure(any())).thenReturn(Mono.just(metricsWithFailure));

        metricsCollectionService.collectMetrics();

        assertEquals(0, requestCounters.get(endpoint).get());
        assertEquals(0, errorCounters.get(endpoint).get());
        assertEquals(0, totalResponseTime.get(endpoint).get());
    }

    @Test
    void updateSystemMetrics() {
        String endpoint = "/api/test";
        double cpu = 75.0;
        double memory = 80.0;
        double latency = 150.0;

        metricsCollectionService.updateSystemMetrics(endpoint, cpu, memory, latency);

        assertEquals(cpu, cpuUsage.get(endpoint));
        assertEquals(memory, memoryUsage.get(endpoint));
        assertEquals(latency, networkLatency.get(endpoint));
    }
} 