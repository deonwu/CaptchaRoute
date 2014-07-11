package org.http.channel.proxy;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.util.ajax.Continuation;

/**
 * 一个大码工人客户端。
 * @author deonwu
 *
 */
public class CaptchaWorker {
	private Log log = LogFactory.getLog("worker");

	public String name = "worker";
	
	/**
	 * 上次有响应的时间。
	 */
	//public long lastActiveTime = System.currentTimeMillis();

	public long lastSendOkTime = System.currentTimeMillis();

	
	/**
	 * 还没有响应完成的数量。
	 */
	public int paddingCount = 0;
	
	/**
	 * 完成数量。
	 */
	public int doneCount = 0;
	
	/**
	 * 保存等待下载HTTP请求的连接。
	 */
	public List<ObjectWriter> connections = new ArrayList<ObjectWriter>();
	
	
	/**
	 * 是否可以分配。
	 * @return
	 */
	public boolean isAssignable(){
		return connections.size() > 0;
	}
	
	public void newConnection(ObjectWriter s){
		synchronized(connections){
			connections.add(s);
		}
		s.worker = this;
		s.username = name;
		
		log.debug("new connection:" + s.toString());

	}
	
	/**
	 * 把一个验证码分配给一个用户。
	 * @param item
	 * @return
	 */	
	public boolean assginCaptchaItem(CaptchaItem item) {
		//paddingCount++;
		return sendObject(item);
	}
	
	protected boolean sendObject(Object item){
		synchronized(connections){
			ObjectWriter w = null;
			for(Iterator<ObjectWriter> iter = connections.iterator();
					iter.hasNext();){
				w = iter.next();
				if(w.isClosed()){
					iter.remove();
					continue;
				}else {
					w.writeObject(item);
				}
			}
		}

		return true;
	}
	
	public boolean notifyChange(CaptchaItem item, String newStatus){
		
		CaptchaStatus st = new CaptchaStatus();
		st.sid = item.sid;
		st.status = newStatus;
		
		return sendObject(st);
	}
	
	
	public boolean heartBeat(){
		RemoteStatus echo = new RemoteStatus();
		echo.connection = RemoteStatus.CONNEDTED;

		return sendObject(echo);
	}
}
