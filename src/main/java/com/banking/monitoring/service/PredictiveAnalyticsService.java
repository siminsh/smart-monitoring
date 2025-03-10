package com.banking.monitoring.service;

import com.banking.monitoring.model.ApiMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Instances;
import weka.core.DenseInstance;
import weka.core.Attribute;
import weka.core.FastVector;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PredictiveAnalyticsService {
    private final ConcurrentHashMap<String, MultilayerPerceptron> endpointModels;
    private final ArrayList<Attribute> attributes;
    private static final double FAILURE_THRESHOLD = 0.7;
    private static final double ERROR_RATE_THRESHOLD = 0.1;
    private static final double RESPONSE_TIME_THRESHOLD = 1000.0;
    private static final double CPU_USAGE_THRESHOLD = 80.0;
    private static final double MEMORY_USAGE_THRESHOLD = 80.0;
    private static final double NETWORK_LATENCY_THRESHOLD = 200.0;
    private final ConcurrentHashMap<String, double[]> means;
    private final ConcurrentHashMap<String, double[]> stdDevs;

    public PredictiveAnalyticsService() {
        this.endpointModels = new ConcurrentHashMap<>();
        this.attributes = createAttributes();
        this.means = new ConcurrentHashMap<>();
        this.stdDevs = new ConcurrentHashMap<>();
    }

    private ArrayList<Attribute> createAttributes() {
        ArrayList<Attribute> attrs = new ArrayList<>();
        attrs.add(new Attribute("responseTime"));
        attrs.add(new Attribute("errorRate"));
        attrs.add(new Attribute("throughput"));
        attrs.add(new Attribute("cpuUsage"));
        attrs.add(new Attribute("memoryUsage"));
        attrs.add(new Attribute("networkLatency"));
        
        ArrayList<String> classValues = new ArrayList<>();
        classValues.add("normal");
        classValues.add("failure");
        attrs.add(new Attribute("failure", classValues));
        
        return attrs;
    }

    public Mono<ApiMetrics> predictFailure(ApiMetrics metrics) {
        return Mono.fromCallable(() -> {
            MultilayerPerceptron model = endpointModels.computeIfAbsent(metrics.getEndpoint(), 
                k -> createNewModel());

            try {
                Instances dataSet = new Instances("prediction", attributes, 0);
                dataSet.setClassIndex(6);

                double[] values = new double[7];
                values[0] = metrics.getResponseTime();
                values[1] = metrics.getErrorRate();
                values[2] = metrics.getThroughput();
                values[3] = metrics.getCpuUsage();
                values[4] = metrics.getMemoryUsage();
                values[5] = metrics.getNetworkLatency();
                values[6] = 0.0;

                if (means.containsKey(metrics.getEndpoint()) && stdDevs.containsKey(metrics.getEndpoint())) {
                    double[] endpointMeans = means.get(metrics.getEndpoint());
                    double[] endpointStdDevs = stdDevs.get(metrics.getEndpoint());
                    for (int i = 0; i < 6; i++) {
                        if (endpointStdDevs[i] != 0) {
                            values[i] = (values[i] - endpointMeans[i]) / endpointStdDevs[i];
                        }
                    }
                }

                DenseInstance instance = new DenseInstance(1.0, values);
                instance.setDataset(dataSet);
                dataSet.add(instance);

                double[] distribution = model.distributionForInstance(instance);
                double failureProbability = distribution[1];

                metrics.setPredictedFailureProbability(failureProbability);
                metrics.setPredictedFailure(failureProbability > FAILURE_THRESHOLD);
                metrics.setFailureReason(generateFailureReason(metrics));

                return metrics;
            } catch (Exception e) {
                log.error("Error predicting failure: ", e);
                throw new RuntimeException("Failed to predict failure", e);
            }
        });
    }

    private MultilayerPerceptron createNewModel() {
        MultilayerPerceptron model = new MultilayerPerceptron();
        try {
            model.setHiddenLayers("3");
            model.setLearningRate(0.1);
            model.setMomentum(0.2);
            model.setTrainingTime(1000);
            model.setNormalizeAttributes(false);
            model.setValidationSetSize(0);
        } catch (Exception e) {
            log.error("Error creating neural network model: ", e);
            throw new RuntimeException("Failed to create neural network model", e);
        }
        return model;
    }

    private String generateFailureReason(ApiMetrics metrics) {
        if (!metrics.isPredictedFailure()) {
            return null;
        }

        StringBuilder reason = new StringBuilder("Potential failure predicted due to: ");
        List<String> reasons = new ArrayList<>();

        if (metrics.getErrorRate() > ERROR_RATE_THRESHOLD) {
            reasons.add("high error rate");
        }
        if (metrics.getResponseTime() > RESPONSE_TIME_THRESHOLD) {
            reasons.add("high response time");
        }
        if (metrics.getCpuUsage() > CPU_USAGE_THRESHOLD) {
            reasons.add("high CPU usage");
        }
        if (metrics.getMemoryUsage() > MEMORY_USAGE_THRESHOLD) {
            reasons.add("high memory usage");
        }
        if (metrics.getNetworkLatency() > NETWORK_LATENCY_THRESHOLD) {
            reasons.add("high network latency");
        }

        if (!reasons.isEmpty()) {
            reason.append(String.join(", ", reasons));
        }

        return reason.toString();
    }

    public void trainModel(String endpoint, List<ApiMetrics> trainingData) {
        try {
            MultilayerPerceptron model = endpointModels.computeIfAbsent(endpoint, k -> createNewModel());
            Instances trainingSet = new Instances("training_data", attributes, 0);
            trainingSet.setClassIndex(6);

            double[] sums = new double[6];
            double[] sumSquares = new double[6];
            int count = trainingData.size();

            for (ApiMetrics metrics : trainingData) {
                double[] values = new double[6];
                values[0] = metrics.getResponseTime();
                values[1] = metrics.getErrorRate();
                values[2] = metrics.getThroughput();
                values[3] = metrics.getCpuUsage();
                values[4] = metrics.getMemoryUsage();
                values[5] = metrics.getNetworkLatency();

                for (int i = 0; i < 6; i++) {
                    sums[i] += values[i];
                    sumSquares[i] += values[i] * values[i];
                }
            }

            double[] endpointMeans = new double[6];
            double[] endpointStdDevs = new double[6];
            for (int i = 0; i < 6; i++) {
                endpointMeans[i] = sums[i] / count;
                double variance = (sumSquares[i] / count) - (endpointMeans[i] * endpointMeans[i]);
                endpointStdDevs[i] = Math.sqrt(variance);
            }

            means.put(endpoint, endpointMeans);
            stdDevs.put(endpoint, endpointStdDevs);

            for (ApiMetrics metrics : trainingData) {
                double[] values = new double[7];
                values[0] = metrics.getResponseTime();
                values[1] = metrics.getErrorRate();
                values[2] = metrics.getThroughput();
                values[3] = metrics.getCpuUsage();
                values[4] = metrics.getMemoryUsage();
                values[5] = metrics.getNetworkLatency();
                
                for (int i = 0; i < 6; i++) {
                    if (endpointStdDevs[i] != 0) {
                        values[i] = (values[i] - endpointMeans[i]) / endpointStdDevs[i];
                    }
                }
                
                boolean isFailure = metrics.getErrorRate() > ERROR_RATE_THRESHOLD ||
                                  metrics.getResponseTime() > RESPONSE_TIME_THRESHOLD ||
                                  metrics.getCpuUsage() > CPU_USAGE_THRESHOLD ||
                                  metrics.getMemoryUsage() > MEMORY_USAGE_THRESHOLD ||
                                  metrics.getNetworkLatency() > NETWORK_LATENCY_THRESHOLD;
                values[6] = isFailure ? 1.0 : 0.0;

                DenseInstance instance = new DenseInstance(1.0, values);
                instance.setDataset(trainingSet);
                trainingSet.add(instance);
            }

            model.buildClassifier(trainingSet);
            endpointModels.put(endpoint, model);
        } catch (Exception e) {
            log.error("Error training model for endpoint {}: ", endpoint, e);
            throw new RuntimeException("Failed to train predictive model", e);
        }
    }
} 