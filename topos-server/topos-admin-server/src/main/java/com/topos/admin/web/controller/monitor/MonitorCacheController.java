package com.topos.admin.web.controller.monitor;

import com.topos.admin.common.core.domain.AdminResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 缓存监控（Redis INFO，与 RuoYi 前端字段对齐）。
 */
@RestController
@RequestMapping("/monitor/cache")
public class MonitorCacheController {

    private final StringRedisTemplate stringRedisTemplate;

    public MonitorCacheController(@Autowired(required = false) StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping
    public AdminResult cache() {
        Map<String, Object> body = new HashMap<>();
        body.put("info", redisInfo());
        body.put("dbSize", dbSize());
        body.put("commandStats", commandStats());
        return AdminResult.success(body);
    }

    @GetMapping("/getNames")
    public AdminResult names() {
        return AdminResult.success(new ArrayList<String>());
    }

    @GetMapping("/getKeys/{cacheName}")
    public AdminResult keys(@PathVariable String cacheName) {
        return AdminResult.success(new ArrayList<String>());
    }

    @GetMapping("/getValue/{cacheName}/{cacheKey}")
    public AdminResult value(@PathVariable String cacheName, @PathVariable String cacheKey) {
        return AdminResult.success("");
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @DeleteMapping("/clearCacheName/{cacheName}")
    public AdminResult clearName(@PathVariable String cacheName) {
        return AdminResult.success();
    }

    /**
     * 与前端 {@code clearCacheKey/{cacheKey}} 一致（仅一个路径段）。
     */
    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @DeleteMapping("/clearCacheKey/{cacheKey:.+}")
    public AdminResult clearKey(@PathVariable String cacheKey) {
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @DeleteMapping("/clearCacheAll")
    public AdminResult clearAll() {
        if (stringRedisTemplate != null) {
            stringRedisTemplate.execute((RedisCallback<Object>) connection -> {
                connection.serverCommands().flushDb();
                return null;
            });
        }
        return AdminResult.success();
    }

    private Map<String, String> redisInfo() {
        Map<String, String> map = defaultInfo();
        if (stringRedisTemplate == null) {
            return map;
        }
        try {
            Properties p = stringRedisTemplate.execute((RedisCallback<Properties>) RedisConnection::info);
            if (p != null && !p.isEmpty()) {
                for (String name : p.stringPropertyNames()) {
                    map.put(name.replace('-', '_'), p.getProperty(name));
                }
            }
        } catch (DataAccessException ignored) {
            map.put("redis_version", "error");
        }
        ensureUiKeys(map);
        return map;
    }

    private static Map<String, String> defaultInfo() {
        Map<String, String> map = new HashMap<>();
        map.put("redis_version", "N/A");
        map.put("redis_mode", "standalone");
        map.put("tcp_port", "6379");
        map.put("connected_clients", "0");
        map.put("uptime_in_days", "0");
        map.put("used_memory_human", "0B");
        map.put("used_cpu_user_children", "0");
        map.put("maxmemory_human", "0B");
        map.put("aof_enabled", "0");
        map.put("rdb_last_bgsave_status", "ok");
        map.put("instantaneous_input_kbps", "0");
        map.put("instantaneous_output_kbps", "0");
        return map;
    }

    /** 前端表格与图表依赖的键（Redis 无对应项时补默认值）。 */
    private static void ensureUiKeys(Map<String, String> map) {
        map.putIfAbsent("redis_version", "N/A");
        map.putIfAbsent("redis_mode", "standalone");
        map.putIfAbsent("tcp_port", map.getOrDefault("tcp_port", "6379"));
        map.putIfAbsent("connected_clients", "0");
        map.putIfAbsent("uptime_in_days", "0");
        map.putIfAbsent("used_memory_human", "0B");
        map.putIfAbsent("used_cpu_user_children", "0");
        map.putIfAbsent("maxmemory_human", "0B");
        map.putIfAbsent("aof_enabled", "0");
        map.putIfAbsent("rdb_last_bgsave_status", "ok");
        map.putIfAbsent("instantaneous_input_kbps", "0");
        map.putIfAbsent("instantaneous_output_kbps", "0");
    }

    private long dbSize() {
        if (stringRedisTemplate == null) {
            return 0;
        }
        try {
            Long n = stringRedisTemplate.execute((RedisCallback<Long>) c -> c.serverCommands().dbSize());
            return n == null ? 0 : n;
        } catch (DataAccessException e) {
            return 0;
        }
    }

    /**
     * ECharts 饼图：{@code [{ name, value }]}；从 INFO commandstats 解析调用次数。
     */
    private List<Map<String, Object>> commandStats() {
        List<Map<String, Object>> list = new ArrayList<>();
        if (stringRedisTemplate == null) {
            list.add(singleSlice("(无 Redis)", 1));
            return list;
        }
        try {
            Properties p = stringRedisTemplate.execute(
                    (RedisCallback<Properties>) c -> c.info("commandstats"));
            if (p != null) {
                for (String key : p.stringPropertyNames()) {
                    if (!key.startsWith("cmdstat_")) {
                        continue;
                    }
                    String cmd = key.substring("cmdstat_".length());
                    String raw = p.getProperty(key);
                    long calls = parseCalls(raw);
                    if (calls > 0) {
                        Map<String, Object> slice = new HashMap<>();
                        slice.put("name", cmd);
                        slice.put("value", calls);
                        list.add(slice);
                    }
                }
            }
        } catch (DataAccessException ignored) {
            // fall through
        }
        if (list.isEmpty()) {
            list.add(singleSlice("(无命令统计)", 1));
        }
        return list;
    }

    private static Map<String, Object> singleSlice(String name, long value) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("value", value);
        return m;
    }

    private static long parseCalls(String cmdstatLine) {
        if (cmdstatLine == null) {
            return 0;
        }
        for (String part : cmdstatLine.split(",")) {
            part = part.trim();
            if (part.startsWith("calls=")) {
                try {
                    return Long.parseLong(part.substring("calls=".length()));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
}
