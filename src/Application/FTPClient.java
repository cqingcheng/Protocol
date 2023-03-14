package Application;

import java.net.InetAddress;

import utils.IFTPDataReceiver;
import utils.ITCPHandler;

public class FTPClient implements ITCPHandler, IFTPDataReceiver{
	private  TCPThreeHandShakes  tcp_socket = null;  
	private  int data_port = 0;
	private FTPDataReceiver data_receiver = null;
	private String server_ip;
	@Override
	public void connect_notify(boolean connect_res) {
		 if (connect_res == true) {
			 System.out.println("connect ftp server ok!");
		 }
	}

	@Override
	public void send_notify(boolean send_res, byte[] packet_send) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void recv_notify(byte[] packet_recv) {
		try {
			String server_return = new String(packet_recv, "ASCII");
			System.out.println("receive info from ftp server: " +  server_return);
			String return_code = server_return.substring(0, 3);
			String return_str = server_return.substring(3);
			if (return_code.equals("220")) {
				System.out.println("receive code 220: " + return_str);
				send_command("USER chenyi\r\n");
			}
			if (return_code.equals("331")) {
				System.out.println("receive code 331: " + return_str);
				//服务器请求用户名密码
				send_command("PASS 1111\r\n");
			}
			if (return_code.equals("230")) {
				System.out.println("receive code 230: " + return_str);
				//用户登录成功
				send_command("PWD\r\n"); //获取服务器文件目录
			}
			if (return_code.equals("257")) {
				System.out.println("receive code 257: " + return_str);
				send_command("PASV\r\n");
			}
			if (return_code.equals("227")) {
				System.out.println("receive code 227: " + return_str);
				int ip_port_index = return_str.indexOf("("); 
				String port_str = return_str.substring(ip_port_index);
				int ip_count = 4; //经过4个逗号就能找到端口
				while (ip_count > 0) {
					int idx = port_str.indexOf(',');
					ip_count--;
					port_str = port_str.substring(idx + 1);
				}
				int idx = port_str.indexOf(',');
				String p1 = port_str.substring(0, idx);
				port_str = port_str.substring(idx + 1);
				idx = port_str.indexOf(')');
				String p2 = port_str.substring(0, idx);
				int port = Integer.parseInt(p1) * 256 + Integer.parseInt(p2);
				System.out.println("get data port : " + port);
				data_port = port;
				send_command("TYPE A\r\n"); //通知服务器以ASCII的方式传输数据
			}
			if  (return_code.equals("200")) { //服务器同意使用ASCII方式传递数据
				System.out.println("receive code 200: " + return_str);
				send_command("LIST\r\n");//要求服务器传输当前目录下的文件信息
				data_receiver = new FTPDataReceiver(server_ip, data_port, this);
				data_receiver.get_data();
			}
			if (return_code.equals("150")) { //服务器通知数据发送完毕
				System.out.println("receive code 150: " + return_str);
				tcp_socket.tcp_close();
			}
			//tcp_socket.tcp_close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void send_command(String command) {
		try {
			tcp_socket.tcp_send(command.getBytes());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void connect_close_notify(boolean close_res) {
		// TODO Auto-generated method stub
		
	}
	
	public void run() {
		 try {
			InetAddress ip = InetAddress.getByName("192.168.2.127"); //连接ftp服务器
			server_ip = "192.168.2.127";
			short port = 20000;
			tcp_socket = new TCPThreeHandShakes(ip.getAddress(), port, this);
			tcp_socket.tcp_connect();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void receive_ftp_data(byte[] data) {
		System.out.println("Successfuly get ftp data");
		String ftp_data = new String(data);
		System.out.println("content of ftp_data: " + ftp_data);
	}

}
