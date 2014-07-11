package org.http.channel.client.gui.events;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 应用事件处理中心, 支持一个事件被多个Action处理。多个Action之间可以共享事件处理
 * 的中间数据。
 * 
 * @author deon
 */
public class EventQueue {
	private Log log = LogFactory.getLog("event.queue");
	//public
	private ArrayList<EventMap> queue = new ArrayList<EventMap>();
	
	private ActionListener globalListener = new ActionListener(){
		@Override
		public void actionPerformed(ActionEvent e) {
			dispatchEvent(e);
		}};
	
	public EventQueue(Action defaultAction){
		EventMap e = new EventMap("*");
		e.order = Integer.MAX_VALUE;
		e.action = defaultAction;
		queue.add(e);
	}
	
	public synchronized int registerAction(String name, int order, Action action){
		log.debug(String.format("reg event:%s, order:%s, action:%s", name, order, action.toString()));
		EventMap e = new EventMap(name);
		e.order = order;
		e.action = action;
		if(e.order < 0){
			e.order = queue.size() - 1;
			queue.add(queue.size() - 1, e);
		}else {
			for(int i = 0; i < queue.size(); i++){
				if(queue.get(i).order > order){
					//e.order = i;
					queue.add(i, e);
					break;
				}
			}
		}
		return e.order;
	}
	
	/**
	 * 创建一个ActionListener, 这个Action回路由请求到当前的Queue.
	 * @return
	 */
	public ActionListener getActionListener(){
		return globalListener;
	}
	
	public void registerAction(Object handler){
		if(handler == null)return;
		Method[] actions = handler.getClass().getMethods();
		for(Method m: actions){
			EventAction action = m.getAnnotation(EventAction.class);
			if(action == null) continue;
			Class[] p = m.getParameterTypes();
			if(p.length == 1 && ActionEvent.class.isAssignableFrom(p[0])){
				String event = action.event();
				if(event == null || "".equals(event.trim())){
					event = m.getName();
				}
				registerAction(event, action.order(), new ProxyMethodAction(handler, m));
			}
		}
		log.debug("Event Queue size:" + this.queue.size());
	}

	/**
	 * 返回事件最终被处理的次数。
	 * @param event
	 * @return
	 */
	public int dispatchEvent(ActionEvent event){
		String name = event.getActionCommand();
		BroadCastEvent bcEvent = null;
		if(event instanceof BroadCastEvent){
			bcEvent = (BroadCastEvent) event; 
		}else {
			bcEvent = new BroadCastEvent(event);
		}
		bcEvent.queue = this;
		
		log.debug(String.format("start dispatch event:%s", name));
		int count = 0;
		for(EventMap action: queue){
			if(action.action != null && action.accept(name)){
				try{
					if(log.isTraceEnabled()){
						log.trace(String.format("performed:[%s]%s", action.order, action.action));
					}
					action.action.actionPerformed(bcEvent);
					count++;
					if(bcEvent.status != BroadCastEvent.STATUS_ACTIVE){
						break;
					}
				}catch(Exception e){
					log.error(e.toString(), e);
					break;
				}
			}
		}
		return count;
	}
	
	public int fireEvent(String name, Object source){
		return dispatchEvent(new ActionEvent(source, 0, name));
	}
	
	public int fireEvent(String name, Object source, Map<String, Object> param){
		return dispatchEvent(new BroadCastEvent(source, 0, name, param));
	}	
	
	class EventMap{
		private Pattern p = null;
		String name;
		int order = 0;
		Action action;
		public EventMap(String name){
			this.name = name;
			if(name.indexOf('*') >= 0){
				p = Pattern.compile("^" + name.replaceAll("\\*", ".*") + "$", Pattern.CASE_INSENSITIVE);
			}
		}
		
		public boolean accept(String name){
			if(this.name.equals(name)){
				return true;
			}else if(p != null && p.matcher(name).find()){
				return true;
			}
			return false;
		}
	}
	
	class ProxyMethodAction extends AbstractAction{
		private Object obj = null;
		private Method m = null;
		public ProxyMethodAction(Object o, Method method){
			this.obj = o;
			this.m = method;
		} 

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				m.invoke(this.obj, new Object[]{e});
			} catch (Exception e1) {
				log.error(e1.toString(), e1.getCause());
			}			
		}
		public String toString(){
			return this.m.getName() + "@" + obj.toString();
		}
	}
	//public int 
}
