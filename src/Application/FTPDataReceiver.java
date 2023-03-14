package Application;

import java.net.InetAddress;
import java.nio.ByteBuffer;

import utils.IFTPDataReceiver;
import utils.ITCPHandler;

public class FTPDataReceiver implements ITCPHandler{
	private int data_port = 0;
	private IFTPDataReceiver data_receiver = null;
	private  TCPThreeHandShakes  tcp_socket = null;
	private String server_ip = "";
	private byte[] data_buffer = new byte[4096];
	private ByteBuffer byteBuffer = null;
	public FTPDataReceiver(String ip, int port, IFTPDataReceiver receiver) {
		this.data_port = port;
		this.data_receiver = receiver;
		this.server_ip = ip;
		byteBuffer = ByteBuffer.wrap(data_buffer);
	}
	
	
    public void get_data() {
    	 try {
 			InetAddress ip = InetAddress.getByName(server_ip); //连接ftp服务器
 			tcp_socket = new TCPThreeHandShakes(ip.getAddress(), (short)data_port, this);
 			tcp_socket.tcp_connect();
 		} catch (Exception e) {
 			// TODO Auto-generated catch block
 			e.printStackTrace();
 		}	
    }
    
	@Override
	public void connect_notify(boolean connect_res) {
		if (connect_res) {
			System.out.println("ftp data connection ok");
		} else {
			System.out.println("ftp data connection fail");
		}
		
	}

	@Override
	public void send_notify(boolean send_res, byte[] packet_send) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void recv_notify(byte[] packet_recv) {
		System.out.println("ftp receiving data");
		byteBuffer.put(packet_recv);
	}

	@Override
	public void connect_close_notify(boolean close_res) {
		try {
			tcp_socket.tcp_close();
			data_receiver.receive_ftp_data(byteBuffer.array());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
