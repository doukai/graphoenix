package io.graphoenix.http.server;

import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

public class Application {

    public void run() {

        HttpServer httpServer =
                HttpServer.create()
                        .host("localhost")
                        .port(8080)
                        .route(routes ->
                                routes.get("/hello",        //<1>
                                        (request, response) -> response.sendString(Mono.just("Hello World!")))
                                        .post("/echo",        //<2>
                                                (request, response) -> response.send(request.receive().retain()))
                                        .get("/path/{param}", //<3>
                                                (request, response) -> response.sendString(Mono.just(request.param("param"))))
                                        .ws("/ws",            //<4>
                                                (wsInbound, wsOutbound) -> wsOutbound.send(wsInbound.receive().retain())));

        httpServer.warmup() //<1>
                .block();

        DisposableServer server = httpServer.bindNow();

        server.onDispose()
                .block();
    }
}
