package com.hogwarts.library;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("migration")
class LiquibaseMigrationTest {

    @Test
    void contextLoads() {
    }
}