package Application;

import java.net.InetAddress;
import utils.ITCPHandler;

public class HTTPClient implements ITCPHandler{
	private  TCPThreeHandShakes  tcp_socket = null;  
	private HTTPEncoder httpEncoder = new HTTPEncoder();
    private String user_agent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 1-14_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.122 Safari/547.36";
    private String accept = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3q=0.9";
    private int total_length = 0;
    private String  http_return_content = "";
    private boolean  receive_first_time = true;
    private int  receive_content_length = 0;
    private static int HTTP_OK = 200;
    private void send_content() throws Exception {//设置http请求数据的头部信息
    	 httpEncoder.set_method(HTTPEncoder.HTTP_METHOD.HTTP_GET, "/");
		 httpEncoder.set_header("Host","192.168.2.127:8888");
		 httpEncoder.set_header("Connection", "keep-alive");
		 httpEncoder.set_header("Upgrade-Insecure-Requests", "1");
		 httpEncoder.set_header("User-Agent", user_agent);
		 httpEncoder.set_header("Accept", accept);
		 httpEncoder.set_header("Purpose", "Prefetch");
		 httpEncoder.set_header("Accept-Encoding", "gzip,deflate");
		 httpEncoder.set_header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
		 String http_content = httpEncoder.get_http_content();
		 System.out.println(http_content);
		 byte[] send_content = http_content.getBytes();
		 tcp_socket.tcp_send(send_content);
    }
	@Override
	public void connect_notify(boolean connect_res) {
		if (connect_res == true) {
			 System.out.println("connect http server ok!");
			 try {
					send_content();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		 } 
		else {
			System.out.println("connect http server fail!");
		}	
	}

	@Override
	public void send_notify(boolean send_res, byte[] packet_send) {
		if (send_res) {
			System.out.println("send request to http server!");
		}
		
	}
    
	private void close_connection() {
		try {
			tcp_socket.tcp_close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void recv_notify(byte[] packet_recv) {
		String content = new String(packet_recv);
		if  (receive_first_time) {
			/*
			  如果是第一次接收服务器返回的数据，那么从http返回的头部获取请求处理结果码和返回数据长度等信息,
			  要不然就是获取服务器返回的数据信息
			 */
			receive_first_time = false;
			int code = httpEncoder.get_return_code(content);
			if  (code != HTTP_OK) {
				System.out.println("http return error: " + code);
				close_connection();
				return;
			} else {
				System.out.println("Http return 200 OK!");
			}
			this.total_length = httpEncoder.get_content_length(content);
			if  (this.total_length <= 0) {
				close_connection();
				return;
			}
			System.out.println("Content Length: " + total_length);
		}
		else {
			//每次接收数据后检查接收数据长度是否达到指定长度，达到表明服务器发送数据完毕，可以关闭连接
			http_return_content += content;
			receive_content_length += packet_recv.length;
			if  (receive_content_length >= total_length) {
				System.out.println("receive content is: " + http_return_content);
				close_connection();
			}
		}
	}

	@Override
	public void connect_close_notify(boolean close_res) {
		if (close_res) {
			System.out.println("Close connection with http server!");
		}
	}
	
	public void run() {
		 try {
			InetAddress ip = InetAddress.getByName("192.168.2.127"); //连接ftp服务器
			short port = 8888;
			tcp_socket = new TCPThreeHandShakes(ip.getAddress(), port, this);
			tcp_socket.tcp_connect();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
