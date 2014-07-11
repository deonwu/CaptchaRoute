package org.http.channel.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Settings {
	
	public final static String HTTP_PORT = "http_port";
	public final static String CORE_ROUTE_THREAD_COUNT = "core_route_thread_count";
	public final static String MAX_ROUTE_THREAD_COUNT = "max_route_thread_count";
	
	public final static String PROXY_SECRET_KEY = "client_secret_key";
	public static final String REMOTE_DOMAIN = "remote_domain";
	public static final String INTERNAL_DOMAIN = "internal_domain";	
	
	private static Log log = LogFactory.getLog("settings");
	protected Properties settings = System.getProperties();	
	private String confName = "master.conf";
	
	//private String[] masterSettings = new String[]{};
	//private String[] routeSettings = new String[]{};
	
	public Settings(String name){
		this.confName = name;
		this.loadSettings();
	}
	
	public void loadSettings(){
		try {
			InputStream is = this.getClass().getClassLoader().getResourceAsStream("org/http/channel/settings" + this.confName);
			if(is != null){
				settings.load(is);
			}else {
				log.info("Not found default configuration!");
			}
		} catch (IOException e) {
			log.error(e, e.getCause());
		}
		
		File f = new File(this.confName);
		InputStream ins = null;
		if(f.isFile()){
			try {
				ins = new FileInputStream(f);
				settings.load(ins);
			} catch (FileNotFoundException e) {
				log.error(e, e.getCause());
			} catch (IOException e) {
				log.error(e, e.getCause());
			} finally{
				if(ins != null) {
					try {
						ins.close();
					} catch (IOException e) {
					}
				}
			}
		}
	}
	
	public void putSetting(String name, String val){
		this.settings.put(name, val);
	}
	
	public String getString(String name, String def){
		return settings.getProperty(name, def);
	}
	
	public int getInt(String name, int def){
		String val = settings.getProperty(name);
		int intVal = def;
		try{
			if(val != null) intVal = Integer.parseInt(val);
		}catch(Exception e){
		}
		
		return intVal;
	}	
	
	public void save(){
		OutputStream os = null;
		try {
			log.info("Saveing settings to " + confName);
			os = new FileOutputStream(new File(confName));
			this.settings.store(os, "");
		} catch (IOException e) {
			log.error(e.toString(), e);
		} finally{
			if(os != null){
				try {
					os.close();
				} catch (IOException e) {
				}
			}
		}
		
	}
		
}
