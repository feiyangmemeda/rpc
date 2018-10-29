import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


/**
 * @author feiyang.d
 * @date 2018/10/26
 */
public class RPCProxy {

    @SuppressWarnings("unchecked")
    public static <T> T create(final Object target) {
        // 生成代理对象
        return (T) Proxy.newProxyInstance(target.getClass().getClassLoader(), target.getClass().getInterfaces(), new InvocationHandler() {

            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // 组装message
                Message message = new Message();
                message.setClassName(target.getClass().getName());
                message.setMethodName(method.getName());
                message.setObjects(args);
                message.setTypes(method.getParameterTypes());

                final ResultHandler resultHandler = new ResultHandler();
                EventLoopGroup group = new NioEventLoopGroup();
                try {
                    Bootstrap b = new Bootstrap();
                    b.group(group)
                            .channel(NioSocketChannel.class)
                            .option(ChannelOption.TCP_NODELAY, true)
                            .handler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                public void initChannel(SocketChannel ch) throws Exception {
                                    ChannelPipeline pipeline = ch.pipeline();
                                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                                    pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                                    pipeline.addLast("encoder", new ObjectEncoder());
                                    pipeline.addLast("decoder", new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(null)));
                                    // 注册自定义handler
                                    pipeline.addLast("handler", resultHandler);
                                }
                            });
                    // 客户端通过IP+端口号去访问netty服务端
                    ChannelFuture future = b.connect("localhost", 8080).sync();
                    // 向服务器发送message
                    future.channel().writeAndFlush(message).sync();
                    future.channel().closeFuture().sync();
                } finally {
                    group.shutdownGracefully();
                }
                // 从handler中拿到服务端的返回值
                return resultHandler.getResponse();
            }
        });
    }

}
