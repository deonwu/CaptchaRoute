package org.http.channel.proxy;

import java.io.Serializable;
import java.util.Date;

public class RemoteStatus implements Serializable{
	private static final long serialVersionUID = 1L;
	
	public static final String DISCONNECTED = "disconnected";
	public static final String CONNEDTED = "connected";
	public static final String AUTH_FAILED = "authFailed";
	
	public String connection = DISCONNECTED;	
	public int requestCount = 0;
	
	public transient Date lastActive = new Date();	
	public void copyFrom(RemoteStatus from){
		this.connection = from.connection;
	}
	
	public synchronized void updated(){
		lastActive = new Date(System.currentTimeMillis());
		this.notifyAll();
	}
	
	public String toString(){
		return this.connection;
	}
}
