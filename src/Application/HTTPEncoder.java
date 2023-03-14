package Application;

import java.util.*;
public class HTTPEncoder {
    public enum HTTP_METHOD {
        HTTP_GET,
        HTTP_HEADER,
        HTTP_POST,
        HTTP_OPTIONS
    };
    
    
    private String  http_method = "";
    private String  http_header = "";
    private Map<String, String>  http_header_map = new HashMap<String, String>();
    
    public  void set_method(HTTP_METHOD method, String url) {//构造http 请求方法字符串
    	switch (method) {
    		case HTTP_GET:
    			http_method = "GET ";
    			break;
    		case HTTP_HEADER:
    			http_method = "HEADER ";
    			break;
    		case HTTP_POST:
    			http_method = "POST ";
    			break;
    		case HTTP_OPTIONS:
    			http_method = "OPTIONS ";
    			break;
    }
    
    http_method += url;
    http_method += " HTTP/1.1\r\n";  //必须以/r/n结尾
    }
    public  void  set_header(String header_name, String header_value) {//将头部信息中的字段和对应值对应起来
    	http_header_map.put(header_name, header_value);
    }
    
    public String get_http_content() {//把起始行和头部字段信息组装成http请求数据包文本
    	String  http_content = "";
    	http_content += http_method;
    	for (Map.Entry<String, String> entry : http_header_map.entrySet()) {
    		String header_name = entry.getKey();
    		String header_value = entry.getValue();
    		http_header += header_name;
    		http_header += ": ";
    		http_header += header_value;
    		http_header += "\r\n";
    	}
    	http_content += http_header;
    	http_content += "\r\n";
    	return  http_content;
    }
    
    public  int  get_return_code(String http_return) {
    	String http_code_begin_str = "HTTP/1.1 "; //获取http回复包的返回码，200表示请求被正常处理
    	int http_code_begin = http_return.indexOf(http_code_begin_str); 
    	if  ( http_code_begin!= 0) {
    		return  -1;
    	}
    	http_code_begin += http_code_begin_str.length();
    	String http_code_end_str = " ";
    	int http_code_end = http_return.indexOf(http_code_end_str, http_code_begin);
    	if  (http_code_end == -1) {
    		return -1;
    	}
    	String  http_code = http_return.substring(http_code_begin, http_code_end);
    	int code = Integer.parseInt(http_code);  //通过Content-Length字段获取服务器返回数据长度
    	
    	return code;
    }
    
  public int  get_content_length(String http_return) {
	  String  http_content_length = "Content-Length: ";
	  String  http_line_end = "\r\n";
	  int length_begin = http_return.indexOf(http_content_length);
	  if  (length_begin == -1) {
		  return  0;
	  }
	  
	  length_begin += http_content_length.length();
	  int  length_end = http_return.indexOf(http_line_end, length_begin);
	  String  content_length_str = http_return.substring(length_begin, length_end);
	  int content_length = Integer.parseInt(content_length_str);
	  return  content_length;
  }
}
 