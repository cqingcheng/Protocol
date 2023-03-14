package protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

import jpcap.packet.Packet;
import utils.Utility;

public class UDPProtocolLayer implements IProtocol{

    private static short  UDP_LEHGTH_WITHOUT_DATA = 8;
    public static byte PROTOCOL_UDP = 17;
    
    private static short UDP_SRC_PORT_OFFSET = 0;
    private static short UDP_DST_PORT_OFFSET = 2;
    private static short UDP_LENGTH_OFFSET = 4;
    
	@Override
	public byte[] createHeader(HashMap<String, Object> headerInfo) {
		short total_length = UDP_LEHGTH_WITHOUT_DATA;
		byte[] data = null;
		if (headerInfo.get("data") != null) {
			data = (byte[])headerInfo.get("data");
			total_length += data.length;
		}
		
		byte[] buf = new byte[total_length];
		ByteBuffer byteBuffer = ByteBuffer.wrap(buf);
		
		if (headerInfo.get("source_port") == null) {
			return null;
		}
		char srcPort = (char)headerInfo.get("source_port");
		byteBuffer.order(ByteOrder.BIG_ENDIAN);
		byteBuffer.putChar(srcPort);
		
		if (headerInfo.get("dest_port") == null) {
			return  null;
		}
		char  destPort = (char)headerInfo.get("dest_port");
		byteBuffer.order(ByteOrder.BIG_ENDIAN);
		byteBuffer.putChar(destPort);
		
		byteBuffer.order(ByteOrder.BIG_ENDIAN);
		byteBuffer.putShort(total_length);
		//UDP包头的checksum可以直接设置成0x0000
		short checksum = 0;
		byteBuffer.putShort(checksum);
	
		if (data != null) {
			byteBuffer.put(data);
		}
		
			
		return byteBuffer.array();
	}
	


	@Override
	public HashMap<String, Object> handlePacket(Packet packet) {
		ByteBuffer buffer= ByteBuffer.wrap(packet.header);
		HashMap<String, Object> headerInfo = new HashMap<String, Object>();
		
		headerInfo.put("src_port", buffer.getShort(UDP_SRC_PORT_OFFSET));
		headerInfo.put("dest_port", buffer.getShort(UDP_DST_PORT_OFFSET));
		headerInfo.put("length", buffer.getShort(UDP_LENGTH_OFFSET));
		headerInfo.put("data", packet.data);
		
		return headerInfo;
	}

}
