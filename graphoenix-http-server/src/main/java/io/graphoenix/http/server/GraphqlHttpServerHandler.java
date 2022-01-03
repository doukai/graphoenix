package io.graphoenix.http.server;

import com.google.common.net.MediaType;
import io.graphoenix.core.error.GraphQLProblem;
import io.graphoenix.core.manager.GraphQLOperationRouter;
import io.graphoenix.http.handler.RequestHandler;
import io.graphoenix.http.handler.RequestHandlerFactory;
import io.graphoenix.spi.dto.GraphQLRequest;
import io.graphoenix.spi.dto.GraphQLResponse;
import io.graphoenix.spi.dto.type.OperationType;
import io.graphoenix.spi.handler.OperationHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.AsciiString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;

import static io.graphoenix.core.utils.GraphQLResponseUtil.GRAPHQL_RESPONSE_UTIL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class GraphqlHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(GraphqlHttpServer.class);

    private static final String FAVICON_ICO = "/favicon.ico";
    private static final AsciiString CONNECTION = AsciiString.cached("Connection");
    private static final AsciiString KEEP_ALIVE = AsciiString.cached("keep-alive");
    private static final AsciiString CONTENT_TYPE = AsciiString.cached("Content-Type");
    private static final AsciiString CONTENT_LENGTH = AsciiString.cached("Content-Length");

    private final OperationHandler operationHandler;
    private final GraphQLOperationRouter graphQLOperationRouter;

    @Inject
    public GraphqlHttpServerHandler(GraphQLOperationRouter graphQLOperationRouter, OperationHandler operationHandler) {
        this.graphQLOperationRouter = graphQLOperationRouter;
        this.operationHandler = operationHandler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {

        log.info("Handle http request:{}", request);
        String uri = request.uri();
        if (uri.equals(FAVICON_ICO)) {
            return;
        }
        RequestHandler requestHandler = RequestHandlerFactory.create(request.method());
        GraphQLRequest requestBody;
        GraphQLResponse graphQLResponse = null;
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);

        try {
            requestBody = requestHandler.handle(request);
            log.info("Handle http query:{}", requestBody.getQuery());
            OperationType type = graphQLOperationRouter.getType(requestBody.getQuery());

            String jsonResult = null;
            switch (type) {
                case QUERY:
                    jsonResult = ((Mono<String>) operationHandler.query(requestBody.getQuery(), requestBody.getVariables())).block();
                    break;
                case MUTATION:
                    jsonResult = ((Mono<String>) operationHandler.mutation(requestBody.getQuery(), requestBody.getVariables())).block();
                    break;
            }
            response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(GRAPHQL_RESPONSE_UTIL.fromJson(jsonResult).getBytes(StandardCharsets.UTF_8)));
            response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(CONTENT_TYPE, MediaType.JSON_UTF_8);
        } catch (GraphQLProblem e) {
            e.printStackTrace();
            response = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST, Unpooled.wrappedBuffer(e.toString().getBytes(StandardCharsets.UTF_8)));
            response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(CONTENT_TYPE, MediaType.JSON_UTF_8);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (!keepAlive) {
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(CONNECTION, KEEP_ALIVE);
            ctx.write(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }
}
