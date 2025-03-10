package com.banking.monitoring.controller;

import com.banking.monitoring.model.ApiRequest;
import com.banking.monitoring.service.ApiMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class ApiMonitoringController {
    private final ApiMonitoringService monitoringService;

    @PostMapping("/process")
    public Mono<ApiRequest> processApiRequest(@RequestBody ApiRequest request) {
        return monitoringService.processApiRequest(request);
    }

    @GetMapping("/anomalies")
    public Flux<ApiRequest> getAnomalies() {
        return monitoringService.getAnomalies();
    }

    @PostMapping("/train")
    public Mono<ResponseEntity<Void>> trainModel(@RequestBody Flux<ApiRequest> trainingData) {
        return monitoringService.trainModelWithResponse(trainingData);
    }

    @PostMapping("/recommendations")
    public Mono<String> generateRecommendations(@RequestBody Flux<ApiRequest> requests) {
        return monitoringService.generateRecommendations(requests);
    }
} 