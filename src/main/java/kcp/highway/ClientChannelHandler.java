package kcp.highway;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import kcp.highway.threadPool.IMessageExecutor;
import kcp.highway.threadPool.IMessageExecutorPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Created by JinMiao
 * 2019-06-26.
 */
public class ClientChannelHandler extends ChannelInboundHandlerAdapter {
    static final Logger logger = LoggerFactory.getLogger(ClientChannelHandler.class);

    private final IChannelManager channelManager;

    private final ChannelConfig channelConfig;

    private final KcpListener kcpListener;

    private final IMessageExecutorPool iMessageExecutorPool;

    private final HashedWheelTimer hashedWheelTimer;

    public ClientChannelHandler(IChannelManager channelManager, ChannelConfig channelConfig, KcpListener kcpListener,
                                IMessageExecutorPool iMessageExecutorPool, HashedWheelTimer hashedWheelTimer) {
        this.channelManager = channelManager;
        this.channelConfig = channelConfig;
        this.kcpListener = kcpListener;
        this.iMessageExecutorPool = iMessageExecutorPool;
        this.hashedWheelTimer = hashedWheelTimer;
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("",cause);
        //SocketAddress socketAddress = ctx.channel().localAddress();
        //Ukcp ukcp = ukcpMap.get(socketAddress);
        //ukcp.getKcpListener().handleException(cause,ukcp);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object object) {
        // 不要影响全局的channelConfig，单独初始化一个。
        final ChannelConfig channelConfig = this.channelConfig;
        DatagramPacket msg = (DatagramPacket) object;
        ByteBuf byteBuf = msg.content();
        User user = new User(ctx.channel(), msg.sender(), msg.recipient());
        if(byteBuf.readableBytes()==20){
            // 前置校验判断,取出convId，基于UDP建立Kcp连接
            int fixedPrefix = byteBuf.readInt();
            if(fixedPrefix != 32345){
                // todo 校验不过，抛异常？
                return;
            }
//            int convIntLEFirst = byteBuf.readIntLE();
//            int convIntLELast = byteBuf.readIntLE();
            long convId = byteBuf.readLong();
            int enet = byteBuf.readInt();
            int fixedSuffix = byteBuf.readInt();
            if(enet !=1225 || fixedSuffix != 340870469){
                return;
            }
            channelConfig.setConv(convId);
            doConnect(user, channelConfig);
        }
        Ukcp ukcp = this.channelManager.get(msg);
        if(ukcp!=null){
            ukcp.read(msg.content());
        }
    }

    public Ukcp doConnect(User user, ChannelConfig channelConfig) {
        IMessageExecutor iMessageExecutor = iMessageExecutorPool.getIMessageExecutor();
        KcpOutput kcpOutput = new KcpOutPutImp();

        Ukcp ukcp = new Ukcp(kcpOutput, kcpListener, iMessageExecutor, channelConfig,channelManager);
        ukcp.user(user);

        channelManager.New(user.getLocalAddress(),ukcp,null);
        iMessageExecutor.execute(() -> {
            try {
                ukcp.getKcpListener().onConnected(ukcp);
            }catch (Throwable throwable){
                ukcp.getKcpListener().handleException(throwable,ukcp);
            }
        });

        ScheduleTask scheduleTask = new ScheduleTask(iMessageExecutor, ukcp,hashedWheelTimer);
        hashedWheelTimer.newTimeout(scheduleTask,ukcp.getInterval(), TimeUnit.MILLISECONDS);
        return ukcp;
    }
}
