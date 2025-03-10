package com.banking.monitoring.service;

import com.banking.monitoring.model.ApiMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Instances;
import weka.core.DenseInstance;
import weka.core.Attribute;
import weka.core.FastVector;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PredictiveAnalyticsServiceTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @InjectMocks
    private PredictiveAnalyticsService predictiveAnalyticsService;

    private ApiMetrics normalMetrics;
    private ApiMetrics highErrorMetrics;
    private ApiMetrics highResponseTimeMetrics;
    private ApiMetrics highSystemLoadMetrics;
    private List<ApiMetrics> trainingData;

    @BeforeEach
    void setUp() {
        normalMetrics = ApiMetrics.builder()
                .endpoint("/api/test")
                .responseTime(100.0)
                .errorRate(0.01)
                .throughput(10.0)
                .cpuUsage(50.0)
                .memoryUsage(60.0)
                .networkLatency(100.0)
                .timestamp(LocalDateTime.now())
                .build();

        highErrorMetrics = ApiMetrics.builder()
                .endpoint("/api/test")
                .responseTime(100.0)
                .errorRate(0.8)
                .throughput(10.0)
                .cpuUsage(50.0)
                .memoryUsage(60.0)
                .networkLatency(100.0)
                .timestamp(LocalDateTime.now())
                .build();

        highResponseTimeMetrics = ApiMetrics.builder()
                .endpoint("/api/test")
                .responseTime(5000.0)
                .errorRate(0.01)
                .throughput(10.0)
                .cpuUsage(50.0)
                .memoryUsage(60.0)
                .networkLatency(100.0)
                .timestamp(LocalDateTime.now())
                .build();

        highSystemLoadMetrics = ApiMetrics.builder()
                .endpoint("/api/test")
                .responseTime(100.0)
                .errorRate(0.01)
                .throughput(10.0)
                .cpuUsage(90.0)
                .memoryUsage(85.0)
                .networkLatency(300.0)
                .timestamp(LocalDateTime.now())
                .build();

        trainingData = new ArrayList<>();
        trainingData.add(normalMetrics);
        trainingData.add(highErrorMetrics);
        trainingData.add(highResponseTimeMetrics);
        trainingData.add(highSystemLoadMetrics);

        predictiveAnalyticsService.trainModel("/api/test", trainingData);
    }

    @Test
    void predictFailure_NormalMetrics() {
        StepVerifier.create(predictiveAnalyticsService.predictFailure(normalMetrics))
                .expectNextMatches(metrics -> {
                    assertFalse(metrics.isPredictedFailure());
                    assertTrue(metrics.getPredictedFailureProbability() < 0.7);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void predictFailure_HighErrorRate() {
        StepVerifier.create(predictiveAnalyticsService.predictFailure(highErrorMetrics))
                .expectNextMatches(metrics -> {
                    assertTrue(metrics.isPredictedFailure());
                    assertTrue(metrics.getPredictedFailureProbability() > 0.7);
                    assertTrue(metrics.getFailureReason().contains("error rate"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void predictFailure_HighResponseTime() {
        StepVerifier.create(predictiveAnalyticsService.predictFailure(highResponseTimeMetrics))
                .expectNextMatches(metrics -> {
                    assertTrue(metrics.isPredictedFailure());
                    assertTrue(metrics.getPredictedFailureProbability() > 0.7);
                    assertTrue(metrics.getFailureReason().contains("response time"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void predictFailure_HighSystemLoad() {
        StepVerifier.create(predictiveAnalyticsService.predictFailure(highSystemLoadMetrics))
                .expectNextMatches(metrics -> {
                    assertTrue(metrics.isPredictedFailure());
                    assertTrue(metrics.getPredictedFailureProbability() > 0.7);
                    assertTrue(metrics.getFailureReason().contains("CPU usage") || 
                             metrics.getFailureReason().contains("memory usage") ||
                             metrics.getFailureReason().contains("network latency"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void predictFailure_NewEndpoint() {
        ApiMetrics newEndpointMetrics = ApiMetrics.builder()
                .endpoint("/api/new")
                .responseTime(100.0)
                .errorRate(0.01)
                .throughput(10.0)
                .cpuUsage(50.0)
                .memoryUsage(60.0)
                .networkLatency(100.0)
                .timestamp(LocalDateTime.now())
                .build();

        predictiveAnalyticsService.trainModel("/api/new", trainingData);

        StepVerifier.create(predictiveAnalyticsService.predictFailure(newEndpointMetrics))
                .expectNextMatches(metrics -> {
                    assertFalse(metrics.isPredictedFailure());
                    assertTrue(metrics.getPredictedFailureProbability() < 0.7);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void trainModel() {
        String endpoint = "/api/test";
        predictiveAnalyticsService.trainModel(endpoint, trainingData);
    }
} 