package com.lanyuan.starter.utils;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class LockUtil {

    private static final int MAX_FAIL_COUNT = 5;
    private static final int LOCK_MINUTES = 30;

    private final ConcurrentHashMap<String, AtomicInteger> failCountMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lockTimeMap = new ConcurrentHashMap<>();

    public void recordFail(String username) {
        failCountMap.computeIfAbsent(username, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void resetFail(String username) {
        failCountMap.remove(username);
        lockTimeMap.remove(username);
    }

    public boolean isLocked(String username) {
        Long lockTime = lockTimeMap.get(username);
        if (lockTime == null) return false;
        if (System.currentTimeMillis() - lockTime > LOCK_MINUTES * 60 * 1000) {
            lockTimeMap.remove(username);
            failCountMap.remove(username);
            return false;
        }
        return true;
    }

    public void lockUser(String username) {
        lockTimeMap.put(username, System.currentTimeMillis());
    }

    public long getRemainingLockMinutes(String username) {
        Long lockTime = lockTimeMap.get(username);
        if (lockTime == null) return 0;
        long remaining = LOCK_MINUTES * 60 * 1000 - (System.currentTimeMillis() - lockTime);
        return remaining > 0 ? remaining / (60 * 1000) : 0;
    }

    public int getFailCount(String username) {
        AtomicInteger count = failCountMap.get(username);
        return count != null ? count.get() : 0;
    }
}
