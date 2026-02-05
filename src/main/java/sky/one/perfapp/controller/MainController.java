package sky.one.perfapp.controller;

import lombok.RequiredArgsConstructor;
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

@RestController
@RequiredArgsConstructor
public class MainController {
    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();

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
