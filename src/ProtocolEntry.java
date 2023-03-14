import Application.DNSApplication;
import Application.PingApp;
import Application.TraceRoute;
import datalinklayer.DataLinkLayer;
import jpcap.JpcapCaptor;
import jpcap.NetworkInterface;
import jpcap.NetworkInterfaceAddress;
import jpcap.PacketReceiver;
import jpcap.packet.*;
import protocol.ARPProtocolLayer;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.HashMap;


//入口协议
public class ProtocolEntry implements PacketReceiver {
    public void receivePacket(Packet packet) {
        System.out.println(packet);
        System.out.println("Receive a packet");
    }

    public static void main(String[] args) throws IOException {
//        System.out.println(System.getProperty("java.library.path"));
        //获取网卡列表
        NetworkInterface[] devices=JpcapCaptor.getDeviceList();
        NetworkInterface device=null;

        for (int i = 0; i < devices.length; i++) {
            boolean findDevice=false;
            for(NetworkInterfaceAddress addr:devices[i].addresses){
                if(!(addr.address instanceof Inet4Address)){
                    continue;
                }
                findDevice=true;
                break;
            }
            if(findDevice){
                device=devices[i];
            }
        }
        System.out.println("Open captor on device" + device.name);//找到网卡设备

        JpcapCaptor jpcap = JpcapCaptor.openDevice(device, 2000, true, 20);
        DataLinkLayer linkLayerInstance = DataLinkLayer.getInstance();//获取数据链路层实例
        linkLayerInstance.initWithOpenDevice(device);//

        // 要发送目标IP
        String ip = "61.232.206.100";  // 网关
        InetAddress address = InetAddress.getByName(ip);
        DNSApplication app=new DNSApplication(address.getAddress(),"www.baidu.com");
        System.out.println("ping");
        app.queryDomain();

//
//        ARPProtocolLayer arpProtocolLayer = new ARPProtocolLayer();
//        HashMap<String, Object> headerInfo = new HashMap<String, Object>();
//        InetAddress address = InetAddress.getByName(ip);
//        headerInfo.put("sender_ip", address.getAddress());
//        // 传入IP参数
//        byte[] header = arpProtocolLayer.createHeader(headerInfo);//根据上层信息创建了arp协议请求
//
//        // 广播，以太网目的地址
//        byte[] broadcast=new byte[]{(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255};
//
//        DataLinkLayer dataLinkLayer = DataLinkLayer.getInstance();
//        // 发送数据到以太网，以太网帧类型2054，即0x0806
//        dataLinkLayer.sendData(header, broadcast, (short)2054);
//
//        // 监听网卡
        jpcap.loopPacket(-1, (jpcap.PacketReceiver) linkLayerInstance);
    }
}
