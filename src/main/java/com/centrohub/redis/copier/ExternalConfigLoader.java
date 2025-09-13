package com.centrohub.redis.copier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Component
public class ExternalConfigLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalConfigLoader.class);
    
    public boolean loadExternalConfig(String configPath, ConfigurableEnvironment environment) {
        try {
            File configFile = new File(configPath);
            
            if (!configFile.exists()) {
                System.err.println("Configuration file not found: " + configPath);
                logger.error("Configuration file not found: {}", configPath);
                return false;
            }
            
            if (!configFile.isFile() || !configFile.canRead()) {
                System.err.println("Cannot read configuration file: " + configPath);
                logger.error("Cannot read configuration file: {}", configPath);
                return false;
            }
            
            Properties externalProps = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                externalProps.load(fis);
            }
            
            // Add external properties with high priority
            PropertiesPropertySource externalPropertySource = 
                new PropertiesPropertySource("externalConfig", externalProps);
            environment.getPropertySources().addFirst(externalPropertySource);
            
            System.out.println("✓ External configuration loaded from: " + configPath);
            logger.info("External configuration loaded from: {}", configPath);
            
            return true;
            
        } catch (IOException e) {
            System.err.println("Error loading configuration file: " + e.getMessage());
            logger.error("Error loading configuration file: {}", configPath, e);
            return false;
        }
    }
}