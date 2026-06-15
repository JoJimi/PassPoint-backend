package org.example.passpoint;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Paths;
import java.time.Duration;

// TODO(3주차): @AutoConfigureMockMvc/@MockitoBean 조합 차이로 SpringBootTest 컨텍스트가
// 여러 개로 갈라지면서 컨테이너(Postgres/ES/Redis)가 컨텍스트마다 새로 기동돼 전체 테스트가 오래 걸림.
// Kafka 컨테이너를 추가할 때, 컨테이너를 static + 수동 start()로 선언하는 singleton container 패턴으로
// 리팩터링해서 컨텍스트 분리와 무관하게 컨테이너를 JVM당 1세트만 공유하도록 한다 (4주차 CI 속도용).
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
    }

    /** Nori 플러그인이 설치된 커스텀 ES 이미지(docker/elasticsearch.Dockerfile)를 빌드해서 사용 */
    @Bean
    @ServiceConnection
    ElasticsearchContainer elasticsearchContainer() {
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

    @Bean
    @ServiceConnection("redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
    }

}
