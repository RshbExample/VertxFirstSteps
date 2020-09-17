package com.rshb.example.vertx.verticles;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.micrometer.MetricsService;
import io.vertx.micrometer.backends.BackendRegistries;

import java.util.concurrent.TimeUnit;

public class MetrixBusPublisher extends AbstractVerticle {
  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    MeterRegistry registry = BackendRegistries.getDefaultNow();

    /*new ClassLoaderMetrics().bindTo(registry);
    new JvmMemoryMetrics().bindTo(registry);
    new ProcessorMetrics().bindTo(registry);
    new JvmThreadMetrics().bindTo(registry);*/

    MetricsService metricsService = MetricsService.create(vertx);

    vertx.setPeriodic(
      1000,
      h ->
        vertx.eventBus().publish("metrics", metricsService.getMetricsSnapshot().encode())
    );
    vertx.eventBus().consumer("metrics", h -> System.out.println(h.body()));
    startPromise.complete();
  }
}
