package Application;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Random;

import protocol.IProtocol;
import protocol.ProtocolManager;
import protocol.UDPProtocolLayer;

public class DNSApplication extends Application {
	private byte[] resove_server_ip = null;
	private String domainName = "";
	private byte[] dnsHeader = null;
	private short transition_id = 0;
	private byte[] dnsQuestion = null;
	private static int QUESTION_TYPE_LENGTH = 2;
	private static int QUESTION_CLASS_LENGTH = 2;
	private static short QUESTION_TYPE_A = 1;
	private static short QUESTION_CLASS = 1;
	private static char DNS_SERVER_PORT = 53;
	//该类型表示服务器返回域名对应服务器的字符串名称
	private static short DNS_ANSWER_CANONICAL_NAME_FOR_ALIAS = 5;
	private static short DNS_ANSWER_HOST_ADDRESS = 1;
	
	
	
    public DNSApplication( byte[] destIP, String domainName) {
    	this.resove_server_ip = destIP;
    	this.domainName = domainName;
    	Random rand = new Random();
    	transition_id = (short)rand.nextInt();
    	this.port = (short)rand.nextInt();
    	constructDNSPacketHeader();
    	constructDNSPacketQuestion();
    }
    
    public void queryDomain() {
    	//向服务器发送域名查询请求数据包
    	byte[] dnsPacketBuffer = new byte[dnsHeader.length + dnsQuestion.length];
    	ByteBuffer buffer = ByteBuffer.wrap(dnsPacketBuffer);
    	buffer.put(dnsHeader);
    	buffer.put(dnsQuestion);
    	
    	byte[] udpHeader = createUDPHeader(dnsPacketBuffer);
    	byte[] ipHeader = createIP4Header(udpHeader.length);
    	
    	byte[] dnsPacket = new byte[udpHeader.length + ipHeader.length];
    	buffer = ByteBuffer.wrap(dnsPacket);
    	buffer.put(ipHeader);
    	buffer.put(udpHeader);
    	//将消息发送给路由器
    	try {
			ProtocolManager.getInstance().sendData(dnsPacket, resove_server_ip);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void constructDNSPacketHeader() {
    	/*
    	 * 构造DNS数据包包头,总共12字节
    	 */
    	byte[] header = new byte[12];
    	ByteBuffer buffer = ByteBuffer.wrap(header);
    	//2字节的会话id
    	buffer.putShort((short)transition_id);
    	//接下来是2字节的操作码，不同的比特位有相应含义
    	short opCode = 0;
    	/*
    	 * 如果是查询数据包，第16个比特位要将最低位设置为0,接下来的4个比特位表示查询类型，如果是查询ip则设置为0，
    	 * 第11个比特位由服务器在回复数据包中设置，用于表明信息是它拥有的还是从其他服务器查询而来，
    	 * 第10个比特位表示消息是否有分割，有的话设置为1，由于我们使用UDP，因此消息不会有分割。
    	 * 第9个比特位表示是否使用递归式查询请求，我们设置成1表示使用递归式查询,
    	 * 第8个比特位由服务器返回时设置，表示它是否接受递归式查询
    	 * 第7,6,5，3个比特位必须保留为0，
    	 * 最后四个比特由服务器回复数据包设置，0表示正常返回数据，1表示请求数据格式错误，2表示服务器出问题，3表示不存在给定域名等等
    	 * 我们发送数据包时只要将第7个比特位设置成1即可
    	 */
    	opCode = (short) (opCode | (1 << 8));
    	buffer.putShort(opCode);
    	//接下来是2字节的question count,由于我们只有1个请求，因此它设置成1
    	short questionCount = 1;
    	buffer.putShort(questionCount);
    	//剩下的默认设置成0
    	short answerRRCount = 0;
    	buffer.putShort(answerRRCount);
    	short authorityRRCount = 0;
    	buffer.putShort(authorityRRCount);
    	short additionalRRCount = 0;
    	buffer.putShort(additionalRRCount);
    	this.dnsHeader = buffer.array();
    }
    
    private void constructDNSPacketQuestion() {
    	/*
    	 * 构造DNS数据包中包含域名的查询数据结构
    	 * 首先是要查询的域名，它的结构是是：字符个数+是对应字符，
    	 * 例如域名字符串pan.baidu.com对应的内容为
    	 * 3pan[5]baidu[3]com也就是把‘.'换成它后面跟着的字母个数
    	 */
    	//根据.将域名分割成多个部分,第一个1用于记录"pan"的长度，第二个1用0表示字符串结束
    	dnsQuestion = new byte[1 + 1 + domainName.length() + QUESTION_TYPE_LENGTH + QUESTION_CLASS_LENGTH];
    	String[] domainParts = domainName.split("\\.");
    	ByteBuffer buffer = ByteBuffer.wrap(dnsQuestion);
    	for (int i = 0; i < domainParts.length; i++) {
    		//先填写字符个数
    		buffer.put((byte)domainParts[i].length());
    		//填写字符
    		for(int k = 0; k < domainParts[i].length(); k++) {
    			buffer.put((byte) domainParts[i].charAt(k));
    		}
    	}
    	//表示域名字符串结束
    	byte end = 0;
    	buffer.put(end);
    	//填写查询问题的类型和级别
    	buffer.putShort(QUESTION_TYPE_A);
    	buffer.putShort(QUESTION_CLASS);
    }
    
    private byte[] createUDPHeader(byte[] data) {
		IProtocol udpProto = ProtocolManager.getInstance().getProtocol("udp");
		if (udpProto == null) {
			return null;
		}
		
		HashMap<String, Object> headerInfo = new HashMap<String, Object>();
		char udpPort = (char)this.port;
		headerInfo.put("source_port", udpPort);
		headerInfo.put("dest_port", DNS_SERVER_PORT);
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
		
		
		ByteBuffer destIP = ByteBuffer.wrap(resove_server_ip);
		headerInfo.put("destination_ip", destIP.getInt());
		byte protocol = UDPProtocolLayer.PROTOCOL_UDP;
		headerInfo.put("protocol", protocol);
		headerInfo.put("identification", transition_id);
		byte[] ipHeader = ip4Proto.createHeader(headerInfo);
		
		
		return ipHeader;
	}
    
    @Override
	public void handleData(HashMap<String, Object> headerInfo) {
    	/*
    	 * 解读服务器回发的数据包，首先读取头2字节判断transition_id是否与我们发送时使用的一致
    	 */
		System.out.println("handleData");
    	byte[] data = (byte[])headerInfo.get("data");
    	if (data == null) {
    		System.out.println("empty data");
    		return;
    	}
    	
    	ByteBuffer buffer = ByteBuffer.wrap(data);
    	short transionID = buffer.getShort();
    	if (transionID != this.transition_id) {
    		System.out.println("transition id different!");
    		return;
    	}
    	
    	//读取2字节flag各个比特位的含义
    	short flag = buffer.getShort();
    	readFlags(flag);
    	//接下来2字节表示请求的数量
    	short questionCount = buffer.getShort();
    	System.out.println("client send " + questionCount + " requests");
    	//接下来的2字节表示服务器回复信息的数量
    	short answerCount = buffer.getShort();
    	System.out.println("server return " + answerCount + " answers");
    	//接下来2字节表示数据拥有属性信息的数量
    	short authorityCount = buffer.getShort();
    	System.out.println("server return " + authorityCount + " authority resources");
    	//接下来2字节表示附加信息的数量
    	short additionalInfoCount = buffer.getShort();
    	System.out.println("serve return " + additionalInfoCount + " additional infos");
    	
    	//回复数据包会将请求数据原封不动的复制，所以接下来我们先处理question数据结构
    	readQuestions(questionCount, buffer);
    	
    	//读取服务器回复信息
    	readAnswers(answerCount, buffer);
    }
    
    private void readQuestions(int count, ByteBuffer buffer) {
		System.out.println("readQuestions");
    	for (int i = 0; i < count; i++) {
    		readStringContent(buffer);
    		//查询问题的类型
    		short  questionType = buffer.getShort();
    		if (questionType == QUESTION_TYPE_A) {
    			System.out.println("request ip for given domain name");
    		}
    		//查询问题的级别
    		short questionClass = buffer.getShort();
    		System.out.println("the class of the request is " + questionClass);
    	}
    }
    
    private void readAnswers(int count, ByteBuffer buffer) {
    	/*
    	 * 回复信息的格式如下：
    	 * 第一个字段是name，它的格式如同请求数据中的域名字符串
    	 * 第二个字段是类型，2字节
    	 * 第三字段是级别，2字节
    	 * 第4个字段是Time to live, 4字节，表示该信息可以缓存多久
    	 * 第5个字段是数据内容长度，2字节
    	 * 第6个字段是内如数组，长度如同第5个字段所示
    	 */
    	
    	/*
    	 * 在读取第name字段时，要注意它是否使用了压缩方式，如果是那么该字段的第一个字节就一定大于等于192，也就是
    	 * 它会把第一个字节的最高2比特设置成11，接下来的1字节表示数据在dns数据段中的偏移
    	 */
		System.out.println("readAnswers");
    	for (int i = 0; i < count; i++) {
    		System.out.println("Name content in answer filed is: ");
        	if (isNameCompression(buffer.get())) {
        		int offset = (int)buffer.get();
        		byte[] array = buffer.array();
        		ByteBuffer dup_buffer = ByteBuffer.wrap(array);
        		//从指定偏移处读取字符串内容
        		dup_buffer.position(offset);
        		readStringContent(dup_buffer);
        		
        	} else {
        		readStringContent(buffer);
        	}
        	
        	short type = buffer.getShort();
        	System.out.println("answer type is : " + type);
        	//接下来2字节对应type
        	if (type == DNS_ANSWER_CANONICAL_NAME_FOR_ALIAS) {
        		System.out.println("this answer contains server string name");
        	}
        	
        	//接下来2字节是级别
        	short cls = buffer.getShort();
        	System.out.println("answer class: " + cls);
        	
        	//接下来4字节是time to live
        	int ttl = buffer.getInt();
        	System.out.println("this information can cache " + ttl + " seconds");
        	
        	//接下来2字节表示数据长度
        	short rdLength = buffer.getShort();
        	System.out.println("content length is " + rdLength);
        	
        	if (type == DNS_ANSWER_CANONICAL_NAME_FOR_ALIAS) {
        		readStringContent(buffer);
        	}
        	
        	if (type == DNS_ANSWER_HOST_ADDRESS) {
        		//显示服务器返回的IP
        		byte[] ip = new byte[4];
        		for (int k = 0; k < 4; k++) {
        			ip[k] = buffer.get();
        		}
        		
        		try {
					InetAddress ipAddr = InetAddress.getByAddress(ip);
					System.out.println("ip address for domain name is: " + ipAddr.getHostAddress());
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}  //if (type == DNS_ANSWER_HOST_ADDRESS)
    	
    	} //for (int i = 0; i < count; i++)
    	
    }
    
    private boolean isNameCompression(byte b) {
    	if ((b & (1<<7)) != 0 && (b & (1<<6)) != 0) {
    		return true;
    	}
    	
    	return false;
    }
    
    private void readStringContent(ByteBuffer buffer) {
		System.out.println("readStringContent");
    	byte charCnt = buffer.get();
    	while(charCnt > 0 || isNameCompression(charCnt) == true) {
    		//如果出现压缩，进行偏移后再读取相应内容
    		if (isNameCompression(charCnt)) {
    			int offset = buffer.get();
    			byte[] array = buffer.array();
    			ByteBuffer dup_buffer = ByteBuffer.wrap(array);
    			dup_buffer.position(offset);
    			readStringContent(dup_buffer);
    			
    			break;
    		}
    		//输出字符
    		for (int i = 0; i < charCnt; i++) {
    			System.out.print((char)buffer.get());
    		}
    		charCnt = buffer.get();
    		if (charCnt != 0) {
    			System.out.print(".");
    		}
    	}
    	
    	System.out.println("\n");
    }
    
    private void  readFlags(short flag) {
		System.out.println("readFlags");
    	//最高字节为1表示该数据包为回复数据包
    	if ((flag & (1 << 15))!= 0) {
    		System.out.println("this is packet return from server");
    	}
    	
    	//如果第9个比特位为1表示客户端请求递归式查询
    	if ((flag & (1 << 8)) != 0) {
    		System.out.println("client requests recursive query!");
    	}
    	
    	//第8个比特位为1表示服务器接受递归式查询请求
    	if ((flag & (1 << 7)) != 0) {
    		System.out.println("server accept recursive query request!");
    	}
    	
    	//第6个比特位表示服务器是否拥有解析信息
    	if ((flag & (1 << 5)) != 0) {
    		System.out.println("sever own the domain info");
    	} else {
    		System.out.println("server query domain info from other servers");
    	}
    }
}
