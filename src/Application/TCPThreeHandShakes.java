package Application;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import datalinklayer.DataLinkLayer;
import jpcap.packet.Packet;
import protocol.IProtocol;
import protocol.ProtocolManager;
import protocol.TCPProtocolLayer;
import utils.ITCPHandler;

import java.net.*;

class SendPacketWrapper {
	//将发送的数据包封装起来存储在队列中
	private byte[] packet_to_send;
	private int seq_num;
	private int ack_num;
	private int send_count = 0;

	public SendPacketWrapper(byte[] packet, int seq_num) {
		this.packet_to_send = packet;
		this.seq_num = seq_num;
		this.ack_num = seq_num + packet.length;
	}
	
	public byte[] get_packet() {
		return this.packet_to_send;
	}
	public int get_seq_num() {
		return this.seq_num;
	}
	public int get_ack_num() {
		return this.ack_num;
	}
	public void increase_send_count() {
		this.send_count++;
	}
	public int get_send_count() {
		return this.send_count;
	}

}

class  SendPacketTask extends TimerTask {
	private TCPThreeHandShakes  task_handler;
	public SendPacketTask(TCPThreeHandShakes  handler) {
		this.task_handler = handler;
	}
	@Override
	public void run() {
		this.task_handler.sendPacketInList();
	}
}

public class TCPThreeHandShakes extends Application{
	private byte[] dest_ip;
	private short dest_port;
	private int ack_num = 0;
	private int seq_num = 0;
    ITCPHandler tcp_handler = null;
	//增加协议状态标量
	private static int CONNECTION_IDLE = 0;
	private static int CONNECTION_CONNECTING = 1;
	private static int CONNECTION_CONNECTED = 2;
	private static int CONNECTION_CLOSING = 3;
	private static int CONNECTION_SEND = 4;
	private int  tcp_state = CONNECTION_IDLE;
	private static int PACKET_SEND_TIMES = 3; //连续发生3次不成功则失败
	private  Timer send_timer = new Timer(); //定时将发送队列中的数据包进行发送
	private int packet_resend_time = 2000; //每过一秒就发送队列中存储的数据包
	private SendPacketTask resend_packet_task = null;
	//每次发送数据包时先将它存储在队列中,发送出去收到ack后再将它从队列中去除
	private ArrayList<SendPacketWrapper> send_packet_list = new ArrayList<SendPacketWrapper>();
	
    public TCPThreeHandShakes(byte[] server_ip, short server_port, ITCPHandler tcp_handler)  {
    	this.dest_ip = server_ip;
    	this.dest_port = server_port;
    	 //指定一个固定端口，以便抓包调试	
    	Random rand = new Random();
		this.port = (short)8013;//rand.nextInt();
		this.tcp_handler = tcp_handler;
		resend_packet_task = new SendPacketTask(this);
		send_timer.scheduleAtFixedRate(resend_packet_task, packet_resend_time, packet_resend_time);
    }
    
   public boolean tcp_connect() throws Exception {
	   if (tcp_state != CONNECTION_IDLE) {  //只有在空闲状态才能发起新连接
		   return  false; 
	   }
	   tcp_state = CONNECTION_CONNECTING;
	   beginThreeHandShakes();
	   
	   return true;
   }
   
   public boolean tcp_close() throws Exception {
	   if (this.tcp_state != CONNECTION_CONNECTED) {
		   return false;
	   }
	   tcp_state = CONNECTION_CLOSING;
	   //启动关闭流程
	   beginClose();
	   return true;
   }
   
   public boolean tcp_send(byte[] data) throws Exception {
	   if (this.tcp_state != CONNECTION_CONNECTED && this.tcp_state != CONNECTION_SEND) {
		   return false;
	   }
	   tcp_state = CONNECTION_SEND;
	   System.out.println("current seq num: " + this.seq_num);
	   createAndSendPacket(data, "ACK");
	   return true;
   }
    
   public void beginThreeHandShakes() throws Exception {
	   createAndSendPacket(null, "SYN");
	   this.tcp_state = CONNECTION_CONNECTING;
   }
   //向服务器发起关闭流程
   public void beginClose() throws Exception {
	   createAndSendPacket(null, "FIN,ACK");
	   this.tcp_state = CONNECTION_CLOSING;
   }
   
