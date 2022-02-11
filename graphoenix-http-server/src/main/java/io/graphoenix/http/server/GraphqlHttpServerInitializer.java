package io.graphoenix.http.server;

import io.graphoenix.http.config.HttpServerConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.stream.ChunkedWriteHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

@ApplicationScoped
public class GraphqlHttpServerInitializer extends ChannelInitializer<SocketChannel> {

    private SslContext sslCtx;

    private final Provider<GraphqlHttpServerHandler> httpServerHandler;

    @Inject
    public GraphqlHttpServerInitializer(HttpServerConfig httpServerConfig,
                                        Provider<GraphqlHttpServerHandler> httpServerHandler) {
        // Configure SSL.
        if (httpServerConfig.getSsl()) {
            try {
                SelfSignedCertificate ssc = new SelfSignedCertificate();
                this.sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            } catch (CertificateException | SSLException e) {
                e.printStackTrace();
            }
        } else {
            this.sslCtx = null;
        }
        this.httpServerHandler = httpServerHandler;
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
        p.addLast("httpServerHandler", httpServerHandler.get());
    }
}
