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
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                    .childOption(ChannelOption.TCP_NODELAY, serverConfiguration.isTcpNoDelay())
                    // 是否开启 TCP 底层心跳机制
//                    .childOption(ChannelOption.SO_KEEPALIVE, serverConfiguration.isSoKeepAlive())
                    //表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
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
