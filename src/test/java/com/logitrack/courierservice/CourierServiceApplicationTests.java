package com.logitrack.courierservice;

import com.logitrack.courierservice.support.JwtTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")

@SpringBootTest
class CourierServiceApplicationTests extends JwtTestSupport {

    @Test
    void contextLoads() {
    }

}
