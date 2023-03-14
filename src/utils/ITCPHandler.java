package utils;

public interface ITCPHandler {
	public  void  connect_notify(boolean connect_res);  //返回tcp连接结果
	public  void  send_notify(boolean send_res, byte[] packet_send); //通知数据包发送结果
	public  void  recv_notify(byte[] packet_recv);
	public  void  connect_close_notify(boolean close_res); //返回连接关闭通知
}
