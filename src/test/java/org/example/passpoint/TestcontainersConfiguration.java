package org.example.passpoint;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.net.URI;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * singleton 컨테이너 패턴: 컨테이너를 static으로 선언하고 JVM당 1회 start().
 * - @MockitoBean 조합 차이로 Spring 컨텍스트가 여러 개로 갈라져도 컨테이너는 1세트만 공유.
 * - 각 @Bean 메서드는 이미 기동된 static 인스턴스를 반환하므로 재시작 없음.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    public static final String MINIO_USER = "minioadmin";
    public static final String MINIO_PASSWORD = "minioadmin";
    public static final String MINIO_BUCKET = "passpoint-audio";

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));

    private static final ElasticsearchContainer ELASTICSEARCH = createElasticsearchContainer();

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    private static final ConfluentKafkaContainer KAFKA =
            new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.0"));

    public static final GenericContainer<?> MINIO =
            new GenericContainer<>(DockerImageName.parse("minio/minio"))
                    .withEnv("MINIO_ROOT_USER", MINIO_USER)
                    .withEnv("MINIO_ROOT_PASSWORD", MINIO_PASSWORD)
                    .withCommand("server /data")
                    .withExposedPorts(9000)
                    .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000).forStatusCode(200));

    static {
        POSTGRES.start();
        ELASTICSEARCH.start();
        REDIS.start();
        KAFKA.start();
        MINIO.start();
        createTestBucket();
    }

    /** @DynamicPropertySource에서 참조해 MinIO 엔드포인트를 동적으로 주입할 때 사용 */
    public static String minioEndpoint() {
        return "http://localhost:" + MINIO.getMappedPort(9000);
    }

    // ─── @Bean 메서드: 이미 기동된 static 인스턴스를 반환 ────────────────────────

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return POSTGRES;
    }

    /** Nori 플러그인이 설치된 커스텀 ES 이미지(docker/elasticsearch.Dockerfile)를 빌드해서 사용 */
    @Bean
    @ServiceConnection
    ElasticsearchContainer elasticsearchContainer() {
        return ELASTICSEARCH;
    }

    @Bean
    @ServiceConnection("redis")
    GenericContainer<?> redisContainer() {
        return REDIS;
    }

    /** docker-compose의 confluentinc/cp-kafka와 동일한 이미지를 사용 */
    @Bean
    @ServiceConnection
    ConfluentKafkaContainer kafkaContainer() {
        return KAFKA;
    }

    // ─── private helpers ───────────────────────────────────────────────────────

    private static ElasticsearchContainer createElasticsearchContainer() {
        ImageFromDockerfile image = new ImageFromDockerfile()
                .withDockerfile(Paths.get("docker/elasticsearch.Dockerfile"));
        DockerImageName imageName = DockerImageName.parse(image.get())
                .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch");
        return new ElasticsearchContainer(imageName)
                .withEnv("xpack.security.enabled", "false")
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                .waitingFor(Wait.forHttp("/").forPort(9200).forStatusCode(200))
                .withStartupTimeout(Duration.ofMinutes(3));
    }

    /** MinIO 컨테이너가 기동된 직후 테스트용 버킷을 생성한다 */
    private static void createTestBucket() {
        try (S3Client s3 = S3Client.builder()
                .region(Region.of("us-east-1"))
                .endpointOverride(URI.create(minioEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(MINIO_USER, MINIO_PASSWORD)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build()) {
            s3.createBucket(CreateBucketRequest.builder().bucket(MINIO_BUCKET).build());
        }
    }
}
