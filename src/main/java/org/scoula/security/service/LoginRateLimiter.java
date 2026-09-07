package org.scoula.security.service;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class LoginRateLimiter {
    private static final int MAX_FAILURES = 5;
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(5);
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(10);
    private static final long CLEANUP_INTERVAL = 256;

    private final ConcurrentMap<LoginKey, AttemptState> attempts = new ConcurrentHashMap<>();
    private final AtomicLong operationCount = new AtomicLong();

    public boolean isBlocked(HttpServletRequest request, String email) {
        Instant now = Instant.now();
        LoginKey key = key(request, email);
        AtomicBoolean blocked = new AtomicBoolean(false);
        attempts.computeIfPresent(key, (ignored, state) -> {
            if (state.isBlocked(now)) {
                blocked.set(true);
                return state;
            }
            AttemptState recent = state.withRecentFailures(now);
            return recent.isExpired(now) ? null : recent;
        });
        cleanupOccasionally(now);
        return blocked.get();
    }

    public boolean recordFailure(HttpServletRequest request, String email) {
        Instant now = Instant.now();
        AtomicBoolean blocked = new AtomicBoolean(false);
        attempts.compute(key(request, email), (ignored, previous) -> {
            AttemptState current = previous == null
                    ? AttemptState.empty()
                    : previous.withRecentFailures(now);
            if (current.isBlocked(now)) {
                blocked.set(true);
                return current;
            }

            List<Instant> failures = new ArrayList<>(current.failures());
            failures.add(now);
            if (failures.size() >= MAX_FAILURES) {
                blocked.set(true);
                return new AttemptState(List.of(), now.plus(BLOCK_DURATION));
            }
            return new AttemptState(List.copyOf(failures), null);
        });
        cleanupOccasionally(now);
        return blocked.get();
    }

    public void reset(HttpServletRequest request, String email) {
        attempts.remove(key(request, email));
    }

    private LoginKey key(HttpServletRequest request, String email) {
        return new LoginKey(resolveIp(request), normalizeEmail(email));
    }

    private String resolveIp(HttpServletRequest request) {
        String proxyIp = request.getHeader("X-Real-IP");
        if (proxyIp != null && !proxyIp.isBlank() && proxyIp.length() <= 64
                && !proxyIp.contains(",")) {
            return proxyIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "<missing>";
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return normalized.length() <= 254 ? normalized : normalized.substring(0, 254);
    }

    private void cleanupOccasionally(Instant now) {
        if (operationCount.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            attempts.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
        }
    }

    private record LoginKey(String ip, String email) {
    }

    private record AttemptState(List<Instant> failures, Instant blockedUntil) {
        private static AttemptState empty() {
            return new AttemptState(List.of(), null);
        }

        private boolean isBlocked(Instant now) {
            return blockedUntil != null && now.isBefore(blockedUntil);
        }

        private AttemptState withRecentFailures(Instant now) {
            if (isBlocked(now)) {
                return this;
            }
            if (blockedUntil != null) {
                return empty();
            }
            Instant cutoff = now.minus(FAILURE_WINDOW);
            List<Instant> recent = failures.stream()
                    .filter(failure -> !failure.isBefore(cutoff))
                    .toList();
            return recent.isEmpty() ? empty() : new AttemptState(recent, null);
        }

        private boolean isExpired(Instant now) {
            if (blockedUntil != null) {
                return !now.isBefore(blockedUntil);
            }
            Instant cutoff = now.minus(FAILURE_WINDOW);
            return failures.isEmpty()
                    || failures.stream().allMatch(failure -> failure.isBefore(cutoff));
        }
    }
}
