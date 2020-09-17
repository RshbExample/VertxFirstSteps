package com.rshb.example.vertx;

import com.rshb.example.vertx.verticles.MetrixBusPublisher;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.micrometer.PrometheusScrapingHandler;

import java.util.stream.Collector;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {
    WebClient webClient = createWebClient();

    Router router = Router.router(vertx);
    router.get("/hello").handler(rc -> {
      rc.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
        .end("Hello from Vert.x!");
    });
    router.mountSubRouter("/rest/api/v1/", createRestRouter(webClient));
    router.route("/metrics").handler(PrometheusScrapingHandler.create());

    SockJSBridgeOptions opts = new SockJSBridgeOptions()
      .addOutboundPermitted(new PermittedOptions()
        .setAddress("metrics"));
    router.mountSubRouter("/eventbus", SockJSHandler.create(vertx).bridge(opts));

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8888, http -> {
        if (http.succeeded()) {
          startPromise.complete();
          System.out.println("HTTP server started on port 8888");
        } else {
          startPromise.fail(http.cause());
        }
      });

    vertx.deployVerticle(MetrixBusPublisher.class.getName());
  }

  private WebClient createWebClient() {
    WebClientOptions webClientOptions = new WebClientOptions();
    webClientOptions.setDefaultPort(80)
      .setDefaultHost("iss.moex.com");
    WebClient webClient = WebClient.create(vertx, webClientOptions);
    return webClient;
  }

  private Router createRestRouter(WebClient webClient) {
    Router restApi = Router.router(vertx);
    restApi.get("/rshb_bonds").handler(rc -> {
      webClient
        .get(80, "iss.moex.com", "/iss/securities.json")
        .addQueryParam("q", "РСХБ")
        .send(response -> {
          rc.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .end(processMoexBondsRequest(response.result().bodyAsJsonObject()).encodePrettily());
        });
    });
    restApi.get("/rshb_bonds/:bondId").handler(rc -> {
      String bondId = rc.request().getParam("bondId");
        webClient
          .get("/iss/securities/"+ bondId +".json")
          .send(response -> {
            rc.response()
              .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
              .end(processMoexBondDescriptionRequest(response.result().bodyAsJsonObject()));
          });
      });

    return restApi;
  }

  private String processMoexBondDescriptionRequest(JsonObject moexJson) {
    return moexJson.getJsonObject("description").getJsonArray("data")
      .stream()
      .map(arr -> (JsonArray)arr)
      .collect(
        JsonObject::new, (json, arr) -> json.put(arr.getString(1), arr.getString(2)), JsonObject::mergeIn
      )
      .encodePrettily();
  }

  private JsonObject processMoexBondsRequest(JsonObject moexJson) {
    return moexJson.getJsonObject("securities")
      .getJsonArray("data")
      .stream()
      .map(arr -> (JsonArray) arr)
      .filter(arr -> arr.getString(1).startsWith("RU"))
      .collect(
        JsonObject::new, (json, arr) -> json.put(arr.getString(1), arr.getString(2)), JsonObject::mergeIn
      );
  }
}
