package io.graphoenix.http.server;

import io.graphoenix.spi.dto.GraphQLRequestBody;
import io.graphoenix.spi.dto.GraphQLResult;
import io.graphoenix.spi.handler.IGraphQLOperationPipeline;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;

public class GraphqlHttpServerInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;
    private final IGraphQLOperationPipeline<GraphQLRequestBody, GraphQLResult> graphQLOperationPipeline;

    public GraphqlHttpServerInitializer(SslContext sslCtx, IGraphQLOperationPipeline<GraphQLRequestBody, GraphQLResult> graphQLOperationPipeline) {
        this.sslCtx = sslCtx;
        this.graphQLOperationPipeline = graphQLOperationPipeline;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        if (sslCtx != null) {
            p.addLast("ssl", sslCtx.newHandler(ch.alloc()));
        }
        p.addLast("decoder", new HttpRequestDecoder());
        p.addLast("encoder", new HttpResponseEncoder());
        // Uncomment the following line if you don't want to handle HttpChunks.
        p.addLast("aggregator", new HttpObjectAggregator(1024 * 1024));
        // Remove the following line if you don't want automatic content compression.
        //p.addLast(new HttpContentCompressor());
        p.addLast("handler", new GraphqlHttpServerHandler(this.graphQLOperationPipeline));
    }
}
