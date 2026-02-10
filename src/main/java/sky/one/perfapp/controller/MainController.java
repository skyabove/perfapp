package sky.one.perfapp.controller;

import lombok.RequiredArgsConstructor;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sky.one.perfapp.dto.UserDto;
import sky.one.perfapp.service.UserService;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.tomcat.util.threads.ThreadPoolExecutor;

@RestController
@RequiredArgsConstructor
public class MainController {
    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

    private final ServletWebServerApplicationContext ctx;
    private final AtomicInteger active = new AtomicInteger();

    static {
        // enable CPU time thread measure
        if (THREAD_MX_BEAN.isThreadCpuTimeSupported() && !THREAD_MX_BEAN.isThreadCpuTimeEnabled()) {
            THREAD_MX_BEAN.setThreadCpuTimeEnabled(true);
        }
    }

    private final UserService userService;

    @GetMapping
    public String getMessage() {
        return "Main controller is up!";
    }

    /**
     * Example:
     * /sleep?sleepMs=150
     * <p>
     * sleepMs -- sleep time in milliseconds for this request
     */
    @GetMapping("/sleep")
    public Map<String, Object> sleep(@RequestParam(defaultValue = "0") long sleepMs) {
        int now = active.incrementAndGet();
        try {
            Thread.sleep(sleepMs);
            return Map.of("active", now);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            active.decrementAndGet();
        }
    }

    @GetMapping("/active")
    public Map<String, Object> active() {
        return Map.of("active", active.get());
    }

    @GetMapping("/users")
    public List<UserDto> getUsers() {
        return userService.getUsers();
    }

    @GetMapping("/users/cached")
    public List<UserDto> getCachedUsers() {
        return userService.getCachedUsers();
    }

    @GetMapping("/users/random-one")
    public UserDto getRandomUser() {
        return userService.getRandomUser();
    }

    /**
     * Examples:
     * /users/random-five
     * /users/random-five?singleSelect=true
     */
    @GetMapping("/users/random-five")
    public List<UserDto> getFiveRandomUsers(@RequestParam(defaultValue = "false") boolean singleSelect) {
        return userService.getFiveRandomUsers(singleSelect);
    }

    @GetMapping("/users/random-one/cached")
    public UserDto getRandomUserCached() {
        return userService.getRandomUserCached();
    }

    /**
     * Examples:
     * /users/random-five/cached
     * /users/random-five/cached?singleSelect=true
     */
    @GetMapping("/users/random-five/cached")
    public List<UserDto> getFiveRandomUsersCached(@RequestParam(defaultValue = "false") boolean singleSelect) {
        return userService.getFiveRandomUsersCached(singleSelect);
    }

    /**
     * Example:
     * /perfapp/work?cpuMs=30
     * <p>
     * cpuMs -- cpu time in ms to consume by the current thread
     */
    @GetMapping("/work")
    public ResponseEntity<Map<String, Object>> work(@RequestParam(defaultValue = "0") long cpuMs) {

        long startWall = System.nanoTime();
        long startCpu = currentThreadCpuNanos();

        // CPU burn with deterministic hashing
        consumeCpuWithMd5(cpuMs);

        long endCpu = currentThreadCpuNanos();
        long endWall = System.nanoTime();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ts", Instant.now().toString());
        res.put("requestedCpuMs", cpuMs);
        res.put("measuredCpuMs", (endCpu - startCpu) / 1_000_000.0);
        res.put("measuredWallMs", (endWall - startWall) / 1_000_000.0);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> out = new LinkedHashMap<>();

        var webServer = ctx.getWebServer();
        if (!(webServer instanceof TomcatWebServer tomcatWebServer)) {
            out.put("error", "Not a TomcatWebServer: " + webServer.getClass().getName());
            return out;
        }

        Map<String, Object> connectorsOut = new LinkedHashMap<>();

        for (Connector c : tomcatWebServer.getTomcat().getService().findConnectors()) {
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("protocol", c.getProtocol());
            one.put("port", c.getPort());

            ProtocolHandler ph = c.getProtocolHandler();

            if (ph instanceof AbstractProtocol<?> ap) {
                one.put("maxConnections", ap.getMaxConnections());
                one.put("acceptCount", ap.getAcceptCount());

                Executor ex = ap.getExecutor();
                if (ex instanceof ThreadPoolExecutor tpe) {
                    one.put("maxThreads", tpe.getMaximumPoolSize());
                    one.put("currentThreadCount", tpe.getPoolSize());
                    one.put("currentThreadsBusy", tpe.getActiveCount());
                    one.put("queueSize", tpe.getQueue() != null ? tpe.getQueue().size() : null);
                } else if (ex != null) {
                    one.put("executorClass", ex.getClass().getName());
                }
            } else {
                one.put("protocolHandlerClass", ph.getClass().getName());
                one.put("note", "ProtocolHandler is not AbstractProtocol, cannot read maxConnections/acceptCount safely");
            }

            connectorsOut.put("connector-" + c.getPort(), one);
        }

        out.put("tomcat", connectorsOut);
        return out;
    }

    private static long currentThreadCpuNanos() {
        return THREAD_MX_BEAN.getCurrentThreadCpuTime();
    }

    /**
     * consumes cpuMs millis CPU time for current thread.
     */
    private static void consumeCpuWithMd5(long cpuMs) {
        if (cpuMs <= 0) return;

        long targetCpuNanos = cpuMs * 1_000_000L;
        long startCpu = currentThreadCpuNanos();

        MessageDigest md5 = md5();
        byte[] block = new byte[8 * 1024];
        long counter = 0L;

        while ((currentThreadCpuNanos() - startCpu) < targetCpuNanos) {
            md5.update(block);
            byte[] digest = md5.digest();
            block[0] ^= digest[0];
            block[1] ^= (byte) counter;
            counter++;
        }

    }

    private static MessageDigest md5() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }
}
