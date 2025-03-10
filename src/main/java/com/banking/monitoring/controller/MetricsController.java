package com.banking.monitoring.controller;

import com.banking.monitoring.model.ApiMetrics;
import com.banking.monitoring.service.MetricsCollectionService;
import com.banking.monitoring.service.PredictiveAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {
    private final MetricsCollectionService metricsCollectionService;
    private final PredictiveAnalyticsService predictiveAnalyticsService;

    @PostMapping("/record")
    public Mono<ResponseEntity<Void>> recordRequest(
            @RequestParam String endpoint,
            @RequestParam long responseTime,
            @RequestParam boolean isError) {
        return Mono.fromRunnable(() -> 
            metricsCollectionService.recordRequest(endpoint, responseTime, isError))
            .thenReturn(ResponseEntity.<Void>ok().build());
    }

    @PostMapping("/system")
    public Mono<ResponseEntity<Void>> updateSystemMetrics(
            @RequestParam String endpoint,
            @RequestParam double cpu,
            @RequestParam double memory,
            @RequestParam double latency) {
        return Mono.fromRunnable(() -> 
            metricsCollectionService.updateSystemMetrics(endpoint, cpu, memory, latency))
            .thenReturn(ResponseEntity.<Void>ok().build());
    }

    @PostMapping("/predict")
    public Mono<ResponseEntity<ApiMetrics>> predictFailure(@RequestBody ApiMetrics metrics) {
        return predictiveAnalyticsService.predictFailure(metrics)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }

    @PostMapping("/train/{endpoint}")
    public Mono<ResponseEntity<Void>> trainModel(
            @PathVariable String endpoint,
            @RequestBody Flux<ApiMetrics> trainingData) {
        ResponseEntity<Void> okResponse = ResponseEntity.<Void>ok().build();
        ResponseEntity<Void> errorResponse = ResponseEntity.<Void>internalServerError().build();
        
        return trainingData
                .collectList()
                .doOnNext(data -> predictiveAnalyticsService.trainModel(endpoint, data))
                .thenReturn(okResponse)
                .onErrorResume(e -> Mono.just(errorResponse));
    }
} 