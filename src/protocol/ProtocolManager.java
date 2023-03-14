package protocol;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import Application.Application;
import Application.ApplicationManager;
import Application.IApplication;
import datalinklayer.DataLinkLayer;
import jpcap.PacketReceiver;
import jpcap.packet.EthernetPacket;
import jpcap.packet.IPPacket;
import jpcap.packet.Packet;

public class ProtocolManager implements PacketReceiver{
	private static ProtocolManager instance = null;
	private static ARPProtocolLayer arpLayer = null;
	private static DataLinkLayer dataLinkInstance = null;
	private static HashMap<String , byte[] > ipToMacTable = null;
	private static HashMap<String, byte[]> dataWaitToSend = null;
	private static ArrayList<Application> icmpPacketReceiverList = null;
	
	private static InetAddress routerAddress = null;
	
	private static byte[] broadcast=new byte[]{(byte)255,(byte)255,(byte)255,(byte)255,(byte)255,(byte)255};
	private ProtocolManager() {}
	public static ProtocolManager getInstance() {
		if (instance == null) {
			instance = new ProtocolManager();
			dataLinkInstance = DataLinkLayer.getInstance();
			ipToMacTable = new HashMap<String, byte[]>();
			
			dataWaitToSend = new HashMap<String, byte[]>();
			dataLinkInstance.registerPacketReceiver(instance);
			arpLayer = new ARPProtocolLayer();
			
			icmpPacketReceiverList = new ArrayList<Application>();
			
			//这里写死路由器IP
			try {
				byte[] selfIP = InetAddress.getByName("192.168.2.243").getAddress();
				byte[] selfMac = new byte[] {0x3c, 0x15, (byte)0xc2, (byte) 0xbb, 0x4b, 0x5c};
				ipToMacTable.put(Arrays.toString(selfIP), selfMac);
				byte[] phoneIP = InetAddress.getByName("192.168.2.127").getAddress();
				byte[] phoneMac = new byte[] {(byte) 0xa8, (byte) 0xbe, (byte)0x27, (byte) 0xe4, 0x51, (byte) 0x98};
				ipToMacTable.put(Arrays.toString(phoneIP), phoneMac);
				byte[] httpServerIP = InetAddress.getByName("124.225.118.89").getAddress();
				byte[] httpServerMac = new byte[] {(byte) 0x98, (byte) 0xbb, (byte) 0x99, 0x13, 0x6a, 0x08};
				ipToMacTable.put(Arrays.toString(httpServerIP), httpServerMac);
				routerAddress = InetAddress.getByName("192.168.2.1");
				instance.prepareDestMac(routerAddress.getAddress());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return instance;
	}
	
	//所有想接收icmp数据包的应用都要注册自己
	public void registToReceiveICMPPacket(Application receiver) {
		if (icmpPacketReceiverList.contains(receiver) != true) {
			icmpPacketReceiverList.add(receiver);
		}
	}
	
	private void prepareDestMac(byte[] ip) throws Exception {
		HashMap<String, Object> headerInfo = new HashMap<String, Object>();
		headerInfo.put("sender_ip", ip);
		byte[] arpRequest = arpLayer.createHeader(headerInfo);
		if (arpRequest == null) {
			throw new Exception("Get mac adress header fail");
		}
		for (int i = 0; i < 5; i++) {
			dataLinkInstance.sendData(arpRequest, broadcast, EthernetPacket.ETHERTYPE_ARP);	
		}
		
	}
	
	private byte[] getDestMac(byte[] ip) {
		return ipToMacTable.get(Arrays.toString(ip));
	}
	
    public IProtocol getProtocol(String name) {
    	switch (name.toLowerCase()) {
    	case "icmp":
    		return new ICMPProtocolLayer();
    	case "ip":
    		return new IPProtocolLayer();
    	case "udp":
    		return new UDPProtocolLayer();
    	case "tcp":
    		return new TCPProtocolLayer();
    	}
    	
    	return null;
    }
    
    public void sendData(byte[] data, byte[] ip) throws Exception {
    	/*
    	 * 发送数据前先检查给定路由器的mac地址是否存在，如果没有则先让ARP协议获取mac地址
    	 */
    	byte[] mac = getDestMac(ip);
    	if (mac == null) {
    	     //如果没有路由器mac，那么先通过ARP协议获取路由器mac
    		prepareDestMac(ip);
    		
    		//将要发送的数据存起，等待mac地址返回后再发送
    		dataWaitToSend.put(Arrays.toString(ip), data);
    	} else {
    		//如果mac地址已经存在则直接发送数据
    		dataLinkInstance.sendData(data, mac, EthernetPacket.ETHERTYPE_IP);
    	}
    }
    
    //增加一个广播IP数据包接口
    public void broadcastData(byte[] data ) {
    	dataLinkInstance.sendData(data, broadcast,EthernetPacket.ETHERTYPE_IP);
    }
    
	@Override
	public void receivePacket(Packet packet) {
		if (packet == null) {
			return;
		}
		
		//确保收到数据包是arp类型
		EthernetPacket etherHeader = (EthernetPacket)packet.datalink;
		if (etherHeader == null) { //如果是监听lookback网卡它会是空
			//handleIPPacket(packet);
			return;
		}
		/*
		 * 数据链路层在发送数据包时会添加一个802.3的以太网包头，格式如下
		 * 0-7字节：[0-6]Preamble , [7]start fo frame delimiter
		 * 8-22字节: [8-13] destination mac, [14-19]: source mac 
		 * 20-21字节: type
		 * type == 0x0806表示数据包是arp包, 0x0800表示IP包,0x8035是RARP包
		 */
		if (etherHeader.frametype == EthernetPacket.ETHERTYPE_ARP) {
			//调用ARP协议解析数据包
			ARPProtocolLayer arpLayer = new ARPProtocolLayer();
			HashMap<String, Object> info = arpLayer.handlePacket(packet);
			byte[] senderIP = (byte[])info.get("sender_ip");
			byte[] senderMac = (byte[])info.get("sender_mac");
			System.out.println(Arrays.toString(senderIP));
			ipToMacTable.put(Arrays.toString(senderIP), senderMac);
			//一旦有mac地址更新后，查看缓存表是否有等待发送的数据
			sendWaitingData();
		}
		
		//处理IP包头
		
		if (etherHeader.frametype == EthernetPacket.ETHERTYPE_IP) {
			handleIPPacket(packet);
		}
		
	}
	
	private void handleIPPacket(Packet packet) {
		IProtocol ipProtocol = new IPProtocolLayer();
		HashMap<String, Object> info = ipProtocol.handlePacket(packet);
		if (info == null) {
			return ;
		}
		
		byte protocol = 0;
		if (info.get("protocol") != null) {
			protocol = (byte)info.get("protocol");
			//设置下一层协议的头部
			packet.header = (byte[])info.get("header");
			System.out.println("receive packet with protocol: " + protocol);
		}
		if (protocol != 0) {
			switch(protocol) {
				case IPPacket.IPPROTO_ICMP:
					handleICMPPacket(packet, info);
					break;
				case IPPacket.IPPROTO_UDP:
					handleUDPPacket(packet, info);
					break;
				case IPPacket.IPPROTO_TCP: 
					handleTCPPacket(packet, info);
					break;
				default:
					return;
			}
					
		}
	}
	
	private void handleUDPPacket(Packet packet, HashMap<String, Object> infoFromUpLayer) {
		IProtocol udpProtocol = new UDPProtocolLayer();
		HashMap<String, Object> headerInfo = udpProtocol.handlePacket(packet);

		short dstPort = (short)headerInfo.get("dest_port");
		//根据端口获得应该接收UDP数据包的程序
		IApplication app = ApplicationManager.getInstance().getApplicationByPort(dstPort);
		if (app != null) {
			app.handleData(headerInfo);	
		}
	}
	
	private void handleTCPPacket(Packet packet,  HashMap<String, Object> infoFromUpLayer) {
		IProtocol tcpProtocol = new TCPProtocolLayer();
		HashMap<String, Object> headerInfo = tcpProtocol.handlePacket(packet);
		short dstPort = (short)headerInfo.get("dest_port");
		short srcPort = (short)headerInfo.get("src_port");
		//根据端口获得应该接收UDP数据包的程序
		IApplication app = ApplicationManager.getInstance().getApplicationByPort(dstPort);
		if (app != null) {
			app.handleData(headerInfo);	
		}
	}
	
	private void handleICMPPacket(Packet packet, HashMap<String, Object> infoFromUpLayer) {
		IProtocol icmpProtocol = new ICMPProtocolLayer();
		HashMap<String, Object> headerInfo = icmpProtocol.handlePacket(packet);
		if (headerInfo != null) {
			for (String key : infoFromUpLayer.keySet()) {
				headerInfo.put(key, infoFromUpLayer.get(key));			
			}	
		}
		
		//把收到的icmp数据包发送给所有等待对象
		for (int i = 0; i < icmpPacketReceiverList.size(); i++) {
			Application receiver = (Application)icmpPacketReceiverList.get(i);
			receiver.handleData(headerInfo);
		}
	}
		
	
	private void sendWaitingData() {
		//将数据包发送给路由器
		ArrayList<String> sendList = new ArrayList();
		for (String key : dataWaitToSend.keySet()) {
			byte[] data = dataWaitToSend.get(key);
			byte[] mac = ipToMacTable.get(key);
			if (mac != null) {
				 dataLinkInstance.sendData(data, mac, EthernetPacket.ETHERTYPE_IP);	
				 sendList.add(key);
			}
		}
	    for (int i = 0; i < sendList.size(); i++) {
	    	String ip = (String)sendList.get(i);
	    	dataWaitToSend.remove(ip);
	    }
		
	}
}
