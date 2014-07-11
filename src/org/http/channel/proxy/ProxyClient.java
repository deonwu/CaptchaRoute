package org.http.channel.proxy;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.http.channel.server.ProxyServer;
import org.json.simple.JSONObject;
import org.mortbay.util.ajax.Continuation;

/**
 * 一个ProxyClient, 表示一个代理的转发规则。
 * 
 * 1. 请求属于转发到什么Client.
 * 
 * 2. Clinet下面所有的代理会话和状态。 
 * 
 * @author deon
 *
 */
public class ProxyClient {
	private Log log = LogFactory.getLog("client");
	
	public String name = null;
	/**
	 * 属性设置为Pulbic,为了显示Status时可以读到内部数据
	 */
	public Map<String, CaptchaItem> sessions = new HashMap<String, CaptchaItem>();
	public Queue<String> waiting = new ConcurrentLinkedQueue<String>();
	public Queue<String> blocking = new ConcurrentLinkedQueue<String>();
	
	/**
	 * 验证码工作人员。
	 */
	//public Queue<CaptchaWorker> clients = new ConcurrentLinkedQueue<CaptchaWorker>();
	public Map<String, CaptchaWorker> activeWorker = new HashMap<String, CaptchaWorker>();
	
	public Queue<CaptchaItem> doneSession = new ConcurrentLinkedQueue<CaptchaItem>();
	
	public String accessKey = null;
	/**
	 * 最后响应时间，用来计算超时客户端。
	 */
	public Date lastActive = new Date();
	/**
	 * 确保只有一个线程在作Schedule操作.
	 */
	private final ReentrantLock lock = new ReentrantLock();
	private long lastScheduleTime = 0;
	

	public void newSession(final CaptchaItem s){
		sessions.put(s.sid, s);
		waiting.add(s.sid);
		s.isWaiting = true;
		
		ProxyServer.ins.threadPool.execute(new Runnable(){
			public void run(){
				assignCaptchItem(s);
			}
		});
	}	
	
	/*
	public void closeSession(String sid){
		sessions.remove(sid);
	}*/

	public CaptchaItem responseSessionError(String sid, String code, String msg){
		CaptchaItem r = doneSession(sid);
		if(r != null){
			r.errorCode = code;
			r.errorMsg = msg;
			r.isOk = false;
			responseCaptchaItem(r);
	
			if(r.curWorker != null){
				r.curWorker.notifyChange(r, CaptchaStatus.ST_TIMEOUT);
			}		
		}
		return r;
	}
	
	public CaptchaItem responseSession(String sid, String code){
		CaptchaItem r = doneSession(sid);
		if(r != null){
			r.code = code;
			r.isOk = true;
			responseCaptchaItem(r);
		}
		return r;
	}
	
	protected void responseCaptchaItem(CaptchaItem item){
		item.isWaiting = false;
		
		if(item.continuation == null || item.continuation.isResumed()){
			return;
		}
		
		Writer writer = (Writer)item.continuation.getObject();
		
		Map<String, String> result = new HashMap<String, String>();
		result.put("sid", item.sid);
		
		if(item.isOk){
			result.put("status", "ok");
			result.put("captcha", item.code);			
		}else {
			result.put("status", "err");
			result.put("code", item.errorCode);			
			result.put("msg", item.errorMsg);			
		}
		
		try {
			JSONObject.writeJSONString(result, writer);
		} catch (IOException e) {
			log.error("Response error:" + e.toString(), e);
		}
		
		/**
		 * 把阻塞的请求，回复。
		 */
		item.continuation.resume();		
	}
	
	public CaptchaItem doneSession(String sid){
		blocking.remove(sid);
		schedule();
		
		CaptchaItem s = sessions.remove(sid);
		if (s != null){
			doneSession.add(s);
			while(doneSession.size() > 20){
				doneSession.poll();
			}
		}
		return s;
	}
	
