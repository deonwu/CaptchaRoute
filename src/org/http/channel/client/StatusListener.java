package org.http.channel.client;

import org.http.channel.proxy.RemoteStatus;

public interface StatusListener {
	public void updated(RemoteStatus r);
}
