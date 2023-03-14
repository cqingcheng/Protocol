package protocol;

import java.nio.ByteBuffer;
import java.util.HashMap;

import jpcap.packet.Packet;

public class ICMPTimeExceededHeader implements IProtocol{
    private static byte ICMP_TIME_EXCEEDED_TYPE = 1;
	private static byte ICMP_TIME_EXCEEDED_CODE = 0;
	private static int ICMP_TIME_EXCEEDED_DATA_OFFSET = 8;
	
	@Override
	public byte[] createHeader(HashMap<String, Object> headerInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashMap<String, Object> handlePacket(Packet packet) {
		ByteBuffer buffer = ByteBuffer.wrap(packet.header);
		if (buffer.get(0) != ICMP_TIME_EXCEEDED_TYPE &&
			buffer.get(1) != 	ICMP_TIME_EXCEEDED_CODE) {
			return  null;
		}
		
		HashMap<String, Object> headerInfo = new HashMap<String, Object>();
		headerInfo.put("type", ICMP_TIME_EXCEEDED_TYPE);
		headerInfo.put("code", ICMP_TIME_EXCEEDED_CODE);
		
		byte[] data = new byte[packet.header.length - ICMP_TIME_EXCEEDED_DATA_OFFSET];
		buffer.position(ICMP_TIME_EXCEEDED_DATA_OFFSET);
		buffer.get(data, 0, data.length);
		headerInfo.put("data", data);
		return headerInfo;
	}

}
