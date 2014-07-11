package org.http.channel.proxy;

import java.io.Serializable;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.http.channel.client.Account;
import org.mortbay.util.ajax.Continuation;

/**
 * 一次HTTP的会话数据.
 * @author deon
 */
public class CaptchaStatus implements Serializable{
	private static final long serialVersionUID = 8989783162442987982L;
	
	public static final String ST_WAITING = "waiting";
	public static final String ST_TIMEOUT = "timeout";
	public static final String ST_REASSIGN = "re_assign";
	public static final String ST_DONE = "done";
	
	public String sid = null;	
	/**
	 * 上次分配码工时间。
	 */
	public long lastAssignTime = 0;
	
	/**
	 * 识别处理的验证码
	 */
	public String status = "";
	
	public String toString(){
		return "sid:" + this.sid + ", status:" + status;
	}
}
