package com.ridebooking.payment;

import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationTest {

    private static final String MIGRATION_LOCATION = "db/migration/V1__initial_schema.sql";

    @Test
    void migrationFileShouldExist() {
        InputStream is = getClass().getClassLoader().getResourceAsStream(MIGRATION_LOCATION);
        assertThat(is)
                .as("V1 migration file should exist at %s", MIGRATION_LOCATION)
                .isNotNull();
    }

    @Test
    void migrationFileShouldContainMultipleTables() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(MIGRATION_LOCATION);
        assertThat(is).isNotNull();
        String sql;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            sql = reader.lines().collect(Collectors.joining("\n"));
        }
        long tableCount = sql.toUpperCase().chars()
                .filter(c -> c == 'T')
                .count();
        assertThat(sql.toUpperCase())
                .as("Migration should contain payments table")
                .contains("PAYMENTS");
        assertThat(sql.toUpperCase())
                .as("Migration should contain wallets table")
                .contains("WALLETS");
        assertThat(sql.toUpperCase())
                .as("Migration should contain invoices table")
                .contains("INVOICES");
    }

    @Test
    void migrationFileShouldHaveIndexes() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(MIGRATION_LOCATION);
        assertThat(is).isNotNull();
        String sql;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            sql = reader.lines().collect(Collectors.joining("\n"));
        }
        assertThat(sql.toUpperCase())
                .as("Migration should create indexes")
                .contains("CREATE INDEX");
    }

    @Test
    void flywayConfigurationShouldLoad() {
        ClassicConfiguration config = new ClassicConfiguration();
        config.setLocations(new Location("classpath:db/migration"));
        assertThat(config.getLocations()).hasSize(1);
        assertThat(config.getLocations()[0].getPath()).isEqualTo("db/migration");
    }
}
