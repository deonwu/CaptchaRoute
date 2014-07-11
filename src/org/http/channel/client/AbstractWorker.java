package org.http.channel.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.http.channel.proxy.CaptchaItem;

public class AbstractWorker {
	protected Log log = LogFactory.getLog("gate");
	protected CaptchaItem request = null;
	protected URL remote = null;
	public AbstractWorker(CaptchaItem s, URL remote){
		this.request = s;
		this.remote = remote;
	}
	
	public void uploadResponse(InputStream in, Map<String, List<String>> header, int code) throws IOException{
		//log.debug(String.format("Proxy return:%s, sid:%s", this.request.queryURL, this.request.sid));		
		HTTPForm form = createUploadResponse(header, code);
		
		if(in != null){
			form.startFileStream("file0", "file", in);
		}
		String data = form.read();
		form.close();
		log.debug(String.format("Proxy done:%s, msg:%s", this.request, data));
	}
	
	public HTTPForm createUploadResponse(Map<String, List<String>> header, int code) throws IOException{
		HTTPForm form = null;
		try {
			form = new HTTPForm(remote.toURI().resolve("/~/reponse").toURL());
		} catch (URISyntaxException e) {
			log.error(e.toString(), e);
			return null;
		}
		form.setParameter("sid", this.request.sid);
		form.setParameter("status", code + "");
		
		
		String values = "";
		for(String k: header.keySet()){
			if(k == null) continue;
			List<String> val = header.get(k);
			values = val.get(0);
			for(int i = 1; i < val.size(); i++){
				values += "," + val.get(i);
			}
			//log.info(String.format("%s->%s", k, values));
			if(k.toLowerCase().equals("set-cookie")){
				log.info(String.format("Set cookie:%s=%s", k, values));
				values = this.convertCookie(values);
				//log.info("Conver to proxy cookie:" + values);
				form.setParameter("cookie", values);				
			}else {
				form.setParameter(k, values);
			}
		}
		

		
		return form;
	}
	
	
	protected void forwardAuthRequest(CaptchaItem s) throws IOException{
		InputStream in = this.getClass().getClassLoader().getResourceAsStream("org/http/channel/client/redirect_user_login.html");
		Map<String, List<String>> header = new HashMap<String, List<String>>();
		List<String> hv = new ArrayList<String>();
		hv.add("failed");
		header.put("X-proxy-login", hv);		
		uploadResponse(in, header, 200);		
	}
	
	protected void forwardAuthOK(CaptchaItem s) throws IOException{
		InputStream in = this.getClass().getClassLoader().getResourceAsStream("org/http/channel/client/redirect_login_ok.html");
		Map<String, List<String>> header = new HashMap<String, List<String>>();
		List<String> hv = new ArrayList<String>();
		hv.add("ok");
		header.put("X-proxy-login", hv);
		uploadResponse(in, header, 200);	
	}
	
	protected String convertCookie(String values){
		String[] keys = values.split(";");
		String name, value, age, path;
		name = keys[0].split("=")[0];
		value = keys[0].split("=")[1];
		path = "/";
		age = "31536000";
		String[] tmp = null;
		for(int i = 1; i < keys.length; i++){
			tmp = keys[i].split("=", 2);
			if(tmp[0].toLowerCase().equals("path")){
				path = tmp[1].trim();
			}else if(tmp[0].toLowerCase().equals("Max-Age")){
				age = tmp[1].trim();
			}
		}
		return String.format("%s;%s;%s;%s;%s", name, value, path, age, remote.getHost());		
	}
}
