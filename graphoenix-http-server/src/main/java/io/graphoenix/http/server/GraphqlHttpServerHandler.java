package io.graphoenix.http.server;

import com.google.common.net.MediaType;
import com.google.gson.Gson;
import io.graphoenix.http.server.config.ServerConfiguration;
import io.graphoenix.http.server.dto.graphql.GraphQLRequestBody;
import io.graphoenix.http.server.dto.graphql.GraphQLResult;
import io.graphoenix.http.server.handler.RequestHandler;
import io.graphoenix.http.server.handler.RequestHandlerFactory;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class GraphqlHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final ServerConfiguration serverConfiguration;

    private static final Logger log = LoggerFactory.getLogger(GraphqlHttpServer.class);

    private static final String FAVICON_ICO = "/favicon.ico";
    private static final AsciiString CONNECTION = AsciiString.cached("Connection");
    private static final AsciiString KEEP_ALIVE = AsciiString.cached("keep-alive");
    private static final AsciiString CONTENT_TYPE = AsciiString.cached("Content-Type");
    private static final AsciiString CONTENT_LENGTH = AsciiString.cached("Content-Length");

    public GraphqlHttpServerHandler() {
        this.serverConfiguration = new ServerConfiguration();
    }

    public GraphqlHttpServerHandler(ServerConfiguration serverConfiguration) {
        this.serverConfiguration = serverConfiguration;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        log.info("Handle http request:{}", request);
        String uri = request.uri();
        if (uri.equals(FAVICON_ICO)) {
            return;
        }
        RequestHandler requestHandler = RequestHandlerFactory.create(request.method());
        GraphQLRequestBody requestBody;
        GraphQLResult graphQLResult = new GraphQLResult();
        FullHttpResponse response;
        try {
            requestBody = requestHandler.handle(request);


            //TODO
            response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(new Gson().toJson(graphQLResult).getBytes(StandardCharsets.UTF_8)));
            response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(CONTENT_TYPE, MediaType.JSON_UTF_8);

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            //TODO
            response = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR, Unpooled.wrappedBuffer(new Gson().toJson(graphQLResult).getBytes(StandardCharsets.UTF_8)));
            response.headers().set(CONTENT_TYPE, MediaType.JSON_UTF_8);
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
