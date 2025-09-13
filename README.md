# Redis Data Copier

A Spring Boot console application that copies data from a source Redis database to a destination Redis database based on a specified key.

## Features

- Connects to two Redis databases (source and destination)
- Tests connectivity and displays connection URLs at startup
- Command-line arguments support or interactive console interface
- Supports all Redis data types: String, List, Set, Sorted Set (ZSet), and Hash
- Preserves TTL (Time To Live) when copying keys
- Proper error handling and user feedback

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Access to two Redis databases (source and destination)

## Configuration

Edit the `src/main/resources/application.properties` file to configure your Redis connections:

```properties
# Source Redis Database Configuration
redis.source.host=localhost
redis.source.port=6379
redis.source.database=0
redis.source.password=
redis.source.timeout=2000

# Destination Redis Database Configuration
redis.destination.host=localhost
redis.destination.port=6380
redis.destination.database=0
redis.destination.password=
redis.destination.timeout=2000
```

## External Configuration

You can use an external configuration file to override the default settings by using the `--config` option. This is useful for:

- **Environment-specific configurations** (dev, staging, production)
- **Multiple Redis setups** without rebuilding the application
- **Security**: Keep sensitive configurations outside the JAR file
- **CI/CD pipelines**: Use different configurations per environment

### External Configuration File Format

Create a `.properties` file with the same structure as `application.properties`:

```properties
# Source Redis Database Configuration
redis.source.host=prod-source-redis.example.com
redis.source.port=6379
redis.source.database=1
redis.source.password=source-password-here
redis.source.timeout=5000

# Destination Redis Database Configuration
redis.destination.host=prod-dest-redis.example.com
redis.destination.port=6379
redis.destination.database=2
redis.destination.password=dest-password-here
redis.destination.timeout=5000

# Optional: Application Configuration
logging.level.com.centrohub.redis.copier=DEBUG
```

### Configuration Priority

The application loads configuration in this order (higher priority overrides lower):

1. **External config file** (via `--config` option) - **HIGHEST PRIORITY**
2. **Default application.properties** - LOWEST PRIORITY

### Expected Configuration Properties

| Property | Required | Default | Description |
|----------|----------|---------|-------------|
| `redis.source.host` | Yes | localhost | Source Redis server hostname/IP |
| `redis.source.port` | Yes | 6379 | Source Redis server port |
| `redis.source.database` | No | 0 | Source Redis database number |
| `redis.source.password` | No | (empty) | Source Redis password |
| `redis.source.timeout` | No | 2000 | Source Redis connection timeout (ms) |
| `redis.destination.host` | Yes | localhost | Destination Redis server hostname/IP |
| `redis.destination.port` | Yes | 6380 | Destination Redis server port |
| `redis.destination.database` | No | 0 | Destination Redis database number |
| `redis.destination.password` | No | (empty) | Destination Redis password |
| `redis.destination.timeout` | No | 2000 | Destination Redis connection timeout (ms) |

### Sample Configuration Files

The project includes sample configuration files in the `sample-configs/` directory:

- **`production-redis.properties`**: Production environment template
- **`staging-redis.properties`**: Staging environment template  
- **`local-docker-redis.properties`**: Local Docker setup template

Copy and modify these files for your environment:

```bash
# Copy a sample configuration
cp sample-configs/production-redis.properties my-redis-config.properties

# Edit with your Redis server details
nano my-redis-config.properties

# Use the custom configuration
java -jar target/redis-data-copier-0.0.1-SNAPSHOT.jar --config=my-redis-config.properties --key=mykey
```

## Build Instructions

1. Clone or download the project
2. Navigate to the project directory
3. Build the application:
   ```bash
   mvn clean package -DskipTests
   ```

## Usage

### Option 1: With Command Line Arguments

Run the application with a key as argument:
```bash
java -jar target/redis-data-copier-0.0.1-SNAPSHOT.jar --key=myrediskey
```

