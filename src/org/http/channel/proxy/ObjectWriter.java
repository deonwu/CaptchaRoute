package org.http.channel.proxy;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.util.ajax.Continuation;

public class ObjectWriter {
	private static Log log = LogFactory.getLog("writer");
	
	public CaptchaWorker worker = null;
	public String username = null;
	public Queue<Object> waiting = new ConcurrentLinkedQueue<Object>();

	private final ReentrantLock lock = new ReentrantLock();
	private ObjectOutputStream os = null;
	private Continuation cons = null;
	private boolean isClosed = false;
	
	public  ObjectWriter(ObjectOutputStream o, Continuation cons){
		this.os = o;
		this.cons = cons;
		isClosed = false;		
	}
	public void writeObject(Object obj){		
		if(!waiting.contains(obj) && waiting.size() < 10){
			waiting.add(obj);
		}
		if (lock.tryLock()) {
			try {
				for(Object o = waiting.poll(); o != null; o = waiting.poll()){
					if(cons.isPending()){
						try {
							os.writeObject(o);
							os.flush();
							//log.debug("Write object:" + o.toString() + ", to:" + toString() + " [OK]");
							worker.lastSendOkTime = System.currentTimeMillis();
						} catch (Throwable e) {
							isClosed = true;
							try {
								os.close();
								cons.resume();
							} catch (IOException e1) {
							}
							log.debug("Write session Error:" + e.toString() + ", username:" + username);
						}
					}else {
						isClosed = true;
					}
				}
				
			} finally {
				lock.unlock();
			}
		}else {
			log.debug("Failed to get lock, " + toString());
		}
	}
	
	public boolean isClosed(){
		return isClosed;
	}
	
	public String toString(){		
		return "user session:" + os.toString() + ", username:" + username;
	}
}
