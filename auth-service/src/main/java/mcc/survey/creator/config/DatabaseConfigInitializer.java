package mcc.survey.creator.config;

import mcc.survey.creator.service.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;

@Component
public class DatabaseConfigInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfigInitializer.class);

    private final ConfigurationService configurationService;
    private final Environment environment;

    // Temporarily inject the old JWT secret to migrate it.
    // This allows us to read it once for the migration.
    // We'll use a placeholder if not found, but it should be there.
    @Value("${app.jwt.secret:ThisIsADefaultSecretKeyForMigrationIfNotFoundInProps}")
    private String jwtSecretToMigrate;

    @Autowired
    public DatabaseConfigInitializer(ConfigurationService configurationService, Environment environment) {
        this.configurationService = configurationService;
        this.environment = environment;
    }

    @Override
    public void run(String... args) throws Exception {
        String configKey = "app.jwt.secret";

        // Check if running in a test profile or if a test property source is active
        // This is to prevent overwriting test configurations if they are handled differently
        if (environment.getProperty("spring.test.context. ενεργός", "false").equals("true") ||
            environment.getPropertySources().contains("test")) {
             logger.info("Skipping JWT secret initialization for test profile.");
             return;
        }


        if (configurationService.getConfigValue(configKey).isEmpty()) {
            if (jwtSecretToMigrate != null && !jwtSecretToMigrate.isEmpty() && !"ThisIsADefaultSecretKeyForMigrationIfNotFoundInProps".equals(jwtSecretToMigrate)) {
                logger.info("Initializing JWT secret in the database from properties value...");
                configurationService.saveConfig(configKey, jwtSecretToMigrate, true);
                logger.info("JWT secret initialized and stored encrypted in the database.");
            } else {
                // Fallback if the property was somehow already removed or not present
                // This is a safeguard; ideally, jwtSecretToMigrate should be found.
                String defaultSecretForDB = "your-super-secret-key-which-should-be-long-and-complex-and-at-least-64-characters-long-for-HS512-DB-DEFAULT";
                logger.warn("Property 'app.jwt.secret' not found for migration. Initializing with a default placeholder in DB.");
                configurationService.saveConfig(configKey, defaultSecretForDB, true);
                logger.info("Default JWT secret initialized and stored encrypted in the database. Ensure this is updated if it was unexpected.");
            }
        } else {
            logger.info("JWT secret (configKey: '{}') already exists in the database. No action taken.", configKey);
        }
    }
}
