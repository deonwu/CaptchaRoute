package org.http.channel.client;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.http.Cookie;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.http.channel.proxy.CaptchaItem;
import org.http.channel.settings.Settings;

public class AuthManager {
	private final String secret_key = "uj(1csv6((2-_*%k+a+voof+ex7_k606-j6s&ht6%1y4ls09gj"; 
	private Log log = LogFactory.getLog("gate");
	public SimpleCache userCache = new SimpleCache();
	private AccountStorage[] storages = null; 
	private Settings settings = null;
	
	public AuthManager(Settings s){
		this.settings = s;
	}
	
	public boolean hasPermession(CaptchaItem session){
		String sid = this.getAuthSID(session);
		Account user = null;
		if(sid != null){
			user = (Account)userCache.get(sid, true);
			if(user == null){
				user = retriveCookie(sid);
				if(user != null){
					userCache.set(sid, user, 60 * 30);
				}
			}
		}
		if(user != null){
			session.account = user;
		}
		
		return session.account != null;
	}
	
	public boolean login(CaptchaItem session, String username, String password){
		Account user = null;
		for(AccountStorage storage: storages){
			if(storage == null) continue;
			user = storage.authencate(username, password);
			if(user != null)break;
		}

		return user != null;
	}
	
	public void load(){
		storages = new AccountStorage[2];
		storages[0] = new LADPUserStorage(settings);
		storages[1] = new SystemEnvStorage();
	}
	
	public int activeUserCount(){
		return this.userCache.keys().size();
	}
	
	private Account retriveCookie(String sid){
		log.info("retriveCookie: " + sid);
		String info = null, username = null, fullname = null, email = null, sign = null;
		Account account = null;
		try {
			info = new String(Base64.decode(sid));
			log.info("user info:" + info);
			String[] tmp = info.split(",");
			switch(tmp.length){
				case 4: sign = tmp[3];
				case 3: email = tmp[2];
				case 2: fullname = tmp[1];
				case 1: username = tmp[0];				
			}
			info = String.format("%s,%s,%s,%s", username, fullname, email, secret_key);
			if(sign != null && md5(info).equals(sign)){
				account = new Account();
				account.username = username;
				account.fullname = fullname;
				account.email = email;
			}
		} catch (UnsupportedEncodingException e) {			
		}
		return account;
	}
	
	private String encodeUser(Account account){
		String info = String.format("%s,%s,%s,%s", account.username, account.fullname, account.email, secret_key);		
		String key = String.format("%s,%s,%s,%s", account.username, account.fullname, account.email, md5(info));
		String sid = Base64.encode(key.getBytes());	
		log.info("Encode user:" + sid + ", user name:" + account.username);
		return sid;
	}	
	
	private String getAuthSID(CaptchaItem session){
		String value = session.param.get("Cookie");
		String sid = this.parseCookie(value, "ACSID");
		//log.debug(String.format("cookie:%s, get sid:%s", value, sid));
		return sid;
	}
	
	private String parseCookie(String cookie, String key){
		if(cookie == null)return null;
		int start = cookie.indexOf(key);
		if(start < 0) return null;
		int end = cookie.indexOf(";", start); //Math.max(, cookie.length());
		end = end < 0 ? cookie.length() : end;
		
		return cookie.substring(start + key.length() + 1, end);
	}
	
	private String md5(String data){
		MessageDigest md = null;		
		StringBuffer md5StrBuff = new StringBuffer();	     
		try {
			md = MessageDigest.getInstance("MD5");
			md.update(data.getBytes());		
			byte[] byteArray = md.digest();     
	        for (int i = 0; i < byteArray.length; i++) {
	            if (Integer.toHexString(0xFF & byteArray[i]).length() == 1)     
	                md5StrBuff.append("0").append(Integer.toHexString(0xFF & byteArray[i]));     
	            else     
	                md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));     
	        }	     
		} catch (NoSuchAlgorithmException e) {
			log.error(e.toString(), e);
		}
		return md5StrBuff.toString().toLowerCase();
	}
}