   private void createAndSendPacket(byte[] data, String flags) throws Exception {
	   byte[] tcpHeader = createTCPHeader(data, flags);
	   if (tcpHeader == null) {
			throw new Exception("tcp Header create fail");
		}	
	   int total_length = tcpHeader.length;
	   //http 数据不能当payload 处理
	   if (data != null) {
		   total_length += data.length;
	   }
	   byte[] ipHeader = createIP4Header(total_length);
	   byte[] packet  = null;
	   if (data != null) {
		   packet = new byte[tcpHeader.length + ipHeader.length + data.length];
	   } else {
		   packet = new byte[tcpHeader.length + ipHeader.length];
	   }
	   ByteBuffer packetBuffer = ByteBuffer.wrap(packet);
	   packetBuffer.put(ipHeader);
	   packetBuffer.put(tcpHeader);
	   if (data != null) {
		   packetBuffer.put(data);   
		 //将发送出去的数据包寄存在队列里
		   savePacketToList(data);
	   }
	   sendPacket(packet);
   }
   
   private void savePacketToList(byte[] packet) {
	   //如果数据包没有存在队列中就加入队列
	   boolean contains = false;
	   for(int i = 0; i < send_packet_list.size(); i++) {
		   SendPacketWrapper packet_wrapper = this.send_packet_list.get(i);
		   if (packet_wrapper.get_packet() == packet) {
			   contains = true;
			   break;
		   }
	   }
	   
	   if (contains == false) {
		   SendPacketWrapper packet_wrapper = new SendPacketWrapper(packet, this.seq_num);
		   this.send_packet_list.add(packet_wrapper);
	   }
   }
   
   public void sendPacketInList() {
	   ArrayList<SendPacketWrapper> wrapper_list = new  ArrayList<SendPacketWrapper>();
	   //将所有在队列中的数据包系数发送,如果数据包发送次数大于给定次数则报告失败
	   for(int i = 0; i < this.send_packet_list.size(); i++) {
		   SendPacketWrapper packet_wrapper = this.send_packet_list.get(i);
		   if (packet_wrapper.get_send_count() >= PACKET_SEND_TIMES) {
			   this.tcp_handler.send_notify(false, packet_wrapper.get_packet());
		   }
		   else {
			   int old_seq_num = this.seq_num;
			   this.seq_num = packet_wrapper.get_seq_num();
			   try {
				    createAndSendPacket(packet_wrapper.get_packet(), "ACK");
			    } catch (Exception e) {
				    e.printStackTrace();
			    }
			   this.seq_num = old_seq_num;
			   wrapper_list.add(packet_wrapper);
		   }
	   }
	   
	  this.send_packet_list = wrapper_list;
   }
   
   private void checkSendPacketByACK(int recv_ack) {
	   ArrayList<SendPacketWrapper> wrapper_list = new  ArrayList<SendPacketWrapper>();
	   //所有ack值小于返回ack的数据包都已经成功发送，此时要将数据包从队列移除并通知上层
	   for(int i = 0; i < this.send_packet_list.size(); i++) {
		   SendPacketWrapper packet_wrapper = this.send_packet_list.get(i);
		   int ack =  packet_wrapper.get_ack_num();
		   System.out.println("receive ack: " + ack);
		   if (packet_wrapper.get_ack_num() <= recv_ack) {
			   this.seq_num = packet_wrapper.get_ack_num();
			   System.out.println("next seq num: "+ this.seq_num);
			   this.tcp_handler.send_notify(true, packet_wrapper.get_packet());
		   }
		   else {
			   wrapper_list.add(packet_wrapper);
		   }
	   }
	   
	  this.send_packet_list = wrapper_list;
	  this.seq_num = recv_ack;
   }
   
