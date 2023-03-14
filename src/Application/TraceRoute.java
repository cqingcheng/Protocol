package Application;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import protocol.IProtocol;
import protocol.ProtocolManager;
import protocol.UDPProtocolLayer;

public class TraceRoute  extends Application{
    
	private char dest_port = 33434;
    private byte[] dest_ip = null;
    private byte time_to_live = 1;
    
    private static byte ICMP_TIME_EXCEEDED_TYPE = 1;
    private static byte ICMP_TIME_EXCEEDED_CODE = 0;
    
	public TraceRoute( byte[] destIP) {
	    this.dest_ip = destIP;	
	}
	
	public void startTraceRoute() {
		try {
			byte[] packet = createPackage(null);
			ProtocolManager.getInstance().sendData(packet, dest_ip);
			
			ProtocolManager.getInstance().registToReceiveICMPPacket(this);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private byte[] createPackage(byte[] data) throws Exception {
		byte[] udpHeader = this.createUDPHeader();
		if (udpHeader == null) {
			throw new Exception("UDP Header create fail");
		}		
		byte[] ipHeader = this.createIP4Header(udpHeader.length);
		
		//分别构建ip包头和icmp echo包头后，将两个包头结合在一起
		byte[] packet  = new byte[udpHeader.length + ipHeader.length];
		ByteBuffer packetBuffer = ByteBuffer.wrap(packet);
		packetBuffer.put(ipHeader);
		packetBuffer.put(udpHeader);
		
		return packetBuffer.array();
 	}
	
	private byte[] createUDPHeader() {
		IProtocol udpProto = ProtocolManager.getInstance().getProtocol("udp");
		if (udpProto == null) {
			return null;
		}
		
		HashMap<String, Object> headerInfo = new HashMap<String, Object>();
		char udpPort = (char)this.port;
		headerInfo.put("source_port", udpPort);
		headerInfo.put("dest_port", dest_port);
		
		byte[] data = new byte[24];
		headerInfo.put("data", data);
		
		return udpProto.createHeader(headerInfo);
	}
	
	protected byte[] createIP4Header(int dataLength) {
		IProtocol ip4Proto = ProtocolManager.getInstance().getProtocol("ip");
		if (ip4Proto == null || dataLength <= 0) {
			return null;
		}
		//创建IP包头默认情况下只需要发送数据长度,下层协议号，接收方ip地址
		HashMap<String, Object> headerInfo = new HashMap<String, Object>();
		headerInfo.put("data_length", dataLength);
		ByteBuffer destIP = ByteBuffer.wrap(this.dest_ip);
		headerInfo.put("destination_ip", destIP.getInt());
		byte protocol = UDPProtocolLayer.PROTOCOL_UDP;
		headerInfo.put("protocol", protocol);
		headerInfo.put("identification", (short)this.port);
		//该值必须依次递增
		headerInfo.put("time_to_live", time_to_live);
		byte[] ipHeader = ip4Proto.createHeader(headerInfo);
		
		
		return ipHeader;
		
	}
	
	public void handleData(HashMap<String, Object> data) {
		if (data.get("type") == null || data.get("code") == null) {
			return;
		}
		
		if ((byte)data.get("type") != ICMP_TIME_EXCEEDED_TYPE ||
			(byte)data.get("code") != ICMP_TIME_EXCEEDED_CODE) {
			//收到的不是icmp_time_exceeded类型的消息
			return;
		}
		
		//获得发送该数据包的路由器ip
		byte[] source_ip = (byte[])data.get("source_ip");
		try {
			String routerIP = InetAddress.getByAddress(source_ip).toString();
			System.out.println("ip of the " + time_to_live + "th router in sending route is: " + routerIP );
			dest_port++;
			time_to_live++;
			startTraceRoute();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}

}
