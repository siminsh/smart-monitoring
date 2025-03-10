package com.banking.monitoring.service;

import com.banking.monitoring.model.ApiRequest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ApiMonitoringServiceTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @Mock
    private AnomalyDetectionService anomalyDetectionService;

    @Mock
    private GenerativeAIService generativeAIService;

    @InjectMocks
    private ApiMonitoringService monitoringService;

    private ApiRequest testRequest;
    private ApiMetrics testMetrics;

    @BeforeEach
    void setUp() {
        testRequest = ApiRequest.builder()
                .endpoint("/api/test")
                .method("GET")
                .statusCode(200)
                .responseTime(100L)
                .build();

        testMetrics = ApiMetrics.builder()
                .endpoint("/api/test")
                .responseTime(100.0)
                .errorRate(0.0)
                .isAnomaly(false)
                .anomalyScore(0.0)
                .build();
    }

    @Test
    void processApiRequest_Success() {
        when(anomalyDetectionService.detectAnomaly(any())).thenReturn(Mono.just(testMetrics));
        when(generativeAIService.analyzeApiRequest(any())).thenReturn(Mono.just("Analysis result"));

        StepVerifier.create(monitoringService.processApiRequest(testRequest))
                .expectNextMatches(request -> {
                    assertNotNull(request.getId());
                    assertNotNull(request.getTimestamp());
                    assertEquals("/api/test", request.getEndpoint());
                    assertEquals(200, request.getStatusCode());
                    assertEquals(100L, request.getResponseTime());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void processApiRequest_WithAnomaly() {
        ApiMetrics anomalyMetrics = ApiMetrics.builder()
                .endpoint("/api/test")
                .responseTime(1000.0)
                .errorRate(0.5)
                .isAnomaly(true)
                .anomalyScore(0.8)
                .anomalyReason("High response time")
                .build();

        when(anomalyDetectionService.detectAnomaly(any())).thenReturn(Mono.just(anomalyMetrics));
        when(generativeAIService.analyzeApiRequest(any())).thenReturn(Mono.just("Analysis result"));

        StepVerifier.create(monitoringService.processApiRequest(testRequest))
                .expectNextMatches(request -> {
                    assertTrue(request.isAnomaly());
                    assertEquals(0.8, request.getAnomalyScore());
                    assertEquals("High response time", request.getAnomalyReason());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void processApiRequest_Error() {
        when(anomalyDetectionService.detectAnomaly(any())).thenReturn(Mono.error(new RuntimeException("Test error")));

        StepVerifier.create(monitoringService.processApiRequest(testRequest))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void convertToMetrics_ValidRequest() {
        ApiMetrics metrics = monitoringService.convertToMetrics(testRequest);
        
        assertEquals(testRequest.getId(), metrics.getId());
        assertEquals(testRequest.getEndpoint(), metrics.getEndpoint());
        assertEquals(testRequest.getResponseTime(), metrics.getResponseTime());
        assertEquals(0.0, metrics.getErrorRate());
        assertEquals(testRequest.getTimestamp(), metrics.getTimestamp());
    }

    @Test
    void convertToMetrics_ErrorRequest() {
        ApiRequest errorRequest = ApiRequest.builder()
                .endpoint("/api/test")
                .statusCode(500)
                .build();

        ApiMetrics metrics = monitoringService.convertToMetrics(errorRequest);
        
        assertEquals(1.0, metrics.getErrorRate());
    }
} 