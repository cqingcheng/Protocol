package Application;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Random;

import datalinklayer.DataLinkLayer;
import protocol.IProtocol;
import protocol.ProtocolManager;
import protocol.UDPProtocolLayer;

public class DHCPApplication extends Application{
    private static byte  HARDWARE_TYPE = 1;
    private static byte  HARDWARE_ADDR_LENGTH = 6;
    private static byte  DHCP_HOPS = 0;
    private static byte  MESSAGE_TYPE_REQUEST = 1;
    
    private short  secs_elapsed = 0;
    private short  bootp_flags = 0;
    
    private byte[] client_ip_address = new byte[4];
    private byte[] your_ip_address = new byte[4];
    private byte[] next_server_ip_address = new byte[4];
    private byte[] relay_agent_ip_address = new byte[4];
    
    private static byte[] MAGIC_COOKIE = new byte[] {0x63, (byte) 0x82, 0x53, 0x63};
    
    private static byte [] dhcp_first_part;
    private static byte [] dhcp_options_part;
    
    private static int DHCP_FIRST_PART_LENGTH = 236;
    
    private int transaction_id = 0;
    
    private static byte OPTION_MSG_TYPE_LENGTH = 3;
    private static byte OPTION_MSG_TYPE = 53;
    private static byte OPTION_MSG_DATA_LENGTH = 1;
    private static byte OPTION_MSG_TYPE_DISCOVERY = 1;
    
    private static byte OPTION_PARAMETER_REQUEST_LIST  = 55;
    private static byte OPTION_PARAMETER_REQUEST_LENGTH = 12;
    private static byte OPTION_PARAMETER_REQUEST_DATA_LENGTH = 10;
    //设置请求的数据类型
    private static byte OPTIONS_PARAMETER_SUBNET_MASK = 1;
    private static byte OPTIONS_PARAMETER_STATIC_ROUTER = 121;
    //请求路由器地址
    private static byte OPTIONS_PARAMETER_ROUTER = 3;
    //请求域名服务器
    private static byte OPTIONS_PARAMETER_DOMAIN_NAME_SERVER = 6;
    //请求子网域名
    private static byte OPTIONS_PARAMETER_DOMAIN_NAME = 15;
    //下面的请求参数具体作用暂时忽略
    private static byte OPTIONS_PARAMETER_DOMAIN_SEARCH = 119;
    private static byte OPTIONS_PARAMETER_PROXY = (byte) 0xfc;
    private static byte OPTIONS_PARAMETER_LDPA = 95;
    private static byte OPTIONS_PARAMETER_IP_NAME_SERVER = 44;
    private static byte OPTIONS_PARAMETER_IP_NODE_TYPE = 46;
    
    private static byte OPTION_MAXIMUM_DHCP_MESSAGE_SIZE_TYPE = 57;
    private static byte OPTION_MAXIMUN_DHCP_MESSAGE_SIZE_DATA_LENGTH = 2;
    private static short OPTION_MAXIMUN_DHCP_MESSAGE_SIZE_CONTENT = 1500;
    private static byte OPTION_MAXIMUN_DHCP_MESSAGE_SIZE_LENGTH = 4;
    
    private static byte OPTION_CLIENT_IDENTIFIER = 61;
    private static byte OPTION_CLIENT_IDENTIFIER_DATA_LENGTH = 7;
    private static byte OPTION_CLIENT_IDENTIFIER_HARDWARE_TYPE = 0x01;
    private static byte OPTION_CLIENT_IDENTIFIER_LENGTH =  9;
    
    private static byte OPTION_IP_LEASE_TIME = 51;
    private static byte OPTION_IP_LEASE_TIME_DATA_LENGTH = 4;
    //租借时间是90天
    private static int OPTION_IP_LEASE_TIME_CONTENT = 7776000;
    private static byte OPTION_IP_LEASE_TIME_LENGTH = 6;
    
    private static byte OPTION_HOST_NAME = 12;
    private static byte[] OPTION_HOST_NAME_CONTENT = "appledeMBP".getBytes();
    private static byte OPTION_HOST_NAME_DATA_LENGTH = (byte)OPTION_HOST_NAME_CONTENT.length;
    private static int OPTION_HOST_NAME_LENGTH = 2 + OPTION_HOST_NAME_CONTENT.length;
    
