package protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Timestamp;
import java.util.HashMap;

import jpcap.packet.Packet;
import utils.Utility;

public class TCPProtocolLayer implements IProtocol {
	private static int HEADER_LENGTH = 20;
	private int sequence_number = 2;
	private int acknowledgement_number = 0;
	private static int PSEUDO_HEADER_LENGTH = 12;
	public static byte TCP_PROTOCOL_NUMBER = 6;
	private static int POSITION_FOR_DATA_OFFSET = 12;
	private static int POSITION_FOR_CHECKSUM = 16;
	private static byte MAXIMUN_SEGMENT_SIZE_OPTION_LENGTH = 4;
	private static byte MAXIMUN_SEGMENT_OPTION_KIND = 2;
	private static byte WINDOW_SCALE_OPTION_KIND = 3;
	private static byte WINDOW_SCALE_OPTION_LENGTH = 3;
	private static byte WINDOW_SCALE_SHIFT_BYTES = 6;
	private static byte TCP_URG_BIT = (1 << 5);
	private static byte TCP_ACK_BIT = (1 << 4);
	private static byte TCP_PSH_BIT = (1 << 3);
	private static byte TCP_RST_BIT = (1 << 2);
	private static byte TCP_SYN_BIT = (1 << 1);
	private static byte TCP_FIN_BIT = (1);
	@Override
	public byte[] createHeader(HashMap<String, Object> headerInfo) {
		byte[] header_buf = new byte[HEADER_LENGTH];
		ByteBuffer byteBuffer = ByteBuffer.wrap(header_buf);
		if (headerInfo.get("src_port") == null) {
			return null;
		}
		short srcPort = (short)headerInfo.get("src_port");
		byteBuffer.putShort(srcPort);
		if (headerInfo.get("dest_port") == null) {
			return  null;
		}
		short  destPort = (short)headerInfo.get("dest_port");
		byteBuffer.putShort(destPort);
		
		//设置初始序列号
		if (headerInfo.get("seq_num") != null) {
			sequence_number = (int)headerInfo.get("seq_num");
		}
		if (headerInfo.get("ack_num") != null) {
			acknowledgement_number = (int)headerInfo.get("ack_num");
		}
		byteBuffer.putInt(sequence_number); 
		byteBuffer.putInt(acknowledgement_number);
		short control_bits = 0;
		//设置控制位
		if (headerInfo.get("URG") != null) {
			control_bits |= (1 << 5);
		}
		if (headerInfo.get("ACK") != null) {
			control_bits |= (1 << 4);
	    }
		if (headerInfo.get("PSH") != null) {
			control_bits |= (1 << 3);
		}
		if (headerInfo.get("RST") != null) {
			control_bits |= (1 << 2);
		}
		if (headerInfo.get("SYN") != null) {
			control_bits |= (1 << 1);
		}
		if (headerInfo.get("FIN") != null) {
			control_bits |= (1);
		}
		byteBuffer.putShort(control_bits);
		System.out.println(Integer.toBinaryString(control_bits));
		
		char window = 2058;
		byteBuffer.putChar(window);
		short check_sum = 0;
		byteBuffer.putShort(check_sum);
		short urgent_pointer = 0;
		byteBuffer.putShort(urgent_pointer);
		
		byte[] maximun_segment_option = new byte[MAXIMUN_SEGMENT_SIZE_OPTION_LENGTH];
		ByteBuffer maximun_segment_buffer =  ByteBuffer.wrap(maximun_segment_option);
		maximun_segment_buffer.put(MAXIMUN_SEGMENT_OPTION_KIND);
		maximun_segment_buffer.put(MAXIMUN_SEGMENT_SIZE_OPTION_LENGTH);
		short segment_size = 1460;
		maximun_segment_buffer.putShort(segment_size);
		
		byte[] window_scale_option = new byte[WINDOW_SCALE_OPTION_LENGTH];
		ByteBuffer window_scale_buffer = ByteBuffer.wrap(window_scale_option);
		window_scale_buffer.put(WINDOW_SCALE_OPTION_KIND);
		window_scale_buffer.put(WINDOW_SCALE_OPTION_LENGTH);
		window_scale_buffer.put(WINDOW_SCALE_SHIFT_BYTES);
		
		byte[] option_end = new byte[1];
		option_end[0] = 0;
		/*
		byte[] options = new byte[] {
		(byte)0x02 ,(byte)0x04 ,(byte)0x05 ,(byte)0xb4 ,(byte)0x01 ,(byte)0x03 ,(byte)0x03 ,(byte)0x06 ,(byte)0x01 ,(byte)0x01  
		,(byte)0x08 ,(byte)0x0a ,(byte)0x62 ,(byte)0x51 ,(byte)0xb5 ,(byte)0x2d ,(byte)0x00 ,(byte)0x00 ,(byte)0x00 ,(byte)0x00 
		,(byte)0x04 ,(byte)0x02 ,(byte)0x00 ,(byte)0x00
		};
		
		byte[] options = new byte[] {
				(byte)0x01 ,(byte)0x01 
				,(byte)0x08 ,(byte)0x0a ,(byte)0x6d ,(byte)0x0f ,(byte)0x0d ,(byte)0x33 ,(byte)0x77 ,(byte)0x55 
				,(byte)0x27 ,(byte)0x99,
		};*/
		
 		int total_length =  header_buf.length + maximun_segment_option.length + window_scale_option.length + option_end.length;
		//总长度必须是4的倍数，不足的话以0补全
		if (total_length % 4 != 0) {
			total_length = (total_length / 4 + 1) * 4;
		}
		byte[] tcp_buffer = new byte[total_length];
		ByteBuffer buffer = ByteBuffer.wrap(tcp_buffer);
		buffer.put(header_buf);
		buffer.put(maximun_segment_option);
		buffer.put(window_scale_option);
		buffer.put(option_end);
		//buffer.put(options);
		short data_offset = buffer.getShort(POSITION_FOR_DATA_OFFSET);
		data_offset |= (((total_length / 4) & 0x0F) << 12);
		buffer.putShort(POSITION_FOR_DATA_OFFSET, data_offset);
		check_sum = (short)compute_checksum(headerInfo, buffer);
		buffer.putShort(POSITION_FOR_CHECKSUM, check_sum);
		return buffer.array();
	}
	
