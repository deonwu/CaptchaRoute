package org.http.channel.client;

public class SystemEnvStorage implements AccountStorage {

	@Override
	public Account authencate(String username, String password) {
		 String name = System.getenv("proxy_user");
		 String passwd = System.getenv("proxy_key");
		 
		 Account account = null;
		 if(name != null && name.equals(username) && passwd != null &&
				 passwd.equals(password)){
			 account = new Account();
			 account.username = username;
			 account.fullname = "System Env";
			 account.email = username + "@deonwu84.com";
			 account.authType = "ENV";
		 }
		 
		return account;
	}

}