    private static byte OPTION_END = (byte)0xff;
    
    private static char srcPort =  68;
    private static char dstPort = 67;
    
    private static byte DHCP_MSG_REPLY = 2;
    private static short DHCP_MSG_TYPE_OFFSET = 0;
    private static short DHCP_YOUR_IP_ADDRESS_OFFSET = 16;
    private static short DHCP_NEXT_IP_ADDRESS_OFFSET = 20;
    private static short DHCP_OPTIONS_OFFSET = 240;
    
    private static final byte DHCP_MSG_TYPE = 53; 
    private static  final byte DHCP_SERVER_IDENTIFER = 54;
    private static final byte DHCP_IP_ADDRESS_LEASE_TIME = 51;
    private static final byte DHCP_RENEWAL_TIME = 58;
    private static final byte DHCP_REBINDING_TIME = 59;
    private static final byte DHCP_SUBNET_MASK = 1;
    private static final byte DHCP_BROADCAST_ADDRESS = 28;
    private static final byte DHCP_ROUTER = 3;
    private static final byte DHCP_DOMAIN_NAME_SERVER = 6;
    private static final byte DHCP_DOMAIN_NAME = 15;
    
    private static byte DHCP_MSG_OFFER = 2;
    
    //记录来自服务器提供的IP
    private InetAddress  server_supply_ip;
    private static byte OPTION_MSG_REQUEST_TYPE = 3;
    private static byte OPTION_MSG_REQUEST_LENGTH = 1;
    private static byte OPTION_REQUESTED_IP_TYPE = 50;
    private static byte OPTION_REQUESTED_IP_LENGTH = 4;
    private static byte OPTION_REQUESTED_IP_TYPE_LENGTH = 6;
    
    private static byte DHCP_MSG_ACK = 5;
    
    private final static int DHCP_STATE_DISCOVER = 0;
    private final static int DHCP_STATE_REQUESTING = 1;
    
    private static int dhcp_current_state = DHCP_STATE_DISCOVER; 
    
   public DHCPApplication() {
    	Random rand = new Random();
    	transaction_id = rand.nextInt();
    	
    	this.port = srcPort;
    	
    	constructDHCPFirstPart();
    	constructDHCPOptionsPart();
    }
    
