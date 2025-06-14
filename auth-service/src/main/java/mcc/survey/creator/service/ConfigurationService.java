package mcc.survey.creator.service;

import mcc.survey.creator.entity.SystemConfig;
import mcc.survey.creator.repository.SystemConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

    private final SystemConfigRepository systemConfigRepository;
    private final EncryptionService encryptionService;

    @Autowired
    public ConfigurationService(SystemConfigRepository systemConfigRepository, EncryptionService encryptionService) {
        this.systemConfigRepository = systemConfigRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional(readOnly = true)
    public Optional<String> getConfigValue(String key) {
        Optional<SystemConfig> configOpt = systemConfigRepository.findByConfigKey(key);
        if (configOpt.isEmpty()) {
            logger.warn("Configuration key '{}' not found.", key);
            return Optional.empty();
        }

        SystemConfig config = configOpt.get();
        if (config.isEncrypted()) {
            try {
                return Optional.of(encryptionService.decrypt(config.getConfigValue()));
            } catch (EncryptionService.EncryptionOperationException e) {
                logger.error("Failed to decrypt configuration value for key '{}'. Error: {}", key, e.getMessage());
                // Depending on policy, you might want to throw an exception or return empty/default
                return Optional.empty();
            }
        } else {
            return Optional.of(config.getConfigValue());
        }
    }

    public String getConfigValueOrDefault(String key, String defaultValue) {
        return getConfigValue(key).orElse(defaultValue);
    }

    @Transactional
    public SystemConfig saveConfig(String key, String value, boolean isEncrypted) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Configuration key cannot be null or empty.");
        }
        if (value == null) {
            // Or handle as an explicit instruction to store null, though typically not done for configs.
            throw new IllegalArgumentException("Configuration value cannot be null. To remove a key, consider a delete operation.");
        }

        String valueToStore = value;
        if (isEncrypted) {
            try {
                valueToStore = encryptionService.encrypt(value);
            } catch (EncryptionService.EncryptionOperationException e) {
                logger.error("Failed to encrypt configuration value for key '{}'. Error: {}", key, e.getMessage());
                throw new RuntimeException("Failed to encrypt configuration for key: " + key, e);
            }
        }

        Optional<SystemConfig> existingConfigOpt = systemConfigRepository.findByConfigKey(key);
        SystemConfig configToSave;
        if (existingConfigOpt.isPresent()) {
            configToSave = existingConfigOpt.get();
            configToSave.setConfigValue(valueToStore);
            configToSave.setEncrypted(isEncrypted);
            logger.info("Updating configuration for key '{}'. Encrypted: {}", key, isEncrypted);
        } else {
            configToSave = new SystemConfig(null, key, valueToStore, isEncrypted);
            logger.info("Saving new configuration for key '{}'. Encrypted: {}", key, isEncrypted);
        }
        return systemConfigRepository.save(configToSave);
    }

    // Optional: method to get a config value and throw an exception if not found
    public String getRequiredConfigValue(String key) {
        return getConfigValue(key)
            .orElseThrow(() -> {
                logger.error("Required configuration key '{}' not found.", key);
                return new IllegalStateException("Required configuration key '" + key + "' not found.");
            });
    }
}
