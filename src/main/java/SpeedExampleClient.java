//import com.backblaze.erasure.fec.Snmp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import kcp.highway.ChannelConfig;
import kcp.highway.KcpClient;
import kcp.highway.KcpListener;
import kcp.highway.Ukcp;
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


    public SpeedExampleClient() {
    }

    public static void main(String[] args) {
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.nodelay(true,30,2,true);
        channelConfig.setAckNoDelay(true);


//        channelConfig.setSndwnd(2048);
//        channelConfig.setRcvwnd(2048);
//        channelConfig.setMtu(20);
        channelConfig.setMtu(1400);
        channelConfig.setSndwnd(256);
        channelConfig.setRcvwnd(256);
        channelConfig.setTimeoutMillis(30 * 1000);//30s
        channelConfig.setUseConvChannel(true);



//        channelConfig.setConv(55L);
        channelConfig.setiMessageExecutorPool(new DisruptorExecutorPool(Runtime.getRuntime().availableProcessors()/2));
        //channelConfig.setFecDataShardCount(10);
        //channelConfig.setFecParityShardCount(3);
        channelConfig.setCrc32Check(false);
//        channelConfig.setWriteBufferSize(channelConfig.getMtu()*300000);

        KcpClient kcpClient = new KcpClient();
        kcpClient.init(channelConfig);

        SpeedExampleClient speedExampleClient = new SpeedExampleClient();
        kcpClient.connect(new InetSocketAddress("127.0.0.1",22102),channelConfig,speedExampleClient);

    }
    private static final int messageSize = 2048;
    private long start = System.currentTimeMillis();

    @Override
    public void onConnected(Ukcp ukcp) {
//        new Thread(() -> {
//            for(;;){
//                long now =System.currentTimeMillis();
//                if(now-start>=1000){
//                    System.out.println("耗时 :" +(now-start) +" 发送数据: " +(Snmp.snmp.OutBytes.doubleValue()/1024.0/1024.0)+"MB"+" 有效数据: "+ Snmp.snmp.BytesSent.doubleValue()/1024.0/1024.0+" MB");
//                    System.out.println(Snmp.snmp.toString());
//                    Snmp.snmp = new Snmp();
//                    start=now;
//                }
//                ByteBuf byteBuf1 = ByteBufAllocator.DEFAULT.buffer(20);
//                byteBuf1.writeBytes(new byte[20]);
//                if(!ukcp.write(byteBuf1)){
//                    try {
//                        Thread.sleep(1);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//                byteBuf1.release();
//            }
//        }).start();

//        new Thread(() -> {
//            long now = System.currentTimeMillis();
//            if (now - start >= 1000) {
//                System.out.println("耗时 :" + (now - start) + " 发送数据: " + (Snmp.snmp.OutBytes.doubleValue() / 1024.0 / 1024.0) + "MB" + " 有效数据: " + Snmp.snmp.BytesSent.doubleValue() / 1024.0 / 1024.0 + " MB");
//                System.out.println(Snmp.snmp.toString());
//                Snmp.snmp = new Snmp();
//                start = now;
//            }
//            ByteBuf byteBuf1 = ByteBufAllocator.DEFAULT.buffer(21,100);
//            byteBuf1.writeBytes(new byte[4]);
//            if (!ukcp.write(byteBuf1)) {
//                try {
//                    Thread.sleep(1);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            byteBuf1.release();
//        }).start();

        for (int i = 0; i < 1; i++) {
            ByteBuf byteBuf = UnpooledByteBufAllocator.DEFAULT.buffer(1024);
            int shakeHandsFlag = 255;
            byteBuf.writeInt(shakeHandsFlag);
            ukcp.write(byteBuf);
            byteBuf.release();
        }

    }

    @Override
    public void handleReceive(ByteBuf byteBuf, Ukcp ukcp) {
        System.out.println(byteBuf);
    }

    @Override
    public void handleException(Throwable ex, Ukcp kcp)
    {
        ex.printStackTrace();
    }

    @Override
    public void handleClose(Ukcp kcp) {
    }
}
