package com.catas.wicked.server.handler.client;

import com.catas.wicked.common.bean.ProxyRequestInfo;
import com.catas.wicked.common.constant.ProxyConstant;
import com.catas.wicked.common.util.WebUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ClientProcessHandler extends ChannelInboundHandlerAdapter {

    private Channel clientChannel;

    private final AttributeKey<ProxyRequestInfo> requestInfoAttributeKey =
            AttributeKey.valueOf(ProxyConstant.REQUEST_INFO);

    public ClientProcessHandler(Channel clientChannel) {
        this.clientChannel = clientChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!clientChannel.isOpen()) {
            ReferenceCountUtil.release(msg);
            return;
        }

        // refresh timing & size
        ProxyRequestInfo requestInfo = ctx.channel().attr(requestInfoAttributeKey).get();
        if (requestInfo != null) {
            requestInfo.updateResponseTime();

            if (msg instanceof HttpResponse httpResponse) {
                requestInfo.updateRespSize(WebUtils.estimateSize(httpResponse));
            } else if (msg instanceof HttpContent httpContent) {
                requestInfo.updateRespSize(httpContent.content().readableBytes());
            } else {
                try {
                    ByteBuf cont = (ByteBuf) msg;
                    requestInfo.updateRespSize(cont.readableBytes());
                } catch (Exception e) {
                    log.warn("Unable to catch request size.", e);
                }
            }
        }

        if (msg instanceof HttpResponse origin) {
            // Bug-fix: HttpAggregator removes Transfer-Encoding header
            DefaultHttpResponse copiedResp = new DefaultHttpResponse(
                    origin.protocolVersion(), origin.status(), origin.headers().copy());
            clientChannel.writeAndFlush(msg);
            ctx.fireChannelRead(copiedResp);
        } else {
            ReferenceCountUtil.retain(msg);
            clientChannel.writeAndFlush(msg);
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().close();
        clientChannel.close();
        // TODO: log error
        log.error("Error occurred in Proxy client.", cause);
    }
}
