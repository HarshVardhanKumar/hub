package com.flightstats.hub.health;

import com.flightstats.hub.app.HubServices;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Singleton;

import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class HubHealthCheck {

    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicBoolean startup = new AtomicBoolean(true);

    public HubHealthCheck() {
        HubServices.register(new HealthService(), HubServices.TYPE.FINAL_POST_START);
    }

    private class HealthService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            startup.set(false);
        }

        @Override
        protected void shutDown() throws Exception {
            shutdown();
        }
    }

    public HealthStatus getStatus() {
        HealthStatus.HealthStatusBuilder builder = HealthStatus.builder();
        if (shutdown.get()) {
            return builder.healthy(false).description("SHUTTING DOWN").build();
        }
        if (startup.get()) {
            return builder.healthy(false).description("Starting up...").build();
        }
        return builder.healthy(true).description("OK").build();
    }

    public void shutdown() {
        shutdown.set(true);
    }

}
