import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author feiyang.d
 * @date 2018/10/26
 */
public class InvokerHandler extends ChannelInboundHandlerAdapter {

    public static ConcurrentHashMap<String, Object> classMap = new ConcurrentHashMap<String, Object>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Message message = (Message) msg;
        Object clazz = null;
        if (!classMap.containsKey(message.getClassName())) {
            try {
                clazz = Class.forName(message.getClassName()).newInstance();
                classMap.put(message.getClassName(), clazz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            clazz = classMap.get(message.getClassName());
        }
        Method method = clazz.getClass().getMethod(message.getMethodName(), message.getTypes());
        Object result = method.invoke(clazz, message.getObjects());
        ctx.write(result);
        ctx.flush();
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}