Or using Maven:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--key=myrediskey"
```

### Option 2: With External Configuration File

Use a custom configuration file to override default settings:
```bash
java -jar target/redis-data-copier-0.0.1-SNAPSHOT.jar --config=/path/to/redis-config.properties --key=myrediskey
```

Or in interactive mode:
```bash
java -jar target/redis-data-copier-0.0.1-SNAPSHOT.jar --config=./my-redis.properties
```

### Option 3: Interactive Mode

Run without arguments for interactive mode (uses default configuration):
```bash
java -jar target/redis-data-copier-0.0.1-SNAPSHOT.jar
```

Or:
```bash
mvn spring-boot:run
```

### Help

To see help information, run:
```bash
java -jar target/redis-data-copier-0.0.1-SNAPSHOT.jar --help
```

## Application Flow

1. **Startup**: Tests connections to both Redis databases
2. **Connection Display**: Shows URLs of source and destination databases
3. **Key Input**: Accepts key via command line argument or prompts user
4. **Key Processing**:
   - Checks if key exists in source database
   - Identifies data type (String, List, Set, ZSet, Hash)
   - Copies all data to destination database
   - Preserves TTL if present
5. **Feedback**: Displays success/error messages
6. **Interactive Mode**: Continues to prompt for more keys until user types 'exit'

## Examples

### Example 1: Copy a string key (default configuration)
```bash
# Command line mode
java -jar target/redis-data-copier-0.0.1-SNAPSHOT.jar --key=user:1001

# Expected output:
# Testing Redis connections...
# ✓ Source Redis connected: redis://localhost:6379/0
# ✓ Destination Redis connected: redis://localhost:6380/0
# Found key 'user:1001' of type 'string'. Starting copy process...
# ✓ Successfully copied key 'user:1001' from source to destination Redis.
```

### Example 2: Using external configuration file
```bash
# Create external config file
cat > production-redis.properties << EOF
redis.source.host=prod-redis-01.company.com
redis.source.port=6379
redis.source.database=0
redis.source.password=prod-source-password
redis.destination.host=prod-redis-02.company.com
redis.destination.port=6379
redis.destination.database=1
redis.destination.password=prod-dest-password
EOF

# Use external configuration
java -jar target/redis-data-copier-0.0.1-SNAPSHOT.jar --config=production-redis.properties --key=user:1001

# Expected output:
# ✓ External configuration loaded from: production-redis.properties
# Testing Redis connections...
# ✓ Source Redis connected: redis://prod-redis-01.company.com:6379/0
# ✓ Destination Redis connected: redis://prod-redis-02.company.com:6379/1
# Found key 'user:1001' of type 'string'. Starting copy process...
# ✓ Successfully copied key 'user:1001' from source to destination Redis.
```

### Example 3: Interactive mode with external config
```bash
java -jar target/redis-data-copier-0.0.1-SNAPSHOT.jar --config=./staging-redis.properties

# Expected interaction:
# ✓ External configuration loaded from: ./staging-redis.properties
# Testing Redis connections...
# ✓ Source Redis connected: redis://staging-source:6379/0
# ✓ Destination Redis connected: redis://staging-dest:6379/0
# 
# Enter the key to copy (or 'exit' to quit): session:abc123
# Found key 'session:abc123' of type 'hash'. Starting copy process...
# TTL copied: 3600 seconds
# ✓ Successfully copied key 'session:abc123' from source to destination Redis.
# 
# Enter the key to copy (or 'exit' to quit): exit
# Application terminated by user.
```

### Example 4: Non-existent key
```bash
java -jar target/redis-data-copier-0.0.1-SNAPSHOT.jar --key=nonexistent

# Expected output:
# Key 'nonexistent' does not exist in source Redis database.
```

### Example 5: Configuration file not found
```bash
java -jar target/redis-data-copier-0.0.1-SNAPSHOT.jar --config=missing-file.properties

# Expected output:
# Configuration file not found: missing-file.properties
# [Application exits with error code 1]
```

## Supported Redis Data Types

- **String**: Simple key-value pairs
- **List**: Ordered collections of strings (LPUSH, RPUSH operations)
- **Set**: Unordered collections of unique strings (SADD operations)
- **Sorted Set (ZSet)**: Ordered collections with scores (ZADD operations)
- **Hash**: Maps between string fields and string values (HSET operations)

## Error Handling

The application handles various error scenarios:
- Redis connection failures
- Non-existent keys
- Network timeouts
- Authentication errors
- Unsupported data types

## Troubleshooting

1. **Connection Issues**: Verify Redis servers are running and accessible
2. **Authentication**: Check if passwords are correctly configured
3. **Permissions**: Ensure the application has read access to source and write access to destination
4. **Network**: Verify network connectivity between application and Redis servers