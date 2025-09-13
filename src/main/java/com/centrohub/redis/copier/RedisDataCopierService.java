package com.centrohub.redis.copier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.Map;
import java.util.List;

@Service
public class RedisDataCopierService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(RedisDataCopierService.class);

    @Autowired
    private RedisConfig redisConfig;

    @Autowired
    private ApplicationContext applicationContext;

    private RedisTemplate<String, Object> sourceRedisTemplate;
    private RedisTemplate<String, Object> destinationRedisTemplate;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting Redis Data Copier Application");

        // Handle help argument
        if (args.length > 0 && ("--help".equals(args[0]) || "-h".equals(args[0]) || "help".equals(args[0]))) {
            showHelp();
            return;
        }

        // Check for command line key argument
        String keyFromArgs = getKeyFromArgs(args);

        if (keyFromArgs != null) {
            // Single key mode from command line - first try to connect
            if (testConnections()) {
                copyKeyData(keyFromArgs);
            } else {
                // Connection failed, go to interactive mode for config
                runConnectionFailureMode();
            }
        } else {
            // Try to connect with current configuration
            if (testConnections()) {
                runInteractiveMode();
            } else {
                // Connection failed, go to interactive mode for config
                runConnectionFailureMode();
            }
        }
    }
    
    private void showHelp() {
        System.out.println("\n=== Redis Data Copier - Help ===");
        System.out.println();
        System.out.println("DESCRIPTION:");
        System.out.println("  A Spring Boot console application that copies Redis keys from a source");
        System.out.println("  database to a destination database, preserving data types and TTL.");
        System.out.println();
        System.out.println("USAGE:");
        System.out.println("  java -jar redis-data-copier-0.0.1-SNAPSHOT.jar [OPTIONS]");
        System.out.println();
        System.out.println("OPTIONS:");
        System.out.println("  --key=<key>        Copy the specified key from source to destination");
        System.out.println("  --config=<path>    Use external configuration file (overrides default config)");
        System.out.println("  --help, -h         Show this help message");
        System.out.println();
        System.out.println("EXAMPLES:");
        System.out.println("  # Copy a specific key");
        System.out.println("  java -jar redis-data-copier-0.0.1-SNAPSHOT.jar --key=user:1001");
        System.out.println();
        System.out.println("  # Use external configuration file");
        System.out.println("  java -jar redis-data-copier-0.0.1-SNAPSHOT.jar --config=/path/to/redis-config.properties --key=user:1001");
        System.out.println();
        System.out.println("  # Interactive mode with custom config");
        System.out.println("  java -jar redis-data-copier-0.0.1-SNAPSHOT.jar --config=./my-redis.properties");
        System.out.println();
        System.out.println("  # Using Maven");
        System.out.println("  mvn spring-boot:run -Dspring-boot.run.arguments=\"--config=./redis.properties --key=session:abc123\"");
        System.out.println();
        System.out.println("CONFIGURATION:");
        System.out.println("  Default: src/main/resources/application.properties");
        System.out.println("  External config file should contain these properties:");
        System.out.println();
        System.out.println("  # Source Redis Database");
        System.out.println("  redis.source.host=localhost");
        System.out.println("  redis.source.port=6379");
        System.out.println("  redis.source.database=0");
        System.out.println("  redis.source.password=");
        System.out.println("  redis.source.timeout=2000");
        System.out.println();
        System.out.println("  # Destination Redis Database");
        System.out.println("  redis.destination.host=localhost");
        System.out.println("  redis.destination.port=6380");
        System.out.println("  redis.destination.database=0");
        System.out.println("  redis.destination.password=");
        System.out.println("  redis.destination.timeout=2000");
        System.out.println();
        System.out.println("SUPPORTED DATA TYPES:");
        System.out.println("  - String: Simple key-value pairs");
        System.out.println("  - List: Ordered collections of strings");
        System.out.println("  - Set: Unordered collections of unique strings");
        System.out.println("  - ZSet: Ordered collections with scores");
        System.out.println("  - Hash: Maps between string fields and string values");
        System.out.println();
    }
    
    private String getKeyFromArgs(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--key=")) {
                return arg.substring(6);
            }
        }
        return null;
    }
    
    private void runInteractiveMode() {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.print("\nEnter the key to copy (or 'exit' to quit): ");
            String key = scanner.nextLine().trim();
            
            if ("exit".equalsIgnoreCase(key)) {
                logger.info("Application terminated by user.");
                break;
            }
            
            if (key.isEmpty()) {
                System.out.println("Please enter a valid key.");
                continue;
            }
            
            copyKeyData(key);
        }
        
        scanner.close();
    }

    private void runConnectionFailureMode() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== Redis Connection Failed ===");
            System.out.println("Unable to connect to one or more Redis databases with current configuration.");
            System.out.println("\nOptions:");
            System.out.println("1. Provide path to configuration file");
            System.out.println("2. Show help");
            System.out.println("3. Exit application");
            System.out.print("\nChoose option (1-3): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    if (handleConfigurationUpdate(scanner)) {
                        scanner.close();
                        return; // Successfully connected, exit this method
                    }
                    break;
                case "2":
                    showHelp();
                    break;
                case "3":
                    System.out.println("Application terminated by user.");
                    scanner.close();
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid option. Please choose 1, 2, or 3.");
                    break;
            }
        }
    }

    private boolean handleConfigurationUpdate(Scanner scanner) {
        System.out.print("Enter path to configuration file: ");
        String configPath = scanner.nextLine().trim();

        if (configPath.isEmpty()) {
            System.out.println("No configuration file path provided.");
            return false;
        }

        if (loadExternalConfiguration(configPath)) {
            // Clear existing templates so they get reinitialized with new config
            sourceRedisTemplate = null;
            destinationRedisTemplate = null;

            // Force refresh of Redis configuration beans
            redisConfig.refreshConfigValues();

            // Test connections with new configuration
            if (testConnections()) {
                System.out.println("✓ Successfully connected with new configuration!");
                runInteractiveMode();
                return true;
            } else {
                System.err.println("✗ Still unable to connect with the provided configuration.");
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean loadExternalConfiguration(String configPath) {
        try {
            File configFile = new File(configPath);

            if (!configFile.exists()) {
                System.err.println("Configuration file not found: " + configPath);
                return false;
            }

            if (!configFile.isFile() || !configFile.canRead()) {
                System.err.println("Cannot read configuration file: " + configPath);
                return false;
            }

            Properties externalProps = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                externalProps.load(fis);
            }

            // Update the environment with new properties
            ConfigurableEnvironment environment = (ConfigurableEnvironment) applicationContext.getEnvironment();
            PropertiesPropertySource externalPropertySource =
                new PropertiesPropertySource("externalConfig", externalProps);
            environment.getPropertySources().addFirst(externalPropertySource);

            System.out.println("✓ External configuration loaded from: " + configPath);
            return true;

        } catch (IOException e) {
            System.err.println("Error loading configuration file: " + e.getMessage());
            logger.error("Error loading external configuration", e);
            return false;
        }
    }

    private boolean testConnections() {
        return testConnections(true);
    }

    private void initializeRedisTemplates() {
        try {
            // Create Redis templates programmatically to ensure fresh configuration
            sourceRedisTemplate = createRedisTemplate(true);
            destinationRedisTemplate = createRedisTemplate(false);
        } catch (Exception e) {
            // Log at DEBUG level to avoid cluttering console during normal error scenarios
            logger.debug("Failed to initialize Redis templates due to configuration issues", e);
            // Templates will remain null, which will be handled in testConnections
            sourceRedisTemplate = null;
            destinationRedisTemplate = null;
        }
    }

    private RedisTemplate<String, Object> createRedisTemplate(boolean isSource) {
        try {
            ConfigurableEnvironment environment = (ConfigurableEnvironment) applicationContext.getEnvironment();

            String hostKey = isSource ? "redis.source.host" : "redis.destination.host";
            String portKey = isSource ? "redis.source.port" : "redis.destination.port";
            String dbKey = isSource ? "redis.source.database" : "redis.destination.database";
            String passwordKey = isSource ? "redis.source.password" : "redis.destination.password";

            String host = environment.getProperty(hostKey);
            Integer port = environment.getProperty(portKey, Integer.class);
            Integer database = environment.getProperty(dbKey, Integer.class);
            String password = environment.getProperty(passwordKey);

            if (host == null || port == null || database == null) {
                throw new IllegalArgumentException("Missing required Redis configuration properties");
            }

            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(host);
            config.setPort(port);
            config.setDatabase(database);
            if (password != null && !password.trim().isEmpty()) {
                config.setPassword(password);
            }

            JedisConnectionFactory connectionFactory = new JedisConnectionFactory(config);
            connectionFactory.afterPropertiesSet(); // Important: initialize the connection factory

            RedisTemplate<String, Object> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(new StringRedisSerializer());
            template.setHashKeySerializer(new StringRedisSerializer());
            template.setHashValueSerializer(new StringRedisSerializer());
            template.afterPropertiesSet();

            return template;
        } catch (Exception e) {
            // Log at DEBUG level to avoid cluttering console during normal error scenarios
            logger.debug("Failed to create Redis template for " + (isSource ? "source" : "destination"), e);
            throw e;
        }
    }

    private boolean testConnections(boolean showMessages) {
        if (showMessages) {
            System.out.println("Testing Redis connections...");
        }

        boolean sourceConnected = false;
        boolean destinationConnected = false;

        // Try to initialize Redis templates
        initializeRedisTemplates();

        // Test source connection
        try {
            if (sourceRedisTemplate != null) {
                sourceRedisTemplate.getConnectionFactory().getConnection().ping();
                sourceConnected = true;
                if (showMessages) {
                    System.out.println("✓ Source Redis connected: " + getSourceUrlSafely());
                }
            } else {
                throw new RuntimeException("Source Redis configuration invalid - check properties file");
            }
        } catch (Exception e) {
            if (showMessages) {
                System.err.println("✗ Failed to connect to source Redis: " + getSourceUrlSafely());
                if (sourceRedisTemplate == null) {
                    System.err.println("  Reason: Invalid configuration properties (check for missing or invalid values)");
                }
            }
            // Only log at DEBUG level to avoid cluttering the console with stack traces
            logger.debug("Source Redis connection failed", e);
        }

        // Test destination connection
        try {
            if (destinationRedisTemplate != null) {
                destinationRedisTemplate.getConnectionFactory().getConnection().ping();
                destinationConnected = true;
                if (showMessages) {
                    System.out.println("✓ Destination Redis connected: " + getDestinationUrlSafely());
                }
            } else {
                throw new RuntimeException("Destination Redis configuration invalid - check properties file");
            }
        } catch (Exception e) {
            if (showMessages) {
                System.err.println("✗ Failed to connect to destination Redis: " + getDestinationUrlSafely());
                if (destinationRedisTemplate == null) {
                    System.err.println("  Reason: Invalid configuration properties (check for missing or invalid values)");
                }
            }
            // Only log at DEBUG level to avoid cluttering the console with stack traces
            logger.debug("Destination Redis connection failed", e);
        }

        return sourceConnected && destinationConnected;
    }

    private String getSourceUrlSafely() {
        try {
            return redisConfig.getSourceUrl();
        } catch (Exception e) {
            return "invalid configuration";
        }
    }

    private String getDestinationUrlSafely() {
        try {
            return redisConfig.getDestinationUrl();
        } catch (Exception e) {
            return "invalid configuration";
        }
    }

    private void copyKeyData(String key) {
        // Ensure Redis templates are initialized
        initializeRedisTemplates();

        try {
            if (!sourceRedisTemplate.hasKey(key)) {
                System.out.println("Key '" + key + "' does not exist in source Redis database.");
                return;
            }

            String keyType = sourceRedisTemplate.type(key).code();
            logger.info("Copying key '{}' of type '{}'", key, keyType);
            System.out.println("Found key '" + key + "' of type '" + keyType + "'. Starting copy process...");

            switch (keyType) {
                case "string":
                    copyStringKey(key);
                    break;
                case "list":
                    copyListKey(key);
                    break;
                case "set":
                    copySetKey(key);
                    break;
                case "zset":
                    copyZSetKey(key);
                    break;
                case "hash":
                    copyHashKey(key);
                    break;
                default:
                    System.out.println("Unsupported key type: " + keyType);
                    return;
            }

            Long ttl = sourceRedisTemplate.getExpire(key);
            if (ttl != null && ttl > 0) {
                destinationRedisTemplate.expire(key, ttl, java.util.concurrent.TimeUnit.SECONDS);
                System.out.println("TTL copied: " + ttl + " seconds");
            }

            System.out.println("✓ Successfully copied key '" + key + "' from source to destination Redis.");
            
        } catch (Exception e) {
            System.err.println("✗ Error copying key '" + key + "': " + e.getMessage());
            logger.error("Error copying key: " + key, e);
        }
    }

    private void copyStringKey(String key) {
        Object value = sourceRedisTemplate.opsForValue().get(key);
        destinationRedisTemplate.opsForValue().set(key, value);
    }

    private void copyListKey(String key) {
        List<Object> list = sourceRedisTemplate.opsForList().range(key, 0, -1);
        if (list != null && !list.isEmpty()) {
            destinationRedisTemplate.delete(key);
            destinationRedisTemplate.opsForList().rightPushAll(key, list);
        }
    }

    private void copySetKey(String key) {
        Set<Object> set = sourceRedisTemplate.opsForSet().members(key);
        if (set != null && !set.isEmpty()) {
            destinationRedisTemplate.delete(key);
            destinationRedisTemplate.opsForSet().add(key, set.toArray());
        }
    }

    private void copyZSetKey(String key) {
        Set<ZSetOperations.TypedTuple<Object>> zset = sourceRedisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
        if (zset != null && !zset.isEmpty()) {
            destinationRedisTemplate.delete(key);
            destinationRedisTemplate.opsForZSet().add(key, zset);
        }
    }

    private void copyHashKey(String key) {
        Map<Object, Object> hash = sourceRedisTemplate.opsForHash().entries(key);
        if (hash != null && !hash.isEmpty()) {
            destinationRedisTemplate.delete(key);
            destinationRedisTemplate.opsForHash().putAll(key, hash);
        }
    }
}