	private long compute_checksum(HashMap<String, Object> headerInfo, ByteBuffer tcp_buffer) {
		byte[] pseudo_header = new byte[PSEUDO_HEADER_LENGTH];
		ByteBuffer pseudo_header_buf = ByteBuffer.wrap(pseudo_header);
		byte[] src_addr = (byte[])headerInfo.get("src_ip");
		byte[] dst_addr = (byte[])headerInfo.get("dest_ip");
		pseudo_header_buf.put(src_addr);
		pseudo_header_buf.put(dst_addr);
		byte reserved = 0;
		pseudo_header_buf.put(reserved);
		pseudo_header_buf.put(TCP_PROTOCOL_NUMBER);
		//将伪包头和tcp包头内容合在一起计算校验值
		short data_length = 0;
		byte[] data = null;
		if (headerInfo.get("data") != null) {
			data = (byte[])headerInfo.get("data");
			data_length = (short)data.length;
			headerInfo.put("data_length", data.length);
		}
		pseudo_header_buf.putShort((short)(tcp_buffer.array().length + data_length));
		byte[] total_buffer = new byte[PSEUDO_HEADER_LENGTH + tcp_buffer.array().length + data_length];
		ByteBuffer total_buf = ByteBuffer.wrap(total_buffer);
		total_buf.put(pseudo_header);
		total_buf.put(tcp_buffer.array());
		if (data_length > 0) {
			total_buf.put(data);	
		}
		
 		long check_sum =  Utility.checksum(total_buffer, total_buffer.length);

		/*byte[] tcpHeader = new byte[] {
				(byte)0xfc ,(byte)0x5e ,(byte)0x04 ,(byte)0xd2 ,(byte)0x16 ,(byte)0xb8 ,(byte)0x06 
				,(byte)0x1e ,(byte)0x6f ,(byte)0xec ,(byte)0xab ,(byte)0xa7 ,(byte)0x80 ,(byte)0x18  
				,(byte)0x08 ,(byte)0x0a ,(byte)0x0 ,(byte)0x0 ,(byte)0x00 ,(byte)0x00 ,(byte)0x01 ,(byte)0x01 
				,(byte)0x08 ,(byte)0x0a ,(byte)0x6d ,(byte)0x0f ,(byte)0x0d ,(byte)0x33 ,(byte)0x77 ,(byte)0x55 
				,(byte)0x27 ,(byte)0x99,  (byte)0x6f
		};
		total_buffer = new byte[PSEUDO_HEADER_LENGTH + tcpHeader.length];
		total_buf = ByteBuffer.wrap(total_buffer);
		pseudo_header = new byte[PSEUDO_HEADER_LENGTH];
		pseudo_header_buf = ByteBuffer.wrap(pseudo_header);
		pseudo_header_buf.put(src_addr);
		pseudo_header_buf.put(dst_addr);
		reserved = 0;
		pseudo_header_buf.put(reserved);
		pseudo_header_buf.put(TCP_PROTOCOL_NUMBER);
		pseudo_header_buf.putShort((short)tcpHeader.length);
		total_buf.put(pseudo_header);
		total_buf.put(tcpHeader);
		check_sum =  Utility.checksum(total_buffer, total_buffer.length);
		*/
		return check_sum;
	}

	@Override
	public HashMap<String, Object> handlePacket(Packet packet) {
		ByteBuffer buffer= ByteBuffer.wrap(packet.header);
		HashMap<String, Object> headerInfo = new HashMap<String, Object>();
		short src_port = buffer.getShort();
		headerInfo.put("src_port", src_port);
		short dst_port = buffer.getShort();
		headerInfo.put("dest_port", dst_port);
		int seq_num = buffer.getInt();
		headerInfo.put("seq_num", seq_num);
		int ack_num = buffer.getInt();
		headerInfo.put("ack_num", ack_num);
		short control_bits = buffer.getShort();
		if ((control_bits & TCP_ACK_BIT) != 0) {
			headerInfo.put("ACK", 1);
		}
		if ((control_bits & TCP_SYN_BIT) != 0) {
			headerInfo.put("SYN", 1);
		}
		if ((control_bits & TCP_FIN_BIT) != 0) {
			headerInfo.put("FIN", 1);
		}
		short win_size = buffer.getShort();
		headerInfo.put("window", win_size);
		//越过校验值
		buffer.getShort();
		short urg_pointer = buffer.getShort();
		headerInfo.put("urg_ptr", urg_pointer);
		//获取对方发送过来的数据
		headerInfo.put("data", packet.data);
		
		headerInfo.put("packet", packet);
		return headerInfo;
	}

}
