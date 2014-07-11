package org.http.channel.client.gui;

import java.awt.Image;
import java.io.File;
import java.net.URL;
import java.security.AccessControlException;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.http.channel.client.ProxyClient;
import org.http.channel.client.gui.events.EventQueue;
import org.http.channel.client.gui.xui.XUIContainer;
import org.http.channel.settings.Settings;

public class GUIMain {
	
	private Log log = LogFactory.getLog("gate");
	private JFrame main = null;
	private EventQueue eventQueue = null;
	private static boolean isWin = false;
	private static File workDir = null;	
	//private BookController services = null;
	private XUIContainer xui = new XUIContainer();
	
	public static void main(String[] args){
		//isWin = System.getProperty("sun.desktop").equals("windows");		
		mainPrivileged(args);
	}
	
	public static void mainPrivileged(String[] args){
		
		new GUIMain().startup();
	}
	
	public void startup(){
		eventQueue = new EventQueue(null);		
		xui.setEventQueue(eventQueue);

		try{
			UIManager.setLookAndFeel(
		            UIManager.getSystemLookAndFeelClassName());
		}catch(Exception e){
			System.out.println(e.toString());
		}	
		
		initWorkDir();
		//services = createPrivilegedProxy(new DefaultBookController(runingJNLP(),
	 	//		runningSandbox()));	
		Settings s = new Settings(new File(workDir, "client.conf").getAbsolutePath());		
		ProxyClient proxy = new ProxyClient(s);
		//proxy.run();

		eventQueue.registerAction(new EventsHandler(xui, proxy));		
		/*
		 * 使用Event thread来初始化界面。Swing的部分控件方法只能在Event thread调用。
		 * 使用自定义LAF后，只能在Event thread创建JFrame
		 */
		SwingUtilities.invokeLater(new Runnable() {
			public void run(){
				MenuToolbar menuBar = new MenuToolbar(eventQueue);
				xui.addComponent("menuBar", menuBar.getMenuBar());
				
				URL layout = this.getClass().getClassLoader().getResource("org/http/channel/client/gui/MainFrame.xml");
				if(layout != null) {
					xui.load(layout);	
					main = (JFrame)xui.getByName("main");
					main.setIconImage(appIcon());
					xui.addComponent("about", new AboutDialog(main));
					//main.initGui();
	            	//窗口居中.
	            	main.setLocationRelativeTo(null);
	            	main.setVisible(true);
				}else {
					log.error("Not found applicaton layout configuration.");
				}
			}
        });	
		
	}
	
	private boolean runingJNLP(){
		try {
			Class.forName("javax.jnlp.ServiceManager");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
	
	private boolean runningSandbox(){
		try {
			System.getenv("USERNAME");
			return false;
		}catch(AccessControlException e){
			return true;
		}
	}
	
	private void initWorkDir(){
		isWin = System.getProperty("sun.desktop").equals("windows");
		if(isWin){
			workDir = new File(System.getenv("APPDATA"), ".freegate");
		}else {
			workDir = new File(System.getenv("HOME"), ".freegate");
		}
		if(!workDir.isDirectory()){
			workDir.mkdirs();
		}
	}
	
	/*
	private BookController createPrivilegedProxy(final BookController stub){
    	return (BookController)Proxy.newProxyInstance(this.getClass().getClassLoader(), 
				   new Class[]{BookController.class}, 
				   new InvocationHandler(){
						@SuppressWarnings("unchecked")
						public Object invoke(Object prxoy, final Method method,
								final Object[] args) throws Throwable {
							try{
								return AccessController.doPrivileged(
										new PrivilegedExceptionAction() {
											public Object run() throws Exception{
												return method.invoke(stub, args);
											}
										});	
							}catch (PrivilegedActionException e){
								String name = method.getName();
								log.error("Exception:" + e.getCause().toString() +
										  "\n Method:" + name,
										  e);
								throw e.getException();
							}
						}
					}
			);		
	}*/	
	
	public static Icon icon(String iconUrl){
		Icon icon = null;
		//String iconUrl = "org/notebook/gui/images/application.png";
		try{
			icon = new ImageIcon(GUIMain.class.getClassLoader().getResource(iconUrl));
		}catch(Exception e){
			//log.error(e.toString(), e);
		}
		return icon;				
	}
	
	private Image appIcon(){
		return MenuToolbar.icon("org/http/channel/client/gui/images/freegate.png").getImage();			
	}
	
	private Image appIcon16(){
		return appIcon().getScaledInstance(16, 16, Image.SCALE_SMOOTH);			
	}	
	

}
