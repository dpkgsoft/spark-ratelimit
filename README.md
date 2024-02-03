# Spark Ratelimit
## Description
Tiny library which allows to set up rate-limiting in Spark in 2 easy steps! If you run your own API, this library is for you.

## Usage
This rate-limiter add 3 headers when a request comes in, it will then test if the max amount has been hit. If it does get it then this sends an empty `429` status code.

### Headers
```
X-RateLimit-Limit: 30
X-RateLimit-Remaining: 29
X-RateLimit-Reset: 49999
```

### Basic
This example allows 30 requests per minute using IP address as the defining key.
This will map to all /api requests.

```java
private final RateLimit rateLimiter = new RateLimit(30, 1, TimeUnit.MINUTES);
    
public void setup() {
    rateLimiter.map("/api/*");
    
    Spark.path("/api", () -> Spark.get("/", (req, res) -> "{\"Hello\", \"World!\"}"));
}
```

### Defining the key
To define the key just add new parameter to the `RateLimit` constuctor.

```java
private final RateLimit rateLimiter = new RateLimit(30, 1, TimeUnit.MINUTES, Request::queryString);

public void setup() {
    rateLimiter.map("/api/*");

    Spark.path("/api", () -> Spark.get("/", (req, res) -> "{\"Hello\", \"World!\"}"));
}
```

### Map to specific endpoint
```java
private final RateLimit rateLimiter = new RateLimit(30, 1, TimeUnit.MINUTES);
    
public void setup() {
    rateLimiter.map("/api/some/endpoint");
    
    // Since we only map to `/api/some/endpoint` this GET will not be rate-limited.
    Spark.path("/api", () -> Spark.get("/", (req, res) -> "{\"Hello\", \"World!\"}"));
}
```

## Installation

Current version is: \
![Maven Central Version](https://img.shields.io/maven-central/v/com.dpkgsoft/spark-ratelimit)

### Maven
```xml
<dependencies>
    <dependency>
        <groupId>com.dpkgsoft</groupId>
        <artifactId>spark-ratelimit</artifactId>
        <version>VERSION, SEE ABOVE</version>
    </dependency>
</dependencies>
```

### Gradle
```groovy
dependencies {
    implementation 'com.dpkgsoft:spark-ratelimit:VERSION'
}
```