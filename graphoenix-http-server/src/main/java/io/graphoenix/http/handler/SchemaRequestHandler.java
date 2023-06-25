package io.graphoenix.http.handler;

import com.google.common.base.CaseFormat;
import io.graphoenix.core.schema.JsonSchemaManager;
import io.graphoenix.http.codec.MimeType;
import io.netty.handler.codec.http.HttpResponseStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.reactivestreams.Publisher;
import org.tinylog.Logger;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

import java.util.Objects;

import static io.graphoenix.core.utils.GraphQLResponseUtil.GRAPHQL_RESPONSE_UTIL;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

@ApplicationScoped
public class SchemaRequestHandler {

    public static final String SCHEMA_PARAM_NAME = "name";
    private final JsonSchemaManager jsonSchemaManager;

    @Inject
    public SchemaRequestHandler(JsonSchemaManager jsonSchemaManager) {
        this.jsonSchemaManager = jsonSchemaManager;
    }

    public Publisher<Void> handle(HttpServerRequest request, HttpServerResponse response) {
        return response.addHeader(CONTENT_TYPE, MimeType.Application.JSON)
                .sendString(
                        Mono.just(jsonSchemaManager.getJsonSchema(CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, Objects.requireNonNull(request.param(SCHEMA_PARAM_NAME)))))
                                .doOnSuccess(jsonString -> response.status(HttpResponseStatus.OK))
                                .onErrorResume(throwable -> {
                                            Logger.error(throwable);
                                            response.status(HttpResponseStatus.BAD_REQUEST);
                                            return Mono.just(GRAPHQL_RESPONSE_UTIL.error(throwable));
                                        }
                                )
                );
    }
}
