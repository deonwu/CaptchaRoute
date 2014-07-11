package org.http.channel.client;

import java.util.Date;

public class Account {
	public String username;
	public String fullname;
	public String email;
	
	public Date loginTime;
	public Date lastActive;
	
	public String ldapDN = null;
	public String authType = "ladp";
}
