package com.catas.wicked.server.handler.client;


import com.catas.wicked.common.bean.ProxyRequestInfo;
import com.catas.wicked.common.bean.ResponseMessage;
import com.catas.wicked.common.config.ApplicationConfig;
import com.catas.wicked.common.constant.ProxyConstant;
import com.catas.wicked.common.pipeline.MessageQueue;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端响应记录器
 * record responses
 */
@Slf4j
public class ClientPostRecorder extends ChannelInboundHandlerAdapter {

    private ApplicationConfig appConfig;
    private MessageQueue messageQueue;
    private final AttributeKey<ProxyRequestInfo> requestInfoKey = AttributeKey.valueOf(ProxyConstant.REQUEST_INFO);

    public ClientPostRecorder(ApplicationConfig applicationConfig, MessageQueue messageQueue) {
        this.appConfig = applicationConfig;
        this.messageQueue = messageQueue;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ProxyRequestInfo requestInfo = ctx.channel().attr(requestInfoKey).get();
        if (!requestInfo.isRecording()) {
            ReferenceCountUtil.release(msg);
            return;
        }
        if (msg instanceof FullHttpResponse response) {
            try {
                recordHttpResponse(ctx, response, requestInfo);
            } catch (Exception e) {
                log.error("Error in recording response.", e);
            } finally {
                response.release();
            }
        } else {
            log.error("????????");
            ReferenceCountUtil.release(msg);
        }
    }

    private void recordHttpResponse(ChannelHandlerContext ctx, FullHttpResponse resp, ProxyRequestInfo requestInfo) {
        HttpHeaders headers = resp.headers();
        HttpResponseStatus status = resp.status();

        ResponseMessage responseMessage = new ResponseMessage();
        Map<String, String> map = new HashMap<>();
        headers.entries().forEach(entry -> {
            map.put(entry.getKey(), entry.getValue());
        });

        responseMessage.setStatus(status.code());
        responseMessage.setHeaders(map);
        ByteBuf content = resp.content();
        if (content.isReadable()) {
            if (content.hasArray()) {
                responseMessage.setContent(content.array());
            } else {
                byte[] bytes = new byte[content.readableBytes()];
                content.getBytes(content.readerIndex(), bytes);
                responseMessage.setContent(bytes);
            }
        }

        responseMessage.setRequestId(requestInfo.getRequestId());
        responseMessage.setStartTime(requestInfo.getResponseStartTime());
        responseMessage.setEndTime(requestInfo.getResponseEndTime());
        messageQueue.pushMsg(responseMessage);
        log.info("<<<< Response received: {} <<<<", requestInfo.getRequestId());
    }
}