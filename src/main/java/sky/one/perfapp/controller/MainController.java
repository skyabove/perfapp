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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;

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

    /**
     * Example:
     * /perfapp/work?cpuMs=30&sleepMs=70
     * <p>
     * cpuMs   — cpu time in ms to consume by the current thread
     * sleepMs — time to sleep (IO wait)
     */
    @GetMapping("/work")
    public ResponseEntity<Map<String, Object>> work(@RequestParam(defaultValue = "0") long cpuMs,
                                                    @RequestParam(defaultValue = "0") long sleepMs) {
        if (cpuMs < 0 || sleepMs < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "cpuMs and sleepMs must be >= 0"));
        }

        long startWall = System.nanoTime();
        long startCpu = currentThreadCpuNanos();

        // 1) CPU burn
        burnCpuForCpuTime(cpuMs);

        // 2) usleep/sleep
        if (sleepMs > 0) {
            LockSupport.parkNanos(sleepMs * 1_000_000L);
        }

        long endCpu = currentThreadCpuNanos();
        long endWall = System.nanoTime();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ts", Instant.now().toString());
        res.put("requestedCpuMs", cpuMs);
        res.put("requestedSleepMs", sleepMs);
        res.put("measuredCpuMs", (endCpu - startCpu) / 1_000_000.0);
        res.put("measuredWallMs", (endWall - startWall) / 1_000_000.0);
        return ResponseEntity.ok(res);
    }

    private static long currentThreadCpuNanos() {
        return THREAD_MX_BEAN.getCurrentThreadCpuTime();
    }

    /**
     * consumes cpuMs milis CPU time for current thread.
     */
    private static void burnCpuForCpuTime(long cpuMs) {
        if (cpuMs <= 0) return;

        long targetCpuNanos = cpuMs * 1_000_000L;
        long startCpu = currentThreadCpuNanos();

        // prevent jit dead code optimization
        double x = 1.0;

        while ((currentThreadCpuNanos() - startCpu) < targetCpuNanos) {
            // some CPU load
            x = x * 1.0000001 + Math.sqrt(x);
            if (x > 1e9) x = 1.0;
        }

    }
}
