package io.graphoenix.http.server;

import io.graphoenix.http.config.HttpServerConfig;
import io.graphoenix.http.config.NettyConfig;
import io.graphoenix.spi.handler.BootstrapHandler;
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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class GraphqlHttpServer {

    private static final Logger log = LoggerFactory.getLogger(GraphqlHttpServer.class);

    private final NettyConfig nettyConfig;

    private final HttpServerConfig httpServerConfig;

    private final GraphqlHttpServerInitializer graphqlHttpServerInitializer;

    private final BootstrapHandler bootstrapHandler;

    @Inject
    public GraphqlHttpServer(NettyConfig nettyConfig,
                             HttpServerConfig httpServerConfig,
                             GraphqlHttpServerInitializer graphqlHttpServerInitializer,
                             BootstrapHandler bootstrapHandler) {
        this.nettyConfig = nettyConfig;
        this.httpServerConfig = httpServerConfig;
        this.graphqlHttpServerInitializer = graphqlHttpServerInitializer;
        this.bootstrapHandler = bootstrapHandler;
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup;
        EventLoopGroup workerGroup;
        Class<? extends ServerSocketChannel> serverSocketChannelClass;

        // Configure the server.
        if (nettyConfig.getEpoll()) {
            bossGroup = new EpollEventLoopGroup(1);
            workerGroup = new EpollEventLoopGroup();
            serverSocketChannelClass = EpollServerSocketChannel.class;
        } else {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            serverSocketChannelClass = NioServerSocketChannel.class;
        }

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(serverSocketChannelClass)
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                    .childOption(ChannelOption.TCP_NODELAY, httpServerConfig.getTcpNoDelay())
                    // 是否开启 TCP 底层心跳机制
//                    .childOption(ChannelOption.SO_KEEPALIVE, serverConfiguration.isSoKeepAlive())
                    //表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .option(ChannelOption.SO_BACKLOG, httpServerConfig.getSoBackLog())
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(graphqlHttpServerInitializer);

            Channel channel = serverBootstrap.bind(httpServerConfig.getPort()).sync().channel();

            bootstrapHandler.bootstrap();

            log.info("Open your web browser and navigate to " +
                    (httpServerConfig.getSsl() ? "https" : "http") + "://127.0.0.1:" + httpServerConfig.getPort() + '/');

            channel.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
