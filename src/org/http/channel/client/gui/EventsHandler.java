package org.http.channel.client.gui;

import java.awt.CardLayout;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.http.channel.client.ProxyClient;
import org.http.channel.client.StatusListener;
import org.http.channel.client.gui.events.BroadCastEvent;
import org.http.channel.client.gui.events.EventAction;
import org.http.channel.client.gui.xui.XUIContainer;
import org.http.channel.proxy.RemoteStatus;
import org.http.channel.settings.Settings;

public class EventsHandler {
	public static final String REMOTE_DOMAIN = "remote_domain";
	public static final String INTERNAL_DOMAIN = "internal_domain";
	public static final String PROXY_PASSWORD = "proxy_password";
	public static final String HTTP_PROXY = "http_proxy";
	
	public static final String STATUS_REMOTE = "status_remote";
	public static final String STATUS_LOCAL = "status_local";
	public static final String STATUS_REQUEST = "status_request";
	public static final String STATUS_ACTIVE_USER = "status_active_user";
	public static final String STATUS_UPDATED = "status_updated";	
	
	public static final String SAVE_SETTINGS = "saveSettings";
	public static final String CLOSE_SETTINGS = "closeSettings";
	public static final String SHOW_SETTINGS = "ShowSettings";
	public static final String SHOW_STATUS = "ShowStatus";
	
	public static final String HIDDEN_MAINFRAME = "HiddenMainFrame";
	public static final String OPEN_MAINFRAME = "OpenMainFrame";
	private final static DateFormat format= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private Log log = LogFactory.getLog("gate");
	
	private XUIContainer xui = null;
	private ProxyClient proxy = null;
	
	private StatusListener listenerProxy = new StatusListener(){
		@Override
		public void updated(RemoteStatus r) {
			updateStatus();
		}
	};
		
	public EventsHandler(XUIContainer xui, ProxyClient proxy){
		this.xui = xui;
		this.proxy = proxy;
		this.proxy.addStatusListener(listenerProxy);
	}
	
	@EventAction(order=1)
	public void XuiLoaded(final BroadCastEvent event){
	}
	
	/**
	 * 菜单-选择代理状态显示，
	 * @param event
	 */
	@EventAction(order=1)
	public void ProxyStatus(final BroadCastEvent event){
		JPanel actionPanel = (JPanel)xui.getByName("mainLayout");
		CardLayout layout = (CardLayout)actionPanel.getLayout();
		layout.show(actionPanel, "status");
	}

	/**
	 * 菜单-选择代理设置
	 * @param event
	 */
	@EventAction(order=1)
	public void Connection(final BroadCastEvent evnet){
		JPanel actionPanel = (JPanel)xui.getByName("mainLayout");
		CardLayout layout = (CardLayout)actionPanel.getLayout();
		layout.show(actionPanel, "login");		
	}
	
	/**
	 * 设置界面打开时触发.
	 * @param event
	 */
	@EventAction(order=1)
	public void ShowSettings(final BroadCastEvent event){
		JTextField field;
		field = (JTextField)xui.getByName(REMOTE_DOMAIN);
		if(field != null){
			field.setText(proxy.settings.getString(Settings.REMOTE_DOMAIN, ""));
		}

		field = (JTextField)xui.getByName(PROXY_PASSWORD);
		if(field != null){
			field.setText(proxy.settings.getString(Settings.PROXY_SECRET_KEY, ""));
		}

		field = (JTextField)xui.getByName(INTERNAL_DOMAIN);
		if(field != null){
			field.setText(proxy.settings.getString(Settings.INTERNAL_DOMAIN, ""));
		}
	}
	
	@EventAction(order=1)
	public void saveSettings(final BroadCastEvent event){
		JTextField field;
		field = (JTextField)xui.getByName(REMOTE_DOMAIN);
		if(field != null){
			proxy.settings.putSetting(REMOTE_DOMAIN, field.getText());
		}

		field = (JTextField)xui.getByName(PROXY_PASSWORD);
		if(field != null){
			proxy.settings.putSetting(Settings.PROXY_SECRET_KEY, field.getText());
		}

		field = (JTextField)xui.getByName(INTERNAL_DOMAIN);
		if(field != null){
			proxy.settings.putSetting(Settings.INTERNAL_DOMAIN, field.getText());
		}
		
		proxy.settings.save();
		proxy.connect();
		 
		//
		ProxyStatus(event);
	}
	
	/**
	 * 切换到状态显示面板
	 * @param event
	 */
	@EventAction(order=1)
	public void ShowStatus(final BroadCastEvent event){
		updateStatus();
	}
	
	/**
	 * 主窗口打开时触发，开始加载Proxy。
	 * @param event
	 */	
	@EventAction(order=1)
	public void OpenMainFrame(final BroadCastEvent event){
		log.info("OpenMainFrame......");
		String r = proxy.settings.getString(Settings.REMOTE_DOMAIN, "");
		String l = proxy.settings.getString(Settings.INTERNAL_DOMAIN, "");
		if(!proxy.isRunning){
			proxy.run();
		}
		
		JPanel actionPanel = (JPanel)xui.getByName("mainLayout");
		CardLayout layout = (CardLayout)actionPanel.getLayout();			
		
		if(r == null || r.length() == 0 || l == null || l.length() == 0){
			//this.ShowSettings(event);
			log.info("OpenMainFrame......login");
			layout.show(actionPanel, "login");
		}else {
			//this.ShowStatus(event);
			log.info("OpenMainFrame......status");
			layout.show(actionPanel, "status");
		}
	}
	
	/**
	 * 退出系统
	 * @param event
	 */
	@EventAction(order=1)
	public void Exit(final BroadCastEvent event){
		System.exit(0);
	}
	
	@EventAction(order=1)
	public void About(final BroadCastEvent event){
		JDialog about = (JDialog)xui.getByName("about");
		about.setLocationRelativeTo(about.getParent());
		about.setVisible(true);		
	}
	
	@EventAction(order=1)
	public void HiddenMainFrame(final BroadCastEvent event){
		
	}
	
	private void updateStatus(){
		JTextField field = (JTextField)xui.getByName(STATUS_REMOTE);
		//name.setText("http://proyx-nsn.deonwu84.com:8080");
		if(field != null){
			field.setText(proxy.settings.getString(Settings.REMOTE_DOMAIN, ""));
		}

		field = (JTextField)xui.getByName(STATUS_LOCAL);
		if(field != null){
			field.setText(proxy.settings.getString(Settings.INTERNAL_DOMAIN, ""));			
		}

		field = (JTextField)xui.getByName(STATUS_REQUEST);
		if(field != null){
			field.setText(proxy.status.requestCount + "");
		}

		field = (JTextField)xui.getByName(STATUS_ACTIVE_USER);
		if(field != null){
			field.setText(proxy.auth.activeUserCount() + "");
		}

		field = (JTextField)xui.getByName(STATUS_UPDATED);
		if(field != null){
			field.setText(proxy.status.connection + " at " + format.format(proxy.status.lastActive));
		}		
	}
}
