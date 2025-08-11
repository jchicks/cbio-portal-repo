# Redis Caching Setup for SampleController.getAllSamplesInStudy

## Overview
Redis caching has been implemented for the `SampleController.getAllSamplesInStudy` endpoint to improve performance for this intensive REST call. The caching is designed to work with the existing cBioPortal caching infrastructure.

## What Was Changed

### 1. SampleServiceImpl.java
Added `@Cacheable` annotations to two methods:

```java
@Override
@Cacheable(
    cacheResolver = "staticRepositoryCacheOneResolver",
    condition = "@cacheEnabledConfig.getEnabled()")
public List<Sample> getAllSamplesInStudy(
    String studyId,
    String projection,
    Integer pageSize,
    Integer pageNumber,
    String sortBy,
    String direction)
    throws StudyNotFoundException {
    // ... method implementation
}

@Override
@Cacheable(
    cacheResolver = "staticRepositoryCacheOneResolver",
    condition = "@cacheEnabledConfig.getEnabled()")
public BaseMeta getMetaSamplesInStudy(String studyId) throws StudyNotFoundException {
    // ... method implementation
}
```

## Configuration Steps

### 1. Enable Redis Caching
Update `application.properties`:
```properties
# Change from no-cache to redis
persistence.cache_type=redis
```

### 2. Configure Redis Connection
Uncomment and configure Redis properties in `application.properties`:
```properties
# Redis Configuration
redis.name=cbioportal
redis.leader_address=localhost:6379
redis.follower_address=localhost:6379
redis.database=0
redis.password=your_redis_password
redis.ttl_mins=10000
redis.clear_on_startup=true
```

### 3. Optional: Redis Cluster Setup
For production environments with Redis cluster:
```properties
redis.leader_address=redis-leader:6379
redis.follower_address=redis-follower:6379
```

## Cache Behavior

### Cache Key Generation
The cache uses Spring's default key generation which creates keys based on:
- Method name
- Method parameters (studyId, projection, pageSize, pageNumber, sortBy, direction)

### Cache Invalidation Strategy
- **Automatic**: Cache entries expire after `redis.ttl_mins` (default: 10000 minutes ≈ 7 days)
- **Manual**: Can be cleared via cache management endpoints
- **On Startup**: Cache can be cleared on application startup if `redis.clear_on_startup=true`

### Cache Conditions
- Caching only occurs when `@cacheEnabledConfig.getEnabled()` returns true
- This respects the existing cache configuration system

## Performance Benefits

### Before Caching
- Every call to `/api/studies/{studyId}/samples` queries the database
- Response times depend on database performance and study size
- High load on database for frequently accessed studies

### After Caching
- First call: Database query + cache storage
- Subsequent calls: Redis cache retrieval (typically < 1ms)
- Significant reduction in database load
- Consistent fast response times

## Monitoring and Management

### Cache Statistics
Access cache statistics via:
```
GET /api/cache/stats
```

### Cache Management
Clear specific caches via:
```
DELETE /api/cache/{cacheName}
```

### Redis Monitoring
Monitor Redis performance using:
```bash
redis-cli info stats
redis-cli info memory
```

## Testing

### 1. Verify Caching is Working
1. Enable Redis caching
2. Make a call to `/api/studies/{studyId}/samples`
3. Check Redis for cached data: `redis-cli keys "*"`
4. Make the same call again and verify faster response

### 2. Performance Testing
```bash
# Test without cache
time curl "http://localhost:8080/api/studies/acc_tcga/samples"

# Test with cache (should be faster)
time curl "http://localhost:8080/api/studies/acc_tcga/samples"
```

### 3. Cache Hit Verification
Monitor Redis stats:
```bash
redis-cli info stats | grep keyspace
```

## Troubleshooting

### Common Issues

1. **Cache Not Working**
   - Verify `persistence.cache_type=redis`
   - Check Redis connection settings
   - Ensure `@cacheEnabledConfig.getEnabled()` returns true

2. **Redis Connection Errors**
   - Verify Redis server is running
   - Check network connectivity
   - Validate credentials

3. **Memory Issues**
   - Monitor Redis memory usage
   - Adjust `redis.ttl_mins` for shorter cache expiration
   - Consider Redis memory limits

### Debug Logging
Enable debug logging for cache operations:
```properties
logging.level.org.springframework.cache=DEBUG
logging.level.org.cbioportal.legacy.persistence.util=DEBUG
```

## Production Considerations

### 1. Redis Cluster
For high availability:
- Use Redis cluster with multiple nodes
- Configure `redis.leader_address` and `redis.follower_address`
- Implement proper failover strategies

### 2. Memory Management
- Set appropriate `redis.ttl_mins` based on data freshness requirements
- Monitor Redis memory usage
- Consider Redis persistence options

### 3. Cache Invalidation
- Implement cache invalidation when study data changes
- Consider using cache versioning for major data updates
- Monitor cache hit rates and adjust strategy accordingly

## Rollback Plan
If issues arise, disable Redis caching by reverting:
```properties
persistence.cache_type=no-cache
```

This will fall back to the original behavior without any performance impact.
