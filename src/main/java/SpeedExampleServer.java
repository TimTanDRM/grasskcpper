//import com.backblaze.erasure.fec.Snmp;
import io.netty.buffer.ByteBuf;
import kcp.highway.ChannelConfig;
import kcp.highway.KcpListener;
import kcp.highway.KcpServer;
import kcp.highway.Ukcp;
import kcp.highway.erasure.fec.Snmp;
import kcp.highway.threadPool.disruptor.DisruptorExecutorPool;

import java.net.InetSocketAddress;
//import kcp.ChannelConfig;
//import kcp.KcpListener;
//import kcp.KcpServer;
//import kcp.Ukcp;
//import threadPool.disruptor.DisruptorExecutorPool;

/**
 * 测试吞吐量
 * Created by JinMiao
 * 2020/12/23.
 */
public class SpeedExampleServer implements KcpListener {
    public static void main(String[] args) {

        SpeedExampleServer speedExampleServer = new SpeedExampleServer();
        ChannelConfig channelConfig = new ChannelConfig();
//        nodelay ：是否启用 nodelay模式，0不启用；1启用。
//        interval ：协议内部工作的 interval，单位毫秒，比如 10ms或者 20ms
//        resend ：快速重传模式，默认0关闭，可以设置2（2次ACK跨越将会直接重传）
//        nc ：是否关闭流控，默认是0代表不关闭，1代表关闭。
//        普通模式： ikcp_nodelay(kcp, 0, 40, 0, 0);
//        极速模式： ikcp_nodelay(kcp, 1, 10, 2, 1);
        channelConfig.nodelay(true,20,2,true);
        channelConfig.setMtu(1400);
        channelConfig.setSndwnd(256);
        channelConfig.setRcvwnd(256);
        channelConfig.setTimeoutMillis(30 * 1000);//30s
        channelConfig.setUseConvChannel(true);
        channelConfig.setAckNoDelay(false);

//        channelConfig.setSndwnd(512);
//        channelConfig.setRcvwnd(512);
//        channelConfig.setMtu(512);
//        channelConfig.setiMessageExecutorPool(new DisruptorExecutorPool(Runtime.getRuntime().availableProcessors()/2));
//        //channelConfig.setFecDataShardCount(10);
//        //channelConfig.setFecParityShardCount(3);
//        channelConfig.setAckNoDelay(true);
//        channelConfig.setTimeoutMillis(5000);
//        channelConfig.setUseConvChannel(true);
//        channelConfig.setCrc32Check(false);
        KcpServer kcpServer = new KcpServer();
        kcpServer.init(speedExampleServer,channelConfig,getAdapterInetSocketAddress());
    }
	private static InetSocketAddress getAdapterInetSocketAddress() {
		InetSocketAddress inetSocketAddress = new InetSocketAddress(22102);
		return inetSocketAddress;
	}

    long start = System.currentTimeMillis();


    long inBytes = 0;

    @Override
    public void handleReceive(ByteBuf buf, Ukcp kcp) {
        inBytes+=buf.readableBytes();
        long now =System.currentTimeMillis();
        if(now-start>=1000){
            System.out.println("耗时 :" +(now-start) +" 接收数据: " +(Snmp.snmp.InBytes.doubleValue()/1024.0/1024.0)+"MB"+" 有效数据: "+inBytes/1024.0/1024.0+" MB");
            System.out.println(Snmp.snmp.BytesReceived.doubleValue()/1024.0/1024.0);
            System.out.println(Snmp.snmp.toString());
            inBytes=0;
            Snmp.snmp = new Snmp();
            start=now;
        }
    }

    @Override
    public void handleException(Throwable ex, Ukcp kcp) {
        ex.printStackTrace();
    }

    @Override
    public void handleClose(Ukcp kcp) {
        System.out.println(Snmp.snmp.toString());
        Snmp.snmp  = new Snmp();
    }

	@Override
	public void onConnected(Ukcp ukcp) {
		System.out.println("有连接进来"+Thread.currentThread().getName()+ukcp.user().getRemoteAddress());
        System.out.println("Server onConnected. conv=" + ukcp.getConv());
    }
}
