package com.banking.monitoring.service;

import com.banking.monitoring.model.ApiMetrics;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
@Service
public class GenerativeAIService {
    private final OpenAiService openAiService;
    private static final String MODEL = "gpt-3.5-turbo";

    public GenerativeAIService(@Value("${openai.api-key}") String apiKey) {
        this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(30));
    }

    public Mono<String> analyzeApiRequest(ApiMetrics metrics) {
        return Mono.fromCallable((Callable<String>) () -> {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(),
                    "You are an expert API monitoring analyst. Analyze the API metrics for potential issues, " +
                    "security concerns, and performance problems. Provide a detailed analysis."));
            
            String prompt = String.format("""
                    API Metrics Analysis:
                    Endpoint: %s
                    Response Time: %.2f ms
                    Error Rate: %.2f%%
                    Throughput: %.2f req/s
                    CPU Usage: %.1f%%
                    Memory Usage: %.1f%%
                    Network Latency: %.2f ms
                    
                    Please analyze these metrics and provide insights about:
                    1. Performance issues
                    2. Resource utilization
                    3. Error patterns
                    4. Recommendations for improvement
                    """,
                    metrics.getEndpoint(),
                    metrics.getResponseTime(),
                    metrics.getErrorRate() * 100,
                    metrics.getThroughput(),
                    metrics.getCpuUsage(),
                    metrics.getMemoryUsage(),
                    metrics.getNetworkLatency());

            messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model(MODEL)
                    .messages(messages)
                    .temperature(0.7)
                    .maxTokens(500)
                    .build();

            return openAiService.createChatCompletion(completionRequest)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();
        }).doOnError(error -> log.error("Error in AI analysis: ", error));
    }

    public Mono<String> generateRecommendations(List<ApiMetrics> metrics) {
        return Mono.fromCallable((Callable<String>) () -> {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(),
                    "You are an expert API optimization specialist. Analyze the patterns in the API metrics and provide " +
                    "recommendations for improving performance, security, and reliability."));

            StringBuilder prompt = new StringBuilder("API Metrics Pattern Analysis:\n\n");
            for (ApiMetrics metric : metrics) {
                prompt.append(String.format("""
                        Endpoint: %s
                        - Response Time: %.2f ms
                        - Error Rate: %.2f%%
                        - Throughput: %.2f req/s
                        - CPU Usage: %.1f%%
                        - Memory Usage: %.1f%%
                        - Network Latency: %.2f ms
                        - Anomaly Score: %.2f
                        
                        """,
                        metric.getEndpoint(),
                        metric.getResponseTime(),
                        metric.getErrorRate() * 100,
                        metric.getThroughput(),
                        metric.getCpuUsage(),
                        metric.getMemoryUsage(),
                        metric.getNetworkLatency(),
                        metric.getAnomalyScore()));
            }

            prompt.append("""
                    Please provide recommendations for:
                    1. Performance optimization
                    2. Resource utilization
                    3. Error handling
                    4. API design best practices
                    5. Scalability improvements
                    """);

            messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt.toString()));

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model(MODEL)
                    .messages(messages)
                    .temperature(0.7)
                    .maxTokens(1000)
                    .build();

            return openAiService.createChatCompletion(completionRequest)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();
        }).doOnError(error -> log.error("Error generating recommendations: ", error));
    }
} 