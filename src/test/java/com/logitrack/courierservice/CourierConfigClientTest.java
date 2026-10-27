package com.logitrack.courierservice;

import com.logitrack.courierservice.security.JwtProperties;
import com.logitrack.courierservice.support.JwtTestSupport;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import static org.assertj.core.api.Assertions.assertThat;

class CourierConfigClientTest extends JwtTestSupport {
    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod"})
    void loadsRemoteConfigurationForEachProfile(String profile) throws Exception {
        var calls = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            byte[] body = ("{\"name\":\"courier-service\",\"profiles\":[\""+profile+"\"],"
                    +"\"propertySources\":[{\"name\":\"isolated-fixture\",\"source\":{"
                    +"\"logitrack.security.issuer\":\"https://remote-config.test\"}}]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try (var context = new SpringApplicationBuilder(CourierServiceApplication.class)
                .web(WebApplicationType.SERVLET).profiles(profile).run("--server.port=0",
                        "--CONFIG_SERVER_URL=http://127.0.0.1:"+server.getAddress().getPort(),
                        "--spring.datasource.url=jdbc:h2:mem:config_"+profile+";MODE=MySQL",
                        "--spring.datasource.driver-class-name=org.h2.Driver", "--spring.datasource.username=sa",
                        "--spring.datasource.password=", "--spring.flyway.enabled=false",
                        "--spring.jpa.hibernate.ddl-auto=create-drop",
                        "--logitrack.security.public-key-location="+PUBLIC_KEY.toUri())) {
            assertThat(calls.get()).isPositive();
            assertThat(context.getBean(JwtProperties.class).issuer()).isEqualTo("https://remote-config.test");
        } finally { server.stop(0); }
    }
}
