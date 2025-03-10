package com.banking.monitoring.service;

import com.banking.monitoring.model.ApiMetrics;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.service.OpenAiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GenerativeAIServiceTest {

    static {
        System.setProperty("net.bytebuddy.experimental", "true");
    }

    @Mock
    private OpenAiService openAiService;

    @Mock
    private GenerativeAIService generativeAIService;

    private ApiMetrics testMetrics;
    private ChatCompletionResult mockCompletionResult;

    @BeforeEach
    void setUp() {
        testMetrics = ApiMetrics.builder()
                .endpoint("/api/test")
                .responseTime(100.0)
                .errorRate(0.01)
                .throughput(10.0)
                .cpuUsage(50.0)
                .memoryUsage(60.0)
                .networkLatency(100.0)
                .timestamp(LocalDateTime.now())
                .build();

        ChatMessage mockMessage = new ChatMessage(ChatMessageRole.ASSISTANT.value(), "Test analysis");
        ChatCompletionChoice mockChoice = new ChatCompletionChoice();
        mockChoice.setMessage(mockMessage);
        mockCompletionResult = new ChatCompletionResult();
        mockCompletionResult.setChoices(Collections.singletonList(mockChoice));
    }

    @Test
    void analyzeApiRequest_Success() {
        when(generativeAIService.analyzeApiRequest(any())).thenReturn(Mono.just("Test analysis"));

        StepVerifier.create(generativeAIService.analyzeApiRequest(testMetrics))
                .expectNext("Test analysis")
                .verifyComplete();
    }

    @Test
    void analyzeApiRequest_WithAnomaly() {
        ApiMetrics anomalyMetrics = ApiMetrics.builder()
                .endpoint("/api/test")
                .responseTime(5000.0)
                .errorRate(0.5)
                .isAnomaly(true)
                .anomalyScore(0.8)
                .anomalyReason("High response time")
                .build();

        when(generativeAIService.analyzeApiRequest(any())).thenReturn(Mono.just("Test analysis"));

        StepVerifier.create(generativeAIService.analyzeApiRequest(anomalyMetrics))
                .expectNext("Test analysis")
                .verifyComplete();
    }

    @Test
    void analyzeApiRequest_Error() {
        when(generativeAIService.analyzeApiRequest(any())).thenReturn(Mono.error(new RuntimeException("API Error")));

        StepVerifier.create(generativeAIService.analyzeApiRequest(testMetrics))
                .expectError(RuntimeException.class)
                .verify();
    }
} 