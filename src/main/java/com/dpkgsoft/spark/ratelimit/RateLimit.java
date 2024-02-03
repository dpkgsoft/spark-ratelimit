package com.dpkgsoft.spark.ratelimit;

import spark.Request;
import spark.Service;
import spark.Spark;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class RateLimit {
    private final Map<String, Semaphore> keyCache = new HashMap();
    private final int maxRequests;
    private final long resetTime;
    private final TimeUnit timeUnit;
    private final ScheduledExecutorService service;
    private final Function<Request, String> keyFunction;
    private long lastReset;

    public RateLimit(int maxRequests, long resetTime, TimeUnit timeUnit) {
        Objects.requireNonNull(timeUnit, "You need to specify TimeUnit");
        this.maxRequests = maxRequests;
        this.resetTime = resetTime;
        this.timeUnit = timeUnit;
        this.lastReset = System.currentTimeMillis();
        this.keyFunction = Request::ip;
        this.service = Executors.newSingleThreadScheduledExecutor();
        this.scheduleExpiry();
    }

    public RateLimit(int maxRequests, long resetTime, TimeUnit timeUnit, Function<Request, String> keyFunction) {
        Objects.requireNonNull(timeUnit, "You need to specify TimeUnit");
        Objects.requireNonNull(keyFunction, "You need to specify key function");
        this.maxRequests = maxRequests;
        this.resetTime = resetTime;
        this.timeUnit = timeUnit;
        this.lastReset = System.currentTimeMillis();
        this.keyFunction = keyFunction;
        this.service = Executors.newSingleThreadScheduledExecutor();
        this.scheduleExpiry();
    }

    public void map(String path) {
        Spark.before(path, (req, res) -> {
            if (!req.requestMethod().equalsIgnoreCase("options")) {
                Semaphore semaphore = this.getSemaphore(req);
                boolean rateLimited = !semaphore.tryAcquire();
                res.header("X-RateLimit-Limit", String.valueOf(this.maxRequests));
                res.header("X-RateLimit-Remaining", String.valueOf(semaphore.availablePermits()));
                res.header("X-RateLimit-Reset", String.valueOf(Math.max(0L, this.getTimeLeft())));
                if (rateLimited) {
                    Spark.halt(429);
                }
            }
        });
    }

    public void map(Service service, String path) {
        service.before(path, (req, res) -> {
            if (!req.requestMethod().equalsIgnoreCase("options")) {
                Semaphore semaphore = this.getSemaphore(req);
                boolean rateLimited = !semaphore.tryAcquire();
                res.header("X-RateLimit-Limit", String.valueOf(this.maxRequests));
                res.header("X-RateLimit-Remaining", String.valueOf(semaphore.availablePermits()));
                res.header("X-RateLimit-Reset", String.valueOf(Math.max(0L, this.getTimeLeft())));
                if (rateLimited) {
                    service.halt(429);
                }
            }
        });
    }

    public boolean tryAcquire(Request request) {
        return this.getSemaphore(request).tryAcquire();
    }

    public long getTimeLeft() {
        return this.lastReset + this.timeUnit.toMillis(this.resetTime) - System.currentTimeMillis();
    }

    public int getRequestsLeft(Request request) {
        return this.getSemaphore(request).availablePermits();
    }

    public void stop() {
        this.service.shutdownNow();
    }

    private void scheduleExpiry() {
        this.service.scheduleAtFixedRate(() -> {
            this.lastReset = System.currentTimeMillis();
            Iterator var1 = this.keyCache.entrySet().iterator();

            while(var1.hasNext()) {
                Map.Entry<String, Semaphore> entry = (Map.Entry)var1.next();
                Semaphore semaphore = (Semaphore)entry.getValue();
                if (semaphore.availablePermits() == this.maxRequests) {
                    this.keyCache.remove(entry.getKey());
                } else {
                    semaphore.release(this.maxRequests - semaphore.availablePermits());
                }
            }

        }, this.resetTime, this.resetTime, this.timeUnit);
    }

    private Semaphore getSemaphore(Request request) {
        return this.keyCache.computeIfAbsent(this.keyFunction.apply(request), (s) -> new Semaphore(this.maxRequests));
    }
}
