package com.centrohub.redis.copier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Autowired
    private Environment environment;

    @Value("${redis.source.host}")
    private String sourceHost;

    @Value("${redis.source.port}")
    private int sourcePort;

    @Value("${redis.source.database}")
    private int sourceDatabase;

    @Value("${redis.source.password}")
    private String sourcePassword;

    @Value("${redis.destination.host}")
    private String destinationHost;

    @Value("${redis.destination.port}")
    private int destinationPort;

    @Value("${redis.destination.database}")
    private int destinationDatabase;

    @Value("${redis.destination.password}")
    private String destinationPassword;

    @Bean(name = "sourceRedisConnectionFactory")
    @Lazy
    public RedisConnectionFactory sourceRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(getCurrentSourceHost());
        config.setPort(getCurrentSourcePort());
        config.setDatabase(getCurrentSourceDatabase());
        String password = getCurrentSourcePassword();
        if (password != null && !password.trim().isEmpty()) {
            config.setPassword(password);
        }
        return new JedisConnectionFactory(config);
    }

    @Bean(name = "destinationRedisConnectionFactory")
    @Lazy
    public RedisConnectionFactory destinationRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(getCurrentDestinationHost());
        config.setPort(getCurrentDestinationPort());
        config.setDatabase(getCurrentDestinationDatabase());
        String password = getCurrentDestinationPassword();
        if (password != null && !password.trim().isEmpty()) {
            config.setPassword(password);
        }
        return new JedisConnectionFactory(config);
    }

    @Bean(name = "sourceRedisTemplate")
    @Lazy
    public RedisTemplate<String, Object> sourceRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(sourceRedisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean(name = "destinationRedisTemplate")
    @Lazy
    public RedisTemplate<String, Object> destinationRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(destinationRedisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @Lazy
    public RedisTemplate<String, Object> redisTemplate() {
        // Default redisTemplate bean - delegates to sourceRedisTemplate
        return sourceRedisTemplate();
    }
    
    // Methods to get current values from Environment (refreshed automatically)
    public String getCurrentSourceHost() {
        return environment.getProperty("redis.source.host");
    }

    public int getCurrentSourcePort() {
        return environment.getProperty("redis.source.port", Integer.class);
    }

    public int getCurrentSourceDatabase() {
        return environment.getProperty("redis.source.database", Integer.class);
    }

    public String getCurrentSourcePassword() {
        return environment.getProperty("redis.source.password");
    }

    public String getCurrentDestinationHost() {
        return environment.getProperty("redis.destination.host");
    }

    public int getCurrentDestinationPort() {
        return environment.getProperty("redis.destination.port", Integer.class);
    }

    public int getCurrentDestinationDatabase() {
        return environment.getProperty("redis.destination.database", Integer.class);
    }

    public String getCurrentDestinationPassword() {
        return environment.getProperty("redis.destination.password");
    }

    public String getSourceUrl() {
        try {
            return String.format("redis://%s:%d/%d", getCurrentSourceHost(), getCurrentSourcePort(), getCurrentSourceDatabase());
        } catch (Exception e) {
            return "invalid configuration";
        }
    }

    public String getDestinationUrl() {
        try {
            return String.format("redis://%s:%d/%d", getCurrentDestinationHost(), getCurrentDestinationPort(), getCurrentDestinationDatabase());
        } catch (Exception e) {
            return "invalid configuration";
        }
    }

    // Method to refresh configuration values (needed after loading external config)
    public void refreshConfigValues() {
        // Spring will automatically refresh @Value fields when properties are reloaded
        // This method exists to trigger any necessary re-initialization
    }
}