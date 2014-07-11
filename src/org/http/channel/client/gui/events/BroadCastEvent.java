package org.http.channel.client.gui.events;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * 扩展了ActionEvent，增强为一个广播的事件，可以由多个Listener处理。
 * 1. 支持附加数据到Event。
 * 2. 定义了事件处理的状态。
 * @author deon
 */
public class BroadCastEvent extends ActionEvent {
	public static final int STATUS_ACTIVE = 1;
	public static final int STATUS_ERROR = 2;
	public static final int STATUS_CANCEL = 3;
	public static final int STATUS_DONE = 3;
	public EventQueue queue = null;
	
	private static final long serialVersionUID = 1L;
	private ActionEvent event = null;
	private Map<String, Object> attached = new HashMap<String, Object>();
	public int status = STATUS_ACTIVE;
	
	public BroadCastEvent(ActionEvent event) {
		super(event.getSource(), event.getID(), event.getActionCommand(), 
			  event.getWhen(), event.getModifiers());
		this.event = event;
	}
	
	public BroadCastEvent(Object source, String cmd){
		super(source, 0, cmd);
	}	
	
	public BroadCastEvent(Object source, int id, String cmd, Map<String, Object> param){
		super(source, id, cmd);
		if(param != null){
			this.attached.putAll(param);
		}
	}

	public Object get(String name){
		return attached.get(name);
	}
	
	public void set(String key, Object obj){
		attached.put(key, obj);
	}
	
	public void done(){
		this.status = STATUS_DONE;
	}
	public void cancel(){
		this.status = STATUS_CANCEL;
	}
	
	public void fireNewEvent(String name, Object source, Map<String, Object> param){
		this.queue.fireEvent(name, source, param);
	}
}