   private void sendPacket(byte[] packet) {
	   try {
		    InetAddress ip = InetAddress.getByAddress(dest_ip);
			ProtocolManager.getInstance().sendData(packet, ip.getAddress());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
   }
   
   private byte[] createTCPHeader(byte[] data, String flags) {
	   IProtocol tcpProto = ProtocolManager.getInstance().getProtocol("tcp");
		if (tcpProto == null) {
			return null;
		}
		HashMap<String, Object> headerInfo = new HashMap<String, Object>();
		byte[] src_ip = DataLinkLayer.getInstance().deviceIPAddress();
		headerInfo.put("data", data);
		headerInfo.put("src_ip", src_ip);
		headerInfo.put("dest_ip", this.dest_ip);
		headerInfo.put("src_port", (short)this.port);
		headerInfo.put("dest_port", this.dest_port);
		headerInfo.put("seq_num", this.seq_num);
		headerInfo.put("ack_num", this.ack_num);
		String[] flag_units = flags.split(","); 
		for(int i = 0; i < flag_units.length; i++) {
			headerInfo.put(flag_units[i], 1);
		}
		
		byte[] tcpHeader = tcpProto.createHeader(headerInfo);
		
		
		return tcpHeader;
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
		byte protocol = TCPProtocolLayer.TCP_PROTOCOL_NUMBER;
		headerInfo.put("protocol", protocol);
		headerInfo.put("identification", (short)0);
		byte[] ipHeader = ip4Proto.createHeader(headerInfo);
		return ipHeader;
	}
   
   @Override
	public void handleData(HashMap<String, Object> headerInfo) {
	   short src_port = (short)headerInfo.get("src_port");
	   System.out.println("receive TCP packet with port:" + src_port);
	   boolean ack =  false, syn = false, fin = false;
	   if (headerInfo.get("ACK") != null) {
		   System.out.println("it is a ACK packet");
		   ack = true;
	   }
	   if (headerInfo.get("SYN") != null) {
		   System.out.println("it is a SYN packet");
		   syn = true;
	   }
	   if (headerInfo.get("FIN") != null) {
		   System.out.println("it is a FIN packet");
		   fin = true;
	   }
	   if (ack) {
		   int seq_num = (int)headerInfo.get("seq_num");
		   int ack_num = (int)headerInfo.get("ack_num");
		   byte[] data = (byte[])headerInfo.get("data");
		   int data_length = 0;
		   if (data != null) {  //如果收到对方数据，那么将对方seq加上数据长度最为ack回应
			   data_length += data.length;
		   }
		   try {
			if (this.tcp_state == CONNECTION_CONNECTING && syn) {
				this.tcp_state = CONNECTION_CONNECTED;
				System.out.println("tcp handshake from othersize with seq_num" + seq_num + " and ack_num: " + ack_num);
				this.seq_num = ack_num;
				this.ack_num = seq_num + 1;
				createAndSendPacket(null, "ACK");
				this.tcp_handler.connect_notify(true); //通知上面连接成功
			}
		  if (tcp_state == CONNECTION_SEND || tcp_state == CONNECTION_CONNECTED) {
			  tcp_state = CONNECTION_CONNECTED;
			  checkSendPacketByACK(ack_num);
			  if (data != null && data.length > 0 && seq_num == this.ack_num) {
				   /*
				    * 这里我们简化数据的接收流程,为了提升数据发送效率，很有可能数据包的到来次序与服务器发送时不一样
				    * ，但为了让实现逻辑简单，我们每次只接收指定数据包，例如当前我们等待seq编号为1,2,3的数据包，结果
				    * 数据包抵达的次序为3,1,2，那么我们就只接收数据包1，让对方再次发送数据包2,3，显然这样子会降低效率，
				    * 但为了实现逻辑简单，我们暂时做妥协
				    */
				   this.seq_num = ack_num;
				   this.ack_num = seq_num + data_length;
				   createAndSendPacket(null, "ACK");
				   
				  
				   this.tcp_handler.recv_notify(data);
				   
				   
			   }
		  }
		  
		  if (tcp_state == CONNECTION_CLOSING) {  //向对方发送ack+fin然后收到ack进入到此
			 // tcp_state = CONNECTION_IDLE;
			  this.seq_num = 0;
			  this.ack_num = 0;
		  }
		  
		  if (fin) {  //收到fin包时，ack 要在对方seq上加1
			  this.seq_num = ack_num;
			  this.ack_num = seq_num + data_length + 1;
			  createAndSendPacket(null, "ACK");
			  if (tcp_state != CONNECTION_IDLE && tcp_state != CONNECTION_CLOSING) { //这里意味对方主动发起断开,因此回复ack后再次向对方发送ack+fin
				  createAndSendPacket(null, "FIN,ACK");
				  this.tcp_handler.connect_close_notify(true);
			  }
		  }
		
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	   }
	  //收到服务器发回的fin+ack包，正式关闭连接
	  if (ack && fin) {  
		  System.out.println("receive fin packet and close connection");
		  if (this.tcp_state == CONNECTION_CLOSING) {
				this.tcp_state = CONNECTION_IDLE;
				this.tcp_handler.connect_close_notify(true); //通知上面连接关闭
				 try { 
					   int seq_num = (int)headerInfo.get("seq_num");
					   int ack_num = (int)headerInfo.get("ack_num");
					   System.out.println("tcp handshake closing from othersize with seq_num" + seq_num + " and ack_num: " + ack_num);
					   this.seq_num += 1;
					   this.ack_num = seq_num + 1;
						createAndSendPacket(null, "ACK");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
	  }
	  	   
   }
}
