package org.http.channel.proxy;

import java.io.Serializable;
import java.util.Map;


import org.http.channel.client.Account;
import org.mortbay.util.ajax.Continuation;

/**
 * 一次HTTP的会话数据.
 * @author deon
 */
public class CaptchaItem implements Serializable{
	private static final long serialVersionUID = 8989783162442987982L;
	
	public String sid = null;
	public Map<String, String> param = null;
	//public String method = null;
	//public String queryURL = null;
	public boolean isOk = false;
	public boolean isWaiting = true;
	public byte[] content = null;
	
	/**
	 * 识别状态
	 */
	public String status = CaptchaStatus.ST_WAITING;
	public String errorCode = null;
	public String errorMsg = null;
	
	/**
	 * 上次分配码工时间。
	 */
	public long lastAssignTime = 0;
	
	/**
	 * 识别处理的验证码
	 */
	public String code = "";
	
	
	/**
	 * 一个Jetty6的特有对象，Jetty7后已经成为Servlet3的标准。实现异步Servlet的一个机制。
	 * http://docs.codehaus.org/display/JETTY/Continuations
	 * 
	 * 里面有一个HttpResponse的引用。
	 */
	public transient Continuation continuation = null;
	public transient Account account = null;
	public transient CaptchaWorker curWorker = null;
	public long createTime = 0;
	
	public String toString(){
		if(account != null){
			return "user:" + account.username + ", sid:" + this.sid;
		}else {
			return "sid:" + this.sid + ", ";
		}
	}
}