    private void constructDHCPFirstPart() {
    	dhcp_first_part = new byte[DHCP_FIRST_PART_LENGTH];
    	ByteBuffer buffer = ByteBuffer.wrap(dhcp_first_part);
    	//设置数据包类型
    	buffer.put(MESSAGE_TYPE_REQUEST);
    	//设置网络类型
    	buffer.put(HARDWARE_TYPE);
    	//设置硬件地址长度
    	buffer.put(HARDWARE_ADDR_LENGTH);
    	//设置数据包跳转次数
    	buffer.put(DHCP_HOPS);
    	
    	//设置会话id
    	//debug 0xdd2d01c0
    	buffer.putInt(transaction_id);
    	//设置等待时间
    	buffer.putShort(secs_elapsed);
    	//设置标志位
    	buffer.putShort(bootp_flags);
    	//设置设备ip
    	buffer.put(client_ip_address);
    	//设置租借ip
    	buffer.put(your_ip_address);
    	//设置下一个服务器ip
    	buffer.put(next_server_ip_address);
    	//设置网关ip
    	buffer.put(relay_agent_ip_address);
    	//设置硬件地址
    	
    	buffer.put(DataLinkLayer.getInstance().deviceFakeMacAddress());
    	
    	//填充接下来的10个字节
    	byte[] padding = new byte[10];
    	buffer.put(padding);
    	//设置64字节的服务器名称
    	byte[] host_name = new byte[64];
    	buffer.put(host_name);
    	//设置128位的byte字段
    	byte[] file = new byte[128];
    	buffer.put(file);
    }
    
    
    private void constructDHCPOptionsPart() {
    	//option 53 DHCP Message Type
    	byte[] option_msg_type = new byte[OPTION_MSG_TYPE_LENGTH];
    	ByteBuffer buffer = ByteBuffer.wrap(option_msg_type);
    	buffer.put(OPTION_MSG_TYPE);
    	buffer.put(OPTION_MSG_DATA_LENGTH);
    	buffer.put(OPTION_MSG_TYPE_DISCOVERY);
    	//option 55 Parameter Request List
    	byte[] parameter_request_list = new byte[OPTION_PARAMETER_REQUEST_LENGTH];
    	buffer = ByteBuffer.wrap(parameter_request_list);
    	buffer.put(OPTION_PARAMETER_REQUEST_LIST);
    	buffer.put(OPTION_PARAMETER_REQUEST_DATA_LENGTH);
    	byte[] option_buffer = new byte[] {OPTIONS_PARAMETER_SUBNET_MASK, OPTIONS_PARAMETER_STATIC_ROUTER,
    			OPTIONS_PARAMETER_ROUTER, OPTIONS_PARAMETER_DOMAIN_NAME_SERVER,
    			OPTIONS_PARAMETER_DOMAIN_NAME, OPTIONS_PARAMETER_DOMAIN_SEARCH,OPTIONS_PARAMETER_PROXY,OPTIONS_PARAMETER_LDPA,
    			OPTIONS_PARAMETER_IP_NAME_SERVER,OPTIONS_PARAMETER_IP_NODE_TYPE};
    	buffer.put(option_buffer);
    	
    	//option 57 Maximum DHCP Message Size
    	byte[] maximun_dhcp_msg_size = new byte[OPTION_MAXIMUN_DHCP_MESSAGE_SIZE_LENGTH];
    	buffer = ByteBuffer.wrap(maximun_dhcp_msg_size);
    	buffer.put(OPTION_MAXIMUM_DHCP_MESSAGE_SIZE_TYPE);
    	buffer.put(OPTION_MAXIMUN_DHCP_MESSAGE_SIZE_DATA_LENGTH);
    	buffer.putShort(OPTION_MAXIMUN_DHCP_MESSAGE_SIZE_CONTENT);
    	
    	//option 61 Client identifier
    	byte[] client_identifier = new byte[OPTION_CLIENT_IDENTIFIER_LENGTH];
    	buffer = ByteBuffer.wrap(client_identifier);
    	buffer.put(OPTION_CLIENT_IDENTIFIER);
    	buffer.put(OPTION_CLIENT_IDENTIFIER_DATA_LENGTH);
    	buffer.put(OPTION_CLIENT_IDENTIFIER_HARDWARE_TYPE);
    	buffer.put(DataLinkLayer.getInstance().deviceFakeMacAddress());
    	
    	//option 51 ip address lease time
    	byte[] ip_lease_time = new byte[OPTION_IP_LEASE_TIME_LENGTH];
    	buffer = ByteBuffer.wrap(ip_lease_time);
    	buffer.put(OPTION_IP_LEASE_TIME);
    	buffer.put(OPTION_IP_LEASE_TIME_DATA_LENGTH);
    	buffer.putInt(OPTION_IP_LEASE_TIME_CONTENT);
    	
    	//option 12 Host Name
    	byte[] host_name = new byte[OPTION_HOST_NAME_LENGTH];
    	buffer = ByteBuffer.wrap(host_name);
    	buffer.put(OPTION_HOST_NAME);
    	buffer.put(OPTION_HOST_NAME_DATA_LENGTH);
    	buffer.put(OPTION_HOST_NAME_CONTENT);
    	
    	//option end
    	byte[] end = new byte[1];
    	end[0] = OPTION_END;
    	byte[] padding = new byte[13];
    	dhcp_options_part = new byte[ + option_msg_type.length + parameter_request_list.length + 
    	                                 maximun_dhcp_msg_size.length + client_identifier.length +
    	                                 ip_lease_time.length + host_name.length + end.length + padding.length];
    	
    	buffer = ByteBuffer.wrap(dhcp_options_part);
    	buffer.put(option_msg_type);
    	buffer.put(parameter_request_list);
    	buffer.put(maximun_dhcp_msg_size);
    	buffer.put(client_identifier);
    	buffer.put(ip_lease_time);
    	buffer.put(host_name);
    	buffer.put(end);
    	buffer.put(padding);
    }
    
