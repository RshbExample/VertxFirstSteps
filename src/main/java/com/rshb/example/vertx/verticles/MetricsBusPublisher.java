package com.rshb.example.vertx.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.micrometer.MetricsService;

public class MetricsBusPublisher extends AbstractVerticle {
  @Override
  public void start(Promise<Void> startPromise) throws Exception {
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
