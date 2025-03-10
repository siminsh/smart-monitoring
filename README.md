# API Monitoring System

A sophisticated API monitoring system for banking applications that leverages machine learning and AI to predict potential failures and anomalies in real-time.

## Features

- **Real-time API Monitoring**: Track response times, error rates, and throughput
- **Predictive Analytics**: Use machine learning to predict potential failures
- **Anomaly Detection**: Identify unusual patterns in API behavior
- **AI-Powered Analysis**: Leverage OpenAI's GPT models for intelligent analysis
- **Reactive Architecture**: Built with Spring WebFlux for high performance
- **R2DBC Support**: Asynchronous database operations with PostgreSQL

## Prerequisites

- Java 21 or higher
- Maven 3.6 or higher
- PostgreSQL 12 or higher
- OpenAI API key (for AI analysis features)

## Getting Started

1. Clone the repository:
```bash
git clone https://github.com/yourusername/api-monitoring.git
cd api-monitoring
```

2. Configure the application:
   - Copy `src/main/resources/application.yml.example` to `src/main/resources/application.yml`
   - Update the configuration with your database and OpenAI credentials

3. Build the project:
```bash
mvn clean install
```

4. Run the application:
```bash
mvn spring-boot:run
```

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/banking/monitoring/
│   │       ├── config/         # Configuration classes
│   │       ├── controller/     # REST controllers
│   │       ├── model/          # Data models
│   │       ├── repository/     # Data repositories
│   │       └── service/        # Business logic
│   └── resources/
│       └── application.yml     # Application configuration
└── test/                      # Test classes
```

## Key Components

### PredictiveAnalyticsService
- Uses Weka's MultilayerPerceptron for failure prediction
- Monitors multiple metrics:
  - Response time
  - Error rate
  - Throughput
  - CPU usage
  - Memory usage
  - Network latency

### AnomalyDetectionService
- Detects unusual patterns in API behavior
- Uses statistical analysis to identify anomalies

### GenerativeAIService
- Leverages OpenAI's GPT models
- Provides intelligent analysis of API behavior
- Generates human-readable insights

### MetricsCollectionService
- Collects and aggregates API metrics
- Performs periodic analysis
- Updates system metrics in real-time

## API Endpoints

- `POST /api/metrics`: Submit API metrics
- `GET /api/metrics/{endpoint}`: Get metrics for an endpoint
- `GET /api/predictions/{endpoint}`: Get failure predictions
- `GET /api/analysis/{endpoint}`: Get AI analysis

## Configuration

Key configuration properties in `application.yml`:

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/monitoring
    username: your_username
    password: your_password

openai:
  api-key: your_openai_api_key

monitoring:
  failure-threshold: 0.7
  collection-interval: 60000  # milliseconds
```

## Testing

Run the test suite:
```bash
mvn test
```

The test suite includes:
- Unit tests for all services
- Integration tests for API endpoints
- Performance tests for the monitoring system

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Spring Boot team for the excellent framework
- Weka team for the machine learning library
- OpenAI for the GPT models
- The open-source community for various dependencies 