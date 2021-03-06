package io.graphoenix.http.server;

import io.graphoenix.common.pipeline.GraphQLDataFetcher;
import io.graphoenix.http.server.config.NettyConfiguration;
import io.graphoenix.http.server.config.ServerConfiguration;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphqlHttpServer {

    private static final Logger log = LoggerFactory.getLogger(GraphqlHttpServer.class);

    private final NettyConfiguration nettyConfiguration;

    private final ServerConfiguration serverConfiguration;

    private final GraphQLDataFetcher dataFetcher;

    public GraphqlHttpServer(NettyConfiguration nettyConfiguration, ServerConfiguration serverConfiguration, GraphQLDataFetcher dataFetcher) {
        this.nettyConfiguration = nettyConfiguration;
        this.serverConfiguration = serverConfiguration;
        this.dataFetcher = dataFetcher;
    }

    public GraphqlHttpServer(GraphQLDataFetcher dataFetcher) {
        this(new NettyConfiguration(), new ServerConfiguration(), dataFetcher);
    }

    public void run() throws Exception {

        // Configure SSL.
        final SslContext sslCtx;
        if (serverConfiguration.isSsl()) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        EventLoopGroup bossGroup;
        EventLoopGroup workerGroup;
        Class<? extends ServerSocketChannel> serverSocketChannelClazz;

        // Configure the server.
        if (nettyConfiguration.isEpoll()) {
            bossGroup = new EpollEventLoopGroup(1);
            workerGroup = new EpollEventLoopGroup();
            serverSocketChannelClazz = EpollServerSocketChannel.class;
        } else {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            serverSocketChannelClazz = NioServerSocketChannel.class;
        }

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(serverSocketChannelClazz)
                    // TCP??????????????? Nagle ????????????????????????????????????????????????????????????????????????????????????TCP_NODELAY ??????????????????????????????????????? Nagle ?????????
                    .childOption(ChannelOption.TCP_NODELAY, serverConfiguration.isTcpNoDelay())
                    // ???????????? TCP ??????????????????
//                    .childOption(ChannelOption.SO_KEEPALIVE, serverConfiguration.isSoKeepAlive())
                    //????????????????????????????????????????????????????????????????????????????????????,????????????????????????????????????????????????????????????????????????????????????????????????
                    .option(ChannelOption.SO_BACKLOG, serverConfiguration.getSoBackLog())
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new GraphqlHttpServerInitializer(sslCtx, dataFetcher));

            Channel ch = b.bind(serverConfiguration.getPort()).sync().channel();

            log.info("Open your web browser and navigate to " +
                    (serverConfiguration.isSsl() ? "https" : "http") + "://127.0.0.1:" + serverConfiguration.getPort() + '/');

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }
}