    private byte[] createUDPHeader(byte[] data) {
		IProtocol udpProto = ProtocolManager.getInstance().getProtocol("udp");
		if (udpProto == null) {
			return null;
		}
		
		HashMap<String, Object> headerInfo = new HashMap<String, Object>();
	
		headerInfo.put("source_port", srcPort);
		headerInfo.put("dest_port", dstPort);
		
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
		byte[] brocastIP = new byte[] {(byte) 255, (byte) 255, (byte) 255, (byte) 255};
		byte[] sourceIP = new byte[] {0, 0, 0, 0};
		ByteBuffer srcIP = ByteBuffer.wrap(sourceIP);
		headerInfo.put("source_ip", srcIP.getInt());
		
		ByteBuffer destIP = ByteBuffer.wrap(brocastIP);
		headerInfo.put("destination_ip", destIP.getInt());
		byte protocol = UDPProtocolLayer.PROTOCOL_UDP;
		headerInfo.put("protocol", protocol);
		headerInfo.put("identification", (short)this.srcPort);
		byte[] ipHeader = ip4Proto.createHeader(headerInfo);
		
		
		return ipHeader;
	}
    
    public void dhcpDiscovery() {
    	byte[] dhcpDiscBuffer = new byte[dhcp_first_part.length + MAGIC_COOKIE.length + dhcp_options_part.length];
    	ByteBuffer buffer = ByteBuffer.wrap(dhcpDiscBuffer);
    	buffer.put(dhcp_first_part);
    	buffer.put(MAGIC_COOKIE);
    	buffer.put(dhcp_options_part);
    	
    	byte[] udpHeader = createUDPHeader(dhcpDiscBuffer);
    	byte[] ipHeader = createIP4Header(udpHeader.length);
    	
    	byte[] dhcpPacket = new byte[ udpHeader.length + ipHeader.length];
    	buffer = ByteBuffer.wrap(dhcpPacket);
    	buffer.put(ipHeader);
    	buffer.put(udpHeader);
    	//将消息广播出去
    	ProtocolManager.getInstance().broadcastData(dhcpPacket);
    }
    
   
	@Override
	public void handleData(HashMap<String, Object> headerInfo) {
		byte[] data = (byte[])headerInfo.get("data");
		boolean readSuccess = readFirstPart(data);
		if (readSuccess) {
			readOptions(data);
		}
		
	}
	
