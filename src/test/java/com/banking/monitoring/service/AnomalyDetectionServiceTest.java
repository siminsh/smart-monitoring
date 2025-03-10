package com.banking.monitoring.service;

import com.banking.monitoring.model.ApiMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @InjectMocks
    private AnomalyDetectionService anomalyDetectionService;

    private ApiMetrics normalMetrics;
    private ApiMetrics highResponseTimeMetrics;
    private ApiMetrics highErrorRateMetrics;

    @BeforeEach
    void setUp() {
        normalMetrics = ApiMetrics.builder()
                .endpoint("/api/test")
                .responseTime(100.0)
                .errorRate(0.01)
                .timestamp(LocalDateTime.now())
                .build();

        highResponseTimeMetrics = ApiMetrics.builder()
                .endpoint("/api/test")
                .responseTime(5000.0)
                .errorRate(0.01)
                .timestamp(LocalDateTime.now())
                .build();

        highErrorRateMetrics = ApiMetrics.builder()
                .endpoint("/api/test")
                .responseTime(100.0)
                .errorRate(0.5)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    void detectAnomaly_NormalMetrics() {
        StepVerifier.create(anomalyDetectionService.detectAnomaly(normalMetrics))
                .expectNextMatches(metrics -> {
                    assertFalse(metrics.isAnomaly());
                    assertTrue(metrics.getAnomalyScore() < 0.5);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void detectAnomaly_HighResponseTime() {
        // Add more baseline data
        for (int i = 0; i < 10; i++) {
            anomalyDetectionService.detectAnomaly(normalMetrics).block();
        }

        StepVerifier.create(anomalyDetectionService.detectAnomaly(highResponseTimeMetrics))
                .expectNextMatches(metrics -> {
                    assertTrue(metrics.isAnomaly());
                    assertTrue(metrics.getAnomalyScore() > 0.5);
                    assertTrue(metrics.getAnomalyReason().contains("response time"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void detectAnomaly_HighErrorRate() {
        // Add more baseline data
        for (int i = 0; i < 10; i++) {
            anomalyDetectionService.detectAnomaly(normalMetrics).block();
        }

        StepVerifier.create(anomalyDetectionService.detectAnomaly(highErrorRateMetrics))
                .expectNextMatches(metrics -> {
                    assertTrue(metrics.isAnomaly());
                    assertTrue(metrics.getAnomalyScore() > 0.5);
                    assertTrue(metrics.getAnomalyReason().contains("error rate"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void detectAnomaly_NewEndpoint() {
        ApiMetrics newEndpointMetrics = ApiMetrics.builder()
                .endpoint("/api/new")
                .responseTime(100.0)
                .errorRate(0.01)
                .timestamp(LocalDateTime.now())
                .build();

        StepVerifier.create(anomalyDetectionService.detectAnomaly(newEndpointMetrics))
                .expectNextMatches(metrics -> {
                    assertFalse(metrics.isAnomaly());
                    assertTrue(metrics.getAnomalyScore() < 0.5);
                    return true;
                })
                .verifyComplete();
    }
//
//    @Test
//    void detectAnomaly_WithSystemMetrics() {
//        ApiMetrics systemMetrics = ApiMetrics.builder()
//                .endpoint("/api/test")
//                .responseTime(100.0)
//                .errorRate(0.01)
//                .cpuUsage(90.0)
//                .memoryUsage(85.0)
//                .networkLatency(300.0)
//                .timestamp(LocalDateTime.now())
//                .build();
//
//        StepVerifier.create(anomalyDetectionService.detectAnomaly(systemMetrics))
//                .expectNextMatches(metrics -> {
//                    assertTrue(metrics.isAnomaly());
//                    assertTrue(metrics.getAnomalyScore() > 0.5);
//                    assertTrue(metrics.getAnomalyReason().contains("system metrics"));
//                    return true;
//                })
//                .verifyComplete();
//    }
} 