	public void assignCaptchItem(CaptchaItem item){
		List<CaptchaWorker> workerList = new ArrayList<CaptchaWorker>();
		for(CaptchaWorker w: new ArrayList<CaptchaWorker>(activeWorker.values())){
			if(w.isAssignable()){
				workerList.add(w);
			}else if(System.currentTimeMillis() - w.lastSendOkTime > 1000 * 60 * 5){
				activeWorker.remove(w.name);
			}
		}
		CaptchaWorker[] workers = (CaptchaWorker[])workerList.toArray(new CaptchaWorker[]{});
		
		if(workers.length > 1){
			Arrays.sort(workers, new Comparator<CaptchaWorker>(){
				@Override
				public int compare(CaptchaWorker arg0, CaptchaWorker arg1) {
					if(arg0.paddingCount == arg1.paddingCount){
						return 0;
					}
					return arg0.paddingCount > arg1.paddingCount ? 1 : -1;
				}				
			});
		}
		
		CaptchaWorker newWorker = null;
		for(CaptchaWorker w : workers){
			if(item.curWorker == null || item.curWorker != w){
				if(w.assginCaptchaItem(item)){
					newWorker = w;
					newWorker.paddingCount++;
					log.info("assign task:" + item.sid + " to:" + w.name + ", padding:" + newWorker.paddingCount + " [OK]");
					break;
				}else {
					log.info("assign task:" + item.sid + " to:" + w.name + " [ERROR]");					
				}
			}
		}
		
		/**
		 * 如果找到新的码工，就分配。如果没有找到新的码工，重复的推送。避免码工重启客户端，没有收到
		 */
		if(newWorker != null){
			item.lastAssignTime = System.currentTimeMillis();
			if(item.curWorker != null){
				item.curWorker.notifyChange(item, CaptchaStatus.ST_REASSIGN);
			}
			item.curWorker = newWorker;
		}else {
			if(item.curWorker != null){
				item.lastAssignTime = System.currentTimeMillis();
				item.curWorker.assginCaptchaItem(item);
				log.info("flush to user:" + item.curWorker.name  +", sid:" + item.sid);				
			}else {
				log.info("Not found worker to assign session, sid:" + item.sid);				
			}
		}
	}
	
	public int activeClient(){
		return this.activeWorker.size();
	}
		
	
	/**
	 * 调度一次把需要转发的请求写到对应的下载队列中。
	 * @param s -- 如果为空,从队列中去一个。
	 */
	public void schedule(){
		/**
		 * 小于1秒内，不重新分配。
		 */
		if(System.currentTimeMillis() - lastScheduleTime < 1000) {
			return;
		}
		
		lastScheduleTime = System.currentTimeMillis();
		if(lock.tryLock()){
			try{
				cleanUpTimeoutSession();
				long now = System.currentTimeMillis();
				for(CaptchaItem item: new ArrayList<CaptchaItem>(sessions.values())){
					if(item.isWaiting){
						if(now - item.createTime > 1000 * 60 * 3){
							responseSessionError(item.sid, CaptchaStatus.ST_TIMEOUT, "Waiting time:"  + ((now - item.createTime) / 1000) + " s");
						}else if(now - item.lastAssignTime > 1000 * 30){
							assignCaptchItem(item);
						}
					}
				}
			}finally{
				lock.unlock();
			}
		}
	}
	
	/**
	 * 超过1分钟没有通信，发送一个状态更新。超过5分钟没有发送成功。删除码工。
	 */
	public void pollingWorks(){
		List<CaptchaWorker> ws = new ArrayList<CaptchaWorker>(activeWorker.values());
		for(CaptchaWorker w: ws){
			if(w.isAssignable() && System.currentTimeMillis() - w.lastSendOkTime > 1000 * 60 * 1){
				w.heartBeat();
			}else if(!w.isAssignable() && System.currentTimeMillis() - w.lastSendOkTime > 1000 * 60 * 5){
				activeWorker.remove(w.name);
				log.info("The worker is long time to active. removed from list, name:" + w.name);
			}
		}
	}
	
	private void cleanUpTimeoutSession(){
		long cur = System.currentTimeMillis();
		for(CaptchaItem s: new ArrayList<CaptchaItem>(sessions.values())){
			if(cur - s.createTime > 1000 * 60 * 4){
				sessions.remove(s.sid);
				waiting.remove(s.sid);
				blocking.remove(s.sid);
				log.info(String.format("Remove time out session:%s, waiting:%s", s.sid, (cur - s.createTime) / 1000));
			}
		}
	}
	
	
}
