package cn.tim.xchat.netty;

import cn.tim.xchat.core.model.DataContentSerializer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

public class WSServerInitializer extends ChannelInitializer<SocketChannel> {

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		
		// websocket 基于HTTP协议，需要HTTP编解码器
		pipeline.addLast(new HttpServerCodec());

		/*
		 * websocket 服务器处理的协议，用于指定给客户端连接访问的路由 : /ws
		 * 本handler会帮你处理一些繁重的复杂的事
		 * 会帮你处理握手动作： handshaking（close, ping, pong） ping + pong = 心跳
		 * 对于websocket来讲，都是以frames进行传输的，不同的数据类型对应的frames也不同
		 */
		pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));

		// 2个字节的校验解码
		pipeline.addLast(new ProtobufVarint32FrameDecoder());
		pipeline.addLast(new ProtobufDecoder(DataContentSerializer.DataContent.getDefaultInstance()));
		// 对写大数据流的支持
		pipeline.addLast(new ChunkedWriteHandler());
		// 对httpMessage进行聚合，聚合成FullHttpRequest或FullHttpResponse
		pipeline.addLast(new HttpObjectAggregator(1024 * 128));

		// 针对客户端，如果在1分钟时没有向服务端发送读写心跳(ALL)，则主动断开
		// 如果是读空闲或者写空闲，不处理
		pipeline.addLast(new IdleStateHandler(8, 10, 12));
		// 自定义的空闲状态检测
		pipeline.addLast(new HeartBeatHandler());
		
		// 聊天消息处理的Handler
		pipeline.addLast(new ChatHandler());
		pipeline.addLast(new ChatHandlerByProtobuf());
		// 2个字节的校验解码
		pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
		//Google Protocol Buffers编码器
		pipeline.addLast(new ProtobufEncoder());
	}
}