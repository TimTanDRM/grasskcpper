//import com.backblaze.erasure.fec.Snmp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import kcp.highway.ChannelConfig;
import kcp.highway.KcpClient;
import kcp.highway.KcpListener;
import kcp.highway.Ukcp;
import kcp.highway.User;
import kcp.highway.erasure.fec.Snmp;
import kcp.highway.threadPool.disruptor.DisruptorExecutorPool;

import java.net.InetSocketAddress;
import java.util.Date;

//import kcp.ChannelConfig;
//import kcp.KcpClient;
//import kcp.KcpListener;
//import kcp.Ukcp;
//import threadPool.disruptor.DisruptorExecutorPool;

/**
 * Created by JinMiao
 * 2020/12/23.
 */
public class SpeedExampleClient implements KcpListener {
    public static final int ENET = 1225;
    public static final int SHAKE_CODE = 100;

    private static Ukcp kcp;

    public SpeedExampleClient() {
    }

    public static void main(String[] args) {
//        int a = 0;
//        long conv = 4380320704257763416L;
//        // (conv >> 32));
//        int le1 = 1019872888;
//        // conv & 0xFFFFFFFFL
//        int le2 = -74274728;
//
//        if(a==0){
//            int leNew1 = (int) (conv >> 32);
//            int leNew2 = (int) (conv & 0xFFFFFFFFL);
//            int b = (le1<<32);
//            int c = (le2 & 0x00000000L);
//            c
//
//
//            return;
//        }


        // 1. 模拟https已登录，拿到了IP、端口、token
        String token = "token-xxx";
        String ip = "127.0.0.1";
        Integer port = 22102;

        // 2.1 开始UDP通信与服务端握手连接，换convId。先准备netty udp通信。 todo 参数待调整
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.nodelay(true,30,2,true);
        channelConfig.setAckNoDelay(true);
//        channelConfig.setSndwnd(2048);
//        channelConfig.setRcvwnd(2048);
//        channelConfig.setMtu(20);
        channelConfig.setMtu(1400);
        channelConfig.setSndwnd(256);
        channelConfig.setRcvwnd(256);
        channelConfig.setTimeoutMillis(400 * 1000);//30s
        channelConfig.setUseConvChannel(true);
        channelConfig.setiMessageExecutorPool(new DisruptorExecutorPool(Runtime.getRuntime().availableProcessors()/2));
        //channelConfig.setFecDataShardCount(10);
        //channelConfig.setFecParityShardCount(3);
        channelConfig.setCrc32Check(false);
//        channelConfig.setWriteBufferSize(channelConfig.getMtu()*300000);

        // 2.2 初始化kcpClient，netty 建立udp连接
        KcpClient kcpClient = new KcpClient();
        SpeedExampleClient speedExampleClient = new SpeedExampleClient();
        kcpClient.init(channelConfig, speedExampleClient);

        InetSocketAddress remoteServerAddress = new InetSocketAddress(ip, port);
        InetSocketAddress localAddress = new InetSocketAddress(0);
        ChannelFuture channelFuture  = kcpClient.getBootstrap().connect(remoteServerAddress,localAddress);
        ChannelFuture sync = channelFuture.syncUninterruptibly();
        NioDatagramChannel channel = (NioDatagramChannel) sync.channel();
        localAddress = channel.localAddress();
        User user = new User(channel, remoteServerAddress, localAddress);

        // todo enet是做什么用的？用作校验的随机数
        ByteBuf packet = Unpooled.buffer(20);
        packet.writeInt(SHAKE_CODE);
        // data.readUnsignedInt(); // Empty
        // data.readUnsignedInt(); // Empty
        packet.writeInt(0);
        packet.writeInt(0);
        packet.writeInt(ENET);
        packet.writeInt(0);
        // 3.客户端发送握手信息
        Ukcp.UDPSend(packet,user);

        while(kcp == null){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("等待kcp连接");
        }
        System.out.println("Conv from main = " + kcp.getConv());
        System.out.println("Time = " + new Date());

        // 发送后需要手动调用 {@link ByteBuf#release()}
        byte[] bytes = {1,2,3};
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        kcp.write(buf);
        buf.release();


        for(;;){
            ByteBuf testServerReceivePacket = Unpooled.buffer(16);
            testServerReceivePacket.writeInt(2222);
            testServerReceivePacket.writeInt(5555);
            testServerReceivePacket.writeLong(800000L);
            kcp.write(testServerReceivePacket);
        }



//        kcpClient.
//        DatagramPacket datagramPacket = new DatagramPacket(packet,user.getRemoteAddress(), user.getLocalAddress());
//        // Send
//        user.getChannel().writeAndFlush(datagramPacket);

//        channelConfig.setConv(KcpClient.FIXED_CONV_ID);
//        kcpClient.connect(user, channelConfig, speedExampleClient);
    }

    @Override
    public void onConnected(Ukcp ukcp) {
        // 握手之后
        kcp = ukcp;
        System.out.println("client onConnected - 客户端已连接，开始通信。conv=" + ukcp.getConv());

    }

    @Override
    public void handleReceive(ByteBuf byteBuf, Ukcp ukcp) {
        ByteBuf bb = byteBuf;
        System.out.println("client handleReceive - 客户端接收数据" + ukcp.getConv());
    }

    @Override
    public void handleException(Throwable ex, Ukcp kcp)
    {
        System.out.println("client handleException - 客户端处理异常" + kcp.getConv());
    }

    @Override
    public void handleClose(Ukcp kcp) {
        System.out.println("client handleClose - 客户端关闭连接" + kcp.getConv());
        System.out.println("Time =-= " + new Date());
    }
}
