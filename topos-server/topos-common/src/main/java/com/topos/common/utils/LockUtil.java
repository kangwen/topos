package com.topos.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁工具（优先 Redis；无 Redis 时降级为 JVM 内锁）。
 *
 * <p>用法：runWithinLock(key, ttl, () -> { ... })</p>
 */
@Component
public class LockUtil {

    private static final Logger log = LoggerFactory.getLogger(LockUtil.class);

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(
                    // language=lua
                    """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                      return redis.call('del', KEYS[1])
                    else
                      return 0
                    end
                    """,
                    Long.class);

    private final StringRedisTemplate redis;
    private final ConcurrentHashMap<String, Object> jvmLocks = new ConcurrentHashMap<>();

    public LockUtil(@Autowired(required = false) StringRedisTemplate redis) {
        this.redis = redis;
    }

    public <T> T runWithinLock(String key, Duration ttl, Supplier<T> fn) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(ttl, "ttl");
        Objects.requireNonNull(fn, "fn");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        if (redis == null) {
            Object lock = jvmLocks.computeIfAbsent(key, k -> new Object());
            synchronized (lock) {
                return fn.get();
            }
        }
        String token = UUID.randomUUID().toString();
        boolean acquired = false;
        try {
            acquired =
                    Boolean.TRUE.equals(
                            redis.opsForValue().setIfAbsent(key, token, ttl.toMillis(), TimeUnit.MILLISECONDS));
            if (!acquired) {
                throw new IllegalStateException("系统繁忙，请稍后重试");
            }
            return fn.get();
        } finally {
            if (acquired) {
                try {
                    redis.execute(RELEASE_SCRIPT, java.util.List.of(key), token);
                } catch (DataAccessException ignored) {
                    // 释放失败不影响主流程；TTL 会兜底自动过期
                }
            }
        }
    }

    public <T> T runWithinLock(String key, long ttlMillis, Supplier<T> fn) {
        return runWithinLock(key, Duration.ofMillis(ttlMillis), fn);
    }

    /**
     * 抢占「周期级」分布式锁：仅首个实例 {@code SET NX} 成功；成功后执行任务，且<strong>不在</strong>结束时删除 key，
     * 直至 TTL 自然过期，从而在同一周期内集群只跑一轮。
     *
     * <p>任务抛出运行时异常或 {@link Error} 时会释放锁，便于其他实例或后续调度重试。</p>
     *
     * <p>未配置 Redis 时记录告警并仍执行任务（本地开发无 Redis 时行为与单实例一致）。</p>
     *
     * @return {@code true} 已执行；{@code false} 未抢到锁已跳过
     */
    public boolean tryRunWithExclusivePeriodLock(String key, Duration period, Runnable action) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(period, "period");
        Objects.requireNonNull(action, "action");
        if (period.isZero() || period.isNegative()) {
            throw new IllegalArgumentException("period must be positive");
        }
        if (redis == null) {
            log.warn("Redis not available, running without exclusive period lock: {}", key);
            action.run();
            return true;
        }
        String token = UUID.randomUUID().toString();
        boolean acquired =
                Boolean.TRUE.equals(
                        redis.opsForValue().setIfAbsent(key, token, period.toMillis(), TimeUnit.MILLISECONDS));
        if (!acquired) {
            log.info("Skipped (exclusive period lock not acquired): {}", key);
            return false;
        }
        try {
            action.run();
            return true;
        } catch (RuntimeException | Error e) {
            try {
                redis.execute(RELEASE_SCRIPT, java.util.List.of(key), token);
            } catch (DataAccessException ignored) {
                // TTL 仍会兜底
            }
            throw e;
        }
    }
}

