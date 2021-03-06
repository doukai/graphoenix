package io.graphoenix.http.server;

import io.graphoenix.common.pipeline.GraphQLDataFetcher;
import io.graphoenix.common.pipeline.operation.OperationPipeline;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;

public class GraphqlHttpServerInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;

    private final GraphQLDataFetcher dataFetcher;

    public GraphqlHttpServerInitializer(SslContext sslCtx, GraphQLDataFetcher dataFetcher) {
        this.sslCtx = sslCtx;
        this.dataFetcher = dataFetcher;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        CorsConfig corsConfig =
                CorsConfigBuilder
                        .forAnyOrigin()
                        .allowedRequestHeaders(HttpHeaderNames.CONTENT_TYPE)
                        .allowedRequestMethods(HttpMethod.GET)
                        .allowedRequestMethods(HttpMethod.POST)
                        .build();
        ChannelPipeline p = ch.pipeline();
        if (sslCtx != null) {
            p.addLast("ssl", sslCtx.newHandler(ch.alloc()));
        }
        p.addLast("decoder", new HttpRequestDecoder());
        p.addLast("encoder", new HttpResponseEncoder());
        // Uncomment the following line if you don't want to handle HttpChunks.
        p.addLast("aggregator", new HttpObjectAggregator(1024 * 1024));
        // Remove the following line if you don't want automatic content compression.
//        p.addLast("compressor",new HttpContentCompressor());
        p.addLast("chunked", new ChunkedWriteHandler());
        p.addLast("cors", new CorsHandler(corsConfig));
        p.addLast("handler", new GraphqlHttpServerHandler(this.dataFetcher));
    }
}
