package com.centrohub.redis.copier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@SpringBootApplication
public class RedisDataCopierApplication {

    public static void main(String[] args) {
        // Check for config parameter before starting Spring application
        String configPath = getConfigPathFromArgs(args);
        
        SpringApplication app = new SpringApplication(RedisDataCopierApplication.class);
        
        if (configPath != null) {
            // Load external configuration
            app.addInitializers(applicationContext -> {
                ConfigurableEnvironment environment = applicationContext.getEnvironment();
                loadExternalConfig(configPath, environment);
            });
        }
        
        app.run(args);
    }
    
    private static String getConfigPathFromArgs(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--config=")) {
                return arg.substring(9);
            }
        }
        return null;
    }
    
    private static void loadExternalConfig(String configPath, ConfigurableEnvironment environment) {
        try {
            File configFile = new File(configPath);
            
            if (!configFile.exists()) {
                System.err.println("Configuration file not found: " + configPath);
                System.exit(1);
                return;
            }
            
            if (!configFile.isFile() || !configFile.canRead()) {
                System.err.println("Cannot read configuration file: " + configPath);
                System.exit(1);
                return;
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
            
        } catch (IOException e) {
            System.err.println("Error loading configuration file: " + e.getMessage());
            System.exit(1);
        }
    }
}