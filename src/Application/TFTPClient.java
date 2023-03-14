package Application;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import protocol.IProtocol;
import protocol.ProtocolManager;
import protocol.UDPProtocolLayer;

public class TFTPClient extends Application{
	private byte[] sever_ip = null;
	private static short OPTION_CODE_READ = 1;  //读请求操作码
	private static short OPTION_CODE_WRITE = 2; //写请求操作码
	private static final short OPTION_CODE_ACK = 4; //应答
	private static final short OPTION_CODE_DATA = 3; //数据块
	private static final short OPTION_CODE_ERR = 5;  //错误消息
	private static short TFTP_ERROR_FILE_NOT_FOUND = 1;
	private static short OPTION_CODE_LENGTH = 2; //操作码字段占据2字节
	private short data_block = 1;
	private short put_block = 0;
	private static char TFTP_SERVER_PORT = 69;
	private char server_port = 0;
	private File download_file;
	private File upload_file;
	int upload_file_size = 0;
	private String file_name;
	FileOutputStream file_stream;
	FileInputStream file_input;
	
	public TFTPClient(byte[] server_ip) {
		this.sever_ip = server_ip;
	    //指定一个固定端口	
		this.port = (short)56276;
		server_port = TFTP_SERVER_PORT;
	}
	
	public void getFile(String file_name) {
		download_file =  new File(file_name);
		this.file_name = file_name;
		try {
			file_stream = new FileOutputStream(download_file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sendRequestPacket(OPTION_CODE_READ);
	}
	
	public void putFile(String file_name) {
		upload_file = new File(file_name);
		this.file_name = file_name;
		//先打开要上传的文件
		try {
			file_input = new FileInputStream(upload_file);
			upload_file_size = file_input.available();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//向服务器发送写请求
		sendRequestPacket(OPTION_CODE_WRITE);
	}
	
	private void sendRequestPacket(short option) {
		//向服务器发送读请求包
		String mode = "netascii";
		//+1表示要用0表示结尾
		byte[] read_request = new byte[OPTION_CODE_LENGTH + this.file_name.length() + 1 + mode.length() + 1];
		ByteBuffer buffer = ByteBuffer.wrap(read_request);
		buffer.putShort(option);
		buffer.put(this.file_name.getBytes());
		buffer.put((byte)0);
		buffer.put(mode.getBytes());
		buffer.put((byte)0);
		
		byte[] udpHeader = createUDPHeader(read_request);
    	byte[] ipHeader = createIP4Header(udpHeader.length);
    	
    	byte[] readRequestPacket = new byte[udpHeader.length + ipHeader.length];
    	buffer = ByteBuffer.wrap(readRequestPacket);
    	buffer.put(ipHeader);
    	buffer.put(udpHeader);
    	//将消息发送给路由器
    	try {
			ProtocolManager.getInstance().sendData(readRequestPacket, sever_ip);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	 private byte[] createUDPHeader(byte[] data) {
			IProtocol udpProto = ProtocolManager.getInstance().getProtocol("udp");
			if (udpProto == null) {
				return null;
			}
			
			HashMap<String, Object> headerInfo = new HashMap<String, Object>();
			char udpPort = (char)this.port;
			headerInfo.put("source_port", udpPort);
			headerInfo.put("dest_port", server_port);
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
			
			ByteBuffer destIP = ByteBuffer.wrap(sever_ip);
			headerInfo.put("destination_ip", destIP.getInt());
			//假装数据包是192.168.2.128发送的,当前主机ip是192.168.2.243,如果不伪造ip,虚拟机发出的数据包就不会走网卡于是我们就抓不到数据包
			try {
				InetAddress fake_ip = InetAddress.getByName("192.168.2.127");
				ByteBuffer buf = ByteBuffer.wrap(fake_ip.getAddress());
				headerInfo.put("source_ip", buf.getInt());
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			byte protocol = UDPProtocolLayer.PROTOCOL_UDP;
			headerInfo.put("protocol", protocol);
			byte[] ipHeader = ip4Proto.createHeader(headerInfo);
			
			
			return ipHeader;
		}
	    
	    @Override
		public void handleData(HashMap<String, Object> headerInfo) {
	    	byte[] data = (byte[])headerInfo.get("data");
	    	if (data == null) {
	    		System.out.println("empty data");
	    		return;
	    	}
	    	short port = (short)headerInfo.get("src_port");
	    	server_port = (char)port;
	    	ByteBuffer buff = ByteBuffer.wrap(data);
	    	short opCode = buff.getShort();
	    	switch (opCode) {
	    	case OPTION_CODE_ERR:
	    		//处理错误数据包
	    		handleErrorPacket(buff);
	    		break;
	    	case OPTION_CODE_DATA:
	    		handleDataPacket(buff);
	    		break;
	    	case OPTION_CODE_ACK:
	    		handleACKPacket(buff);
	    		break;
	    	}
	    
	    }
	    
	    private void handleACKPacket(ByteBuffer buff) {
	    	/*
	    	 * 头两字节是option code,接下来的两字节是数据块编号
	    	 */
	    	put_block = buff.getShort();
	    	put_block++;
	    	System.out.println("receive server ack with bolck num: " + put_block);
	    	sendDataBlockPacket();
	    }
	    
	    private void sendDataBlockPacket() {
	    	System.out.println("send data block: " + put_block);
	    	/*
	    	 * 数据块数据包包含三部分，头2字节是操作码，接下来2字节是数据块编号，最后512字节是数据块
	    	 */
	    	byte[] file_content = new byte[512];
	    	try {
	    		int bytes_read = file_input.read(file_content);
	    		byte[] content = new byte[2 + 2 + bytes_read];
	    		ByteBuffer buf = ByteBuffer.wrap(content);
	    		buf.putShort(OPTION_CODE_DATA);
	    		buf.putShort(put_block);
	    		buf.put(file_content, 0, bytes_read);
	    		
	    		byte[] udpHeader = createUDPHeader(content);
	        	byte[] ipHeader = createIP4Header(udpHeader.length);
	        	byte[] dataPacket = new byte[udpHeader.length + ipHeader.length];
	        	buf = ByteBuffer.wrap(dataPacket);
	        	buf.put(ipHeader);
	        	buf.put(udpHeader);
	        	ProtocolManager.getInstance().sendData(dataPacket, sever_ip);
	        	System.out.println("send content with bytes: " + bytes_read);
	        	upload_file_size -= bytes_read;
	        	if (bytes_read < 512 || upload_file_size <= 0) {
	        		System.out.println("put file complete");
	        		put_block = 0;
	        	}
	    	} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch(Exception e) {
				e.printStackTrace();
			}
	    	
	    }
	    
	    private void handleDataPacket(ByteBuffer buff) {
	    	//获取数据块编号
	    	data_block = buff.getShort();
	    	System.out.println("receive data block " + data_block);
	    	byte[] data = buff.array();
	    	int content_len = data.length - buff.position(); 
	    	//将数据块写入文件
	    	byte[] file_content = new byte[content_len];
	    	buff.get(file_content);
	    	
	    	try {
	    		file_stream.write(file_content);
	    	    System.out.println("write data block " + data_block + " to file");
	    	} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	
	    	if (content_len == 512) {
	    		sendACKPacket();
	    		data_block++;
	    	}
	    	
	    	if (content_len < 512) {
	    		sendACKPacket();
	    		try {
					file_stream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    		data_block = 1;
	    	}
	    }
	    
	    private void sendACKPacket() {
	    	//ack 数据包只有4个字节，前2字节是ack操作码，后2字节是当前收到的数据块编号
	    	byte[] ack_content = new byte[4];
	    	ByteBuffer buff = ByteBuffer.wrap(ack_content);
	    	buff.putShort(OPTION_CODE_ACK);
	    	buff.putShort(data_block);
	    	
	    	byte[] udpHeader = createUDPHeader(ack_content);
	    	byte[] ipHeader = createIP4Header(udpHeader.length);
	    	
	    	byte[] ackPacket = new byte[udpHeader.length + ipHeader.length];
	    	buff = ByteBuffer.wrap(ackPacket);
	    	buff.put(ipHeader);
	    	buff.put(udpHeader);
	    	//将消息发送给路由器
	    	try {
				ProtocolManager.getInstance().sendData(ackPacket, sever_ip);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    
	    private void handleErrorPacket(ByteBuffer buff) {
	    	//获取具体错误码
	    	short err_info = buff.getShort();
	    	if (err_info == TFTP_ERROR_FILE_NOT_FOUND) {
	    		System.out.println("TFTP server return file not found packet");
	    	}
	    	byte[] data = buff.array();
	    	int pos = buff.position();
	    	int left_len = data.length - pos;
	    	byte[] err_msg = new byte[left_len];
	    	buff.get(err_msg);
	    	String err_str = new String(err_msg);
	    	System.out.println("error message from server : " + err_str);
	    }
}
