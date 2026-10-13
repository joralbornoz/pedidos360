package cl.duoc.pedidos360.rabbitmq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration," +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "RABBITMQ_PASSWORD=test",
        "SUPABASE_DB_PASSWORD=test",
        "CATALOG_SERVICE_URL=http://localhost:8082"
})
class MsPedidos360RabbitmqApplicationTests {

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring arranca sin errores de configuración
    }
}