	private boolean readFirstPart(byte[] data) {
		ByteBuffer buffer = ByteBuffer.wrap(data);
		byte reply = buffer.get(DHCP_MSG_TYPE_OFFSET);
		if (reply != DHCP_MSG_REPLY) {
			return false;
		}
		
		byte[] your_addr = new byte[4];
		buffer.position(DHCP_YOUR_IP_ADDRESS_OFFSET);
		buffer.get(your_addr, 0, your_addr.length);
		System.out.println("available ip offer by dhcp server is: ");
		try {
			//记录下服务器提供的可用ip
			server_supply_ip = InetAddress.getByAddress(your_addr);
			System.out.println(server_supply_ip.getHostAddress());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		buffer.position(DHCP_NEXT_IP_ADDRESS_OFFSET);
		byte[] next_server_addr = new byte[4];
		buffer.get(next_server_addr, 0, next_server_addr.length);
		try {
			InetAddress addr = InetAddress.getByAddress(next_server_addr);
			System.out.println(addr.getHostAddress());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}
	
	private void readOptions(byte[] data) {
		ByteBuffer buff = ByteBuffer.wrap(data);
		buff.position(DHCP_OPTIONS_OFFSET);
		while (true) {
			byte type = buff.get();
			if (type == OPTION_END) {
				break;
			}
		
			switch(type) {
			case DHCP_MSG_TYPE:
				//越过长度字段
				buff.get();
				byte msg_type = buff.get();
				if (msg_type == DHCP_MSG_OFFER) {
					System.out.println("receive DHCP OFFER message from server");
					//接收到DHCP_OFFER后，将状态转变为requesting
					dhcp_current_state = DHCP_STATE_REQUESTING; 
				}
				
			    //receive ack msg
			    if (msg_type == DHCP_MSG_ACK) {
			    	System.out.println("receive DHCP ACK message from server");
			    }
				break;
				
			case DHCP_SERVER_IDENTIFER:
				printOptionArray("DHCP server identifier:", buff);
				break;
			case DHCP_IP_ADDRESS_LEASE_TIME:
				//越过长度字段
				buff.get();
				int lease_time_secs = buff.getInt();
				System.out.println("The ip will lease to us for " + lease_time_secs + "seconds" );
				break;
			case DHCP_RENEWAL_TIME:
				//越过长度字段
				buff.get();
				int renew_time = buff.getInt();
				System.out.println("we need to renew ip after " + renew_time + "seconds");
				break;
			case DHCP_REBINDING_TIME:
				//越过长度字段
				buff.get();
				int rebinding_time = buff.getInt();
				System.out.println("we need to rebinding new ip after  " + rebinding_time + "seconds");
				break;
			case DHCP_SUBNET_MASK:
				printOptionArray("Subnet mask is : ", buff);
				break;
			case DHCP_BROADCAST_ADDRESS:
				printOptionArray("Broadcasting Address is : ", buff);
				break;
			case DHCP_ROUTER:
				printOptionArray("Broadcasting Address is : ", buff);
				break;
			case DHCP_DOMAIN_NAME_SERVER:
				printOptionArray("Domain name server is : ", buff);
				break;
			case DHCP_DOMAIN_NAME:
				int len = buff.get();
				for(int i = 0; i < len; i++) {
					System.out.print((char)buff.get() + " ");
				}
				break;
			}
		}
		
		trigger_action_by_state();
	}
	
	private void printOptionArray(String content, ByteBuffer buff) {
		System.out.println(content);
		int len = buff.get();
		if (len == 4) {
			byte[] buf = new byte[4];
			for (int i = 0; i < len; i++) {
				buf[i] = buff.get();
			}
			
			try {
				InetAddress addr = InetAddress.getByAddress(buf);
				System.out.println(addr.getHostAddress());
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			for (int i = 0; i < len; i++) {
				System.out.print(buff.get() + ".");
			}
		}
		System.out.println("\n");
	}
	
	private void trigger_action_by_state() {
		switch(dhcp_current_state) {
		case DHCP_STATE_REQUESTING:
			dhcpRequest();
			break;
			default:
				break; 
		}
	}
	
	private byte[] constructDHCPRequestOptions() {
		byte[] option_msg_type = new byte[OPTION_MSG_TYPE_LENGTH];
    	ByteBuffer buffer = ByteBuffer.wrap(option_msg_type);
    	buffer.put(DHCP_MSG_TYPE);
    	buffer.put(OPTION_MSG_REQUEST_LENGTH);
    	buffer.put(OPTION_MSG_REQUEST_TYPE);
    	//option 55 Parameter Request List
    	byte[] parameter_request_list = new byte[OPTION_PARAMETER_REQUEST_LENGTH];
    	buffer = ByteBuffer.wrap(parameter_request_list);
    	buffer.put(OPTION_PARAMETER_REQUEST_LIST);
    	buffer.put(OPTION_PARAMETER_REQUEST_DATA_LENGTH);
    	byte[] option_buffer = new byte[] {OPTIONS_PARAMETER_SUBNET_MASK, OPTIONS_PARAMETER_STATIC_ROUTER,
    			OPTIONS_PARAMETER_ROUTER, OPTIONS_PARAMETER_DOMAIN_NAME_SERVER,
    			OPTIONS_PARAMETER_DOMAIN_NAME, OPTIONS_PARAMETER_DOMAIN_SEARCH,OPTIONS_PARAMETER_PROXY,OPTIONS_PARAMETER_LDPA,
    			OPTIONS_PARAMETER_IP_NAME_SERVER,OPTIONS_PARAMETER_IP_NODE_TYPE};
    	buffer.put(option_buffer);
    	
    	//option 57 Maximum DHCP Message Size
    	byte[] maximun_dhcp_msg_size = new byte[OPTION_MAXIMUN_DHCP_MESSAGE_SIZE_LENGTH];
    	buffer = ByteBuffer.wrap(maximun_dhcp_msg_size);
    	buffer.put(OPTION_MAXIMUM_DHCP_MESSAGE_SIZE_TYPE);
    	buffer.put(OPTION_MAXIMUN_DHCP_MESSAGE_SIZE_DATA_LENGTH);
    	buffer.putShort(OPTION_MAXIMUN_DHCP_MESSAGE_SIZE_CONTENT);
    	
    	//add ip request
    	byte[] requested_ip_addr = new byte[OPTION_REQUESTED_IP_TYPE_LENGTH + server_supply_ip.getAddress().length];
    	buffer = ByteBuffer.wrap(requested_ip_addr);
    	buffer.put(OPTION_REQUESTED_IP_TYPE);
    	buffer.put(OPTION_REQUESTED_IP_LENGTH);
    	buffer.put(server_supply_ip.getAddress());
    	
    	//option 61 Client identifier
    	byte[] client_identifier = new byte[OPTION_CLIENT_IDENTIFIER_LENGTH];
    	buffer = ByteBuffer.wrap(client_identifier);
    	buffer.put(OPTION_CLIENT_IDENTIFIER);
    	buffer.put(OPTION_CLIENT_IDENTIFIER_DATA_LENGTH);
    	buffer.put(OPTION_CLIENT_IDENTIFIER_HARDWARE_TYPE);
    	buffer.put(DataLinkLayer.getInstance().deviceFakeMacAddress());
    	
    	//option 51 ip address lease time
    	byte[] ip_lease_time = new byte[OPTION_IP_LEASE_TIME_LENGTH];
    	buffer = ByteBuffer.wrap(ip_lease_time);
    	buffer.put(OPTION_IP_LEASE_TIME);
    	buffer.put(OPTION_IP_LEASE_TIME_DATA_LENGTH);
    	buffer.putInt(OPTION_IP_LEASE_TIME_CONTENT);
    	
    	//option 12 Host Name
    	byte[] host_name = new byte[OPTION_HOST_NAME_LENGTH];
    	buffer = ByteBuffer.wrap(host_name);
    	buffer.put(OPTION_HOST_NAME);
    	buffer.put(OPTION_HOST_NAME_DATA_LENGTH);
    	buffer.put(OPTION_HOST_NAME_CONTENT);
    	
    	//option end
    	byte[] end = new byte[1];
    	end[0] = OPTION_END;
    	byte[] padding = new byte[13];
    	dhcp_options_part = new byte[ + option_msg_type.length + parameter_request_list.length + 
    	                                 maximun_dhcp_msg_size.length + client_identifier.length +
    	                                 + requested_ip_addr.length + 
    	                                 ip_lease_time.length + host_name.length + end.length + padding.length];
    	
    	buffer = ByteBuffer.wrap(dhcp_options_part);
    	buffer.put(option_msg_type);
    	buffer.put(parameter_request_list);
    	buffer.put(maximun_dhcp_msg_size);
    	buffer.put(requested_ip_addr);
    	buffer.put(client_identifier);
    	buffer.put(ip_lease_time);
    	buffer.put(host_name);
    	buffer.put(end);
    	buffer.put(padding);
    	
    	return buffer.array();
	}
	
	public void dhcpRequest() {
		if (this.server_supply_ip == null) {
			return;
		}
		
		byte[] options = constructDHCPRequestOptions();
		byte[] dhcpDiscBuffer = new byte[dhcp_first_part.length + MAGIC_COOKIE.length + options.length];
    	ByteBuffer buffer = ByteBuffer.wrap(dhcpDiscBuffer);
    	buffer.put(dhcp_first_part);
    	buffer.put(MAGIC_COOKIE);
    	buffer.put(dhcp_options_part);
    	
    	byte[] udpHeader = createUDPHeader(dhcpDiscBuffer);
    	byte[] ipHeader = createIP4Header(udpHeader.length);
    	
    	byte[] dhcpPacket = new byte[ udpHeader.length + ipHeader.length];
    	buffer = ByteBuffer.wrap(dhcpPacket);
    	buffer.put(ipHeader);
    	buffer.put(udpHeader);
    	//将消息广播出去
    	ProtocolManager.getInstance().broadcastData(dhcpPacket);
	}
}
