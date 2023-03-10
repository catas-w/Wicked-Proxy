package com.catas.wicked.proxy.proxy;

import com.catas.wicked.proxy.cert.CertPool;
import com.catas.wicked.proxy.cert.CertService;
import com.catas.wicked.proxy.config.ProxyConfig;
import com.catas.wicked.proxy.proxy.handler.ProxyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;


@Slf4j
@Component
public class ProxyServer {

    @Autowired
    private ProxyConfig proxyConfig;
    @Autowired
    private CertService certService;
    @Autowired
    private CertPool certPool;

    @Autowired
    private ResourceLoader resourceLoader;

    public ProxyServer() {
    }

    public void start() {
        NioEventLoopGroup workGroup = new NioEventLoopGroup(2);
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<Channel>() {

                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            channel.pipeline().addLast("httpCodec", new HttpServerCodec());
                            channel.pipeline().addLast("serverHandle", new ProxyServerHandler(
                                    proxyConfig, certService, certPool
                            ));
                        }
                    });
            ChannelFuture channelFuture = bootstrap.bind(proxyConfig.getPort()).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Error occured in proxy server: ", e);
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    @PostConstruct
    private void init() {
        SslContextBuilder contextBuilder = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        X509Certificate caCert;
        PrivateKey caPriKey;
        try {
            proxyConfig.setClientSslCtx(contextBuilder.build());
            caCert = certService.loadCert((new ClassPathResource("/ca.crt").getInputStream()));
            caPriKey = certService.loadPriKey((new ClassPathResource("/ca_private.der").getInputStream()));
            //??????CA?????????????????????
            proxyConfig.setIssuer(certService.getSubject(caCert));
            //??????CA??????????????????(server?????????????????????CA????????????????????????????????????????????????)
            proxyConfig.setCaNotBefore(caCert.getNotBefore());
            proxyConfig.setCaNotAfter(caCert.getNotAfter());
            //CA????????????????????????????????????SSL????????????
            proxyConfig.setCaPriKey(caPriKey);
            //???????????????????????????????????????SSL??????????????????
            KeyPair keyPair = certService.genKeyPair();
            proxyConfig.setServerPriKey(keyPair.getPrivate());
            proxyConfig.setServerPubKey(keyPair.getPublic());
        } catch (Exception e) {
            e.printStackTrace();
            proxyConfig.setHandleSsl(false);
        }
    }
}
