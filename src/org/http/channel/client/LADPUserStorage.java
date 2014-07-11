package org.http.channel.client;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.http.channel.settings.Settings;

public class LADPUserStorage implements AccountStorage {
	private Log log = LogFactory.getLog("ldap");
	private Settings settings = null;
	private String baseDN = "ou=People,o=NSN";
	private String provider = "ldap://ed.es.emea.nsn-intra.net:389";
	
	public LADPUserStorage(Settings s){
		this.settings = s;
	}
	
	public Account authencate(String username, String password){
		if(username == null || username.trim().length() == 0) return null;
		Account account = this.searchUser(username);
		if(account != null && password != null && password.length() > 0){
			if(this.authencation(account, password)){
				 log.info(String.format("Login ok, uid:%s, full name:%s", username, account.fullname));
			}else {
				log.info("Password error, user name:" + username);
				return null;
			}
		}else {
			log.info("Not found user name:" + username);
		}
		return account;
	}
	
	private Account searchUser(String username){
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,  "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, this.provider + "/" + this.baseDN);
		
		Account account = null;
		
		DirContext ctx = null;
		try {			
			ctx = new InitialDirContext(env);
			
			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			
			NamingEnumeration answer = ctx.search("", 
					String.format("uid=%s", username),      // Filter expression
				    new Object[]{"mail", "cn", "dn"},                // Filter arguments
				    searchControls);
			SearchResult name = null;
			if(answer.hasMore()){
				name = (SearchResult) answer.next();
				Attributes attr = name.getAttributes();	
				
				account = new Account();
				account.username = username;
				account.email = attr.get("mail").get().toString();
				account.fullname = attr.get("cn").get().toString();				
				account.ldapDN = name.getName();				
			}
		} catch (NamingException e) {
			log.error(e.toString(), e);
		} finally{
			if(ctx != null){
				try {
					ctx.close();
				} catch (NamingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return account;
	}
	
	private boolean authencation(Account user, String password){
		Hashtable<String, String> env = new Hashtable<String, String>();

		env.put(Context.INITIAL_CONTEXT_FACTORY,  "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, this.provider + "/" + this.baseDN);
		
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, user.ldapDN + "," + this.baseDN);
		env.put(Context.SECURITY_CREDENTIALS, password);

		// Create the initial context
		DirContext ctx = null;
		boolean isOk = false;
		try {			
			ctx = new  InitialDirContext(env);
			isOk = true;
		} catch (NamingException e) {
			log.error(e.toString(), e);
		} finally {
			if(ctx != null){
				try {
					ctx.close();
				} catch (NamingException e) {
					log.error(e.toString(), e);
				}
			}
		}	
		return isOk;
		
	}
	
}
