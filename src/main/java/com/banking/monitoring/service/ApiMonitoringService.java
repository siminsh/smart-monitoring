package com.banking.monitoring.service;

import com.banking.monitoring.model.ApiRequest;
import com.banking.monitoring.model.ApiMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiMonitoringService {
    private final AnomalyDetectionService anomalyDetectionService;
    private final GenerativeAIService generativeAIService;

    public Mono<ApiRequest> processApiRequest(ApiRequest request) {
        request.setId(UUID.randomUUID().toString());
        request.setTimestamp(LocalDateTime.now());
        
        // Convert ApiRequest to ApiMetrics
        ApiMetrics metrics = convertToMetrics(request);
        
        return anomalyDetectionService.detectAnomaly(metrics)
                .flatMap(analyzedMetrics -> 
                    generativeAIService.analyzeApiRequest(analyzedMetrics)
                        .doOnNext(analysis -> log.info("AI Analysis for request {}: {}", request.getId(), analysis))
                        .map(analysis -> {
                            updateRequest(request, analyzedMetrics);
                            return request;
                        }))
                .doOnNext(this::logAnomaly)
                .doOnError(error -> log.error("Error processing API request: ", error));
    }

    private void logAnomaly(ApiRequest request) {
        if (request.isAnomaly()) {
            log.warn("Anomaly detected for request {}: {} (Score: {})",
                    request.getId(),
                    request.getEndpoint(),
                    request.getResponseTime(),
                    request.getStatusCode(),
                    request.getAnomalyScore(),
                    request.getAnomalyReason());
        }
    }

    ApiMetrics convertToMetrics(ApiRequest request) {
        return ApiMetrics.builder()
                .id(request.getId())
                .endpoint(request.getEndpoint())
                .responseTime(request.getResponseTime())
                .errorRate(request.getStatusCode() >= 400 ? 1.0 : 0.0)
                .timestamp(request.getTimestamp())
                .build();
    }

    private ApiRequest updateRequest(ApiRequest request, ApiMetrics metrics) {
        request.setAnomaly(metrics.isAnomaly());
        request.setAnomalyScore(metrics.getAnomalyScore());
        request.setAnomalyReason(metrics.getAnomalyReason());
        return request;
    }

    public Flux<ApiRequest> getAnomalies() {
        // TODO: Implement repository to fetch anomalies
        return Flux.empty();
    }

    public Mono<Void> trainModel(Flux<ApiRequest> trainingData) {
        return trainingData
                .map(this::convertToMetrics)
                .collectList()
                .doOnNext(anomalyDetectionService::updateBaselineStats)
                .then();
    }

    public Mono<ResponseEntity<Void>> trainModelWithResponse(Flux<ApiRequest> trainingData) {
        ResponseEntity<Void> okResponse = ResponseEntity.ok().build();
        ResponseEntity<Void> errorResponse = ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        
        return trainModel(trainingData)
                .then(Mono.just(okResponse))
                .onErrorResume(e -> Mono.just(errorResponse));
    }

    public Mono<String> generateRecommendations(Flux<ApiRequest> requests) {
        return requests
                .map(this::convertToMetrics)
                .collectList()
                .flatMap(generativeAIService::generateRecommendations)
                .doOnNext(recommendations -> log.info("Generated recommendations: {}", recommendations));
    }
} 