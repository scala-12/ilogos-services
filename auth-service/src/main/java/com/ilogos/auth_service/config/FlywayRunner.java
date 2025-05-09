package com.ilogos.auth_service.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class FlywayRunner implements CommandLineRunner {

    private final Flyway flyway;

    public FlywayRunner(Flyway flyway) {
        this.flyway = flyway;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== Flyway migration start ===");
        System.out.println("Flyway version: " + Flyway.class.getPackage().getImplementationVersion());

        MigrateResult migrationsApplied = flyway.migrate();
        System.out.println("Applied " + migrationsApplied.migrationsExecuted + " migrations");

        System.out.println("=== Applied migrations ===");
        for (MigrationInfo info : flyway.info().applied()) {
            System.out.println(info.getVersion() + " - " + info.getDescription());
        }
    }
}
