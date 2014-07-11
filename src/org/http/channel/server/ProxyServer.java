package org.http.channel.server;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.http.channel.Version;
import org.http.channel.proxy.CaptchaItem;
import org.http.channel.proxy.CaptchaWorker;
import org.http.channel.proxy.ObjectWriter;
import org.http.channel.proxy.ProxyClient;
import org.http.channel.proxy.RemoteStatus;
import org.http.channel.settings.Settings;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;
import org.apache.commons.codec.binary.Base64;

/**
 * 基于HTTP穿墙代理：
 * 
 * 墙外用户：        Gate-Server:   Gate-Client:         墙内主机  
 * 
 * 原始请求  --->    打包HTTP头        
 *                             <------下载原始请求
 *                                              ---->  目的请求
 *                                              <---   目的响应
 *                             <------上传响应数据
 *         <----    返回给用户
 * 
 * 墙内的主机始终，主动使用HTTP的方式，连接墙外的服务器。所有原始数据都是在 HTTP的
 * 数据段。最终实现穿墙的目的。
 * 
 * @author deon
 *
 */
public class ProxyServer {
	public static final String XAUTH = "X-proxy-auth";
	public static ProxyServer ins = null;
	public ThreadPoolExecutor threadPool = null;

	private Log log = LogFactory.getLog("gate");
	private long proxySessionId = 0;
	
	private Settings settings = null;
	private Timer timer = new Timer();
	
	/**
	 * 保存当前还在活动的Proxy会话。
	 */
	
	private Map<String, ProxyClient> proxyClients = new HashMap<String, ProxyClient>();
	
	//private Map<String, Queue<String>> newState = new HashMap<String, Queue<String>>();
	//private 
	
	public ProxyServer(Settings s){
		this.settings = s;
		ins = this;

		ProxyClient proxy = new ProxyClient();
		proxy.accessKey = "test";
		proxyClients.put("default", proxy);
	}

	public void run(){
		/**
		 * 小于coreSize自动增加新线程，
		 * 大于coreSize放到Queue里面，
		 * Queue满后开始创建新线程，至到maxSize
		 */
		int core_thread_count = 200;
		threadPool = new ThreadPoolExecutor(
				core_thread_count,
				settings.getInt(Settings.MAX_ROUTE_THREAD_COUNT, 500),
				10, 
				TimeUnit.SECONDS, 
				new LinkedBlockingDeque<Runnable>(core_thread_count * 2)
				);
		
		timer.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run() {
				for(ProxyClient c : new ArrayList<ProxyClient>(proxyClients.values())){
					final ProxyClient tmpClient = c; 
					/**
					 * 重新调度没有响应的验证码。
					 */
					threadPool.execute(new Runnable(){
						public void run(){
							tmpClient.schedule();
						}
					});
					
					/**
					 * 检查码工连接状态。
					 */
					threadPool.execute(new Runnable(){
						public void run(){
							tmpClient.pollingWorks();
						}
					});					
				}
			}
			
		}, 2000, 1000);
		
		startHTTPServer();
	}
	
	//private 
	
	private void startHTTPServer(){
		int httpPort = settings.getInt(Settings.HTTP_PORT, -1);
		Server server = new Server(httpPort);
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);
        handler.addServletWithMapping("org.http.channel.server.servlet.CommandServlet", "/~/*");
        handler.addServletWithMapping("org.http.channel.server.servlet.ProxyServlet", "/route/*");
        try {
        	log.info("Start http server at " + httpPort);
			server.start();
			server.join();
		} catch (Exception e) {
			log.error(e.toString(), e);
		}
	}
	
	/**
	 * 穿墙的第一步. 用户开始HTTP请求，把请求的协议头封装成数据包, 然后阻塞原始请求。 
	 * @param request
	 * @param response
	 * @throws IOException 
	 */
	public void startRequest(HttpServletRequest request, HttpServletResponse response) throws IOException{
		Map<String, String> result = new HashMap<String, String>();
		
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("utf8");
		response.setContentType("application/json");
		
		CaptchaItem s = initSession(request);
		ProxyClient client = this.getProxyClient(request, s.param.get("g"));
		
		if(client != null && client.activeClient() > 0){			
			log.info(String.format("New route request, sid:" + s.sid));			
			s.continuation =  ContinuationSupport.getContinuation(request, null); 
			
			//保存当前Session
			client.newSession(s);
			
			/**
			 * Servlet的方法已经会返回，但是Response的会话是一直保留。直到
			 * s.continuation.resume()， 被调用。
			 * 
			 * 这个其实是一个线程复用的设计模式。
			 */
			log.info("proxy session suspend.");
			s.continuation.setObject(response.getWriter());
			s.continuation.suspend(5 * 60 * 1000);			
		}else {	
			result.put("status", "err");
			if(client != null){
				result.put("code", "没有和跃的码工, 服务分组:" + client.name);
			}else {
				result.put("code", "服务分组创建失败");				
			}
			result.put("msg", "no_worker");
			
			JSONValue.writeJSONString(result, response.getWriter());
		}
	}
	
	private CaptchaItem initSession(HttpServletRequest request){
		CaptchaItem s = newProxySession();
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		
		s.param = new HashMap<String, String>();
		if(isMultipart){
			ServletFileUpload upload = new ServletFileUpload();
			FileItemIterator iter = null;
			
			try{
				iter = upload.getItemIterator(request);				

				while (iter.hasNext()) {
				    FileItemStream item = iter.next();
				    String name = item.getFieldName();
				    InputStream stream = item.openStream();
				    if (item.isFormField()) {
				    	s.param.put(name, Streams.asString(stream));
				    } else {
				    	int len = 0;
						byte[] tmpPostData = new byte[1024 * 64]; 
						if(stream != null){
							for(int i = 0; i >= 0; ){
								i = stream.read(tmpPostData, len, tmpPostData.length - len);
								if(i < 0 )break;
								len += i;
							}
							stream.close();
							if(len > 0){
								s.content = new byte[len];
								System.arraycopy(tmpPostData, 0, s.content, 0, len);						
							}
						}				    	
				    }
				}
			}catch(Exception e){
				log.info("Upload file error:" + e.toString(), e);
			}
		}else {			
			for(Enumeration<String> enums = request.getParameterNames(); enums.hasMoreElements();){
				String name = enums.nextElement();
				s.param.put(name, request.getParameter(name));
			}
			
			//if(s.param.)
			String content = s.param.get("content");
			if(content != null){
				s.content = Base64.decodeBase64(content);
				s.param.remove("content");
				if(s.content != null){
					log.debug("Convert data from base64 code, length:" + s.content.length);
				}else {
					log.debug("Not found image data");					
				}
			}
		}
		
		return s;
	}
	
	/**
	 * 穿墙的第二步. 目的主机下载，用户的HTTP请求的协议数据头。在墙内去转发请求。
	 * @param request
	 * @param response
	 */
	public void forwardRequest(HttpServletRequest request, final HttpServletResponse response) throws IOException{
		//request.get
		//ProxySession s =
		//使用HTTP的分片方式，写数据数据流。
		ProxyClient client = this.getProxyClient(request, true);
		
		if(client != null){
			response.setHeader("Transfer-Encoding", "chunked");
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("application/octet-stream");
			
			final Continuation cons = ContinuationSupport.getContinuation(request, null); 			
			
			FilterOutputStream f = new FilterOutputStream(response.getOutputStream()){
				public void flush() throws IOException{
					super.flush();
					response.flushBuffer();
				}
			};
			
			final ReentrantLock lock = new ReentrantLock();
			ObjectOutputStream os = new ObjectOutputStream(f);
			//ObjectOutputStream
			String xAuth = request.getHeader(XAUTH);
			if(client.accessKey == null || (xAuth != null && client.accessKey.equals(xAuth))){				
				client.lastActive = new Date(System.currentTimeMillis());
				/**
				 * 等待30分钟的HTTP请求。
				 */
				//cons.setObject(os);
				response.flushBuffer();
				
				CaptchaWorker w = this.getWorker(request, client);
				if(w != null){
					w.newConnection(new ObjectWriter(os, cons));
					log.info("new task worker connection, worker:" + w.name);
				}

				cons.suspend(30 * 60 * 1000);
			}else {
				log.info("auth error:" + client.name + ", key:" + client.accessKey + ", client key:" + xAuth);
				RemoteStatus o = new RemoteStatus();
				o.connection = RemoteStatus.AUTH_FAILED;
				os.writeObject(o);
				os.flush();
			}
		}else {
			log.info("Not found proxy clinet:" + request.getServerName());
		}
	}
	
	/**
	 * 穿墙的最后一步. 目的主机Post最终的响应数据。返回数据后，唤醒原始的HTTP连接。返回数据给用户。
	 * @param request
	 * @param response
	 */
	public void doneRequest(HttpServletRequest request, HttpServletResponse response) throws IOException{
    	ProxyClient client = this.getProxyClient(request);
    	
    	Map<String, String> result = new HashMap<String, String>();
		result.put("status", "err");

    	if(client != null){
    		String sid = request.getParameter("sid");
    		String code = request.getParameter("code");
    		String pading = request.getParameter("padding");
    		
    		log.info("response: sid:" + sid + ", code:" + code + ", padding:" + pading);
    		if(sid != null && code != null){
    			client.responseSession(sid, code);
    			result.put("status", "ok");
    		}
    		
    		/**
    		 * 更新码工，还没有完成的数量。
    		 */
    		int count = -1;
    		if(pading != null){
	    		try{
	    			count = Integer.parseInt(pading);
	    		}catch(Exception e){
	    			
	    		}
    		}
    		if(count >= 0){
    			CaptchaWorker w = this.getWorker(request, client);
    			w.paddingCount = count;
    		}
    	}
    	
		try {
			JSONObject.writeJSONString(result, response.getWriter());
		} catch (IOException e) {
			log.error("Response error:" + e.toString(), e);
		}
	}
	
	public void status(HttpServletRequest request, HttpServletResponse response) throws IOException{
		StringBuffer status = new StringBuffer();
		
		status.append(String.format("=====%s %s %s=====\n", Version.getName(), Version.getVersion(), Version.getBuildDate()));
		ProxyClient client = this.getProxyClient(request);
		if(client != null){
			ArrayList<CaptchaItem> sessions = new ArrayList<CaptchaItem>(client.sessions.values());
			status.append(String.format("\n\n=====Active Session=====\n"));
			for(CaptchaItem s: sessions){
				status.append(s.toString() + "\n");
			}
			
			status.append(String.format("\n\n=====Blocking Session=====\n"));
			ArrayList<String> blocking = new ArrayList<String>(client.blocking);
			for(String s: blocking){
				status.append(s + "\n");
			}

			status.append(String.format("\n\n=====Waiting Session=====\n"));
			ArrayList<String> waiting = new ArrayList<String>(client.waiting);
			for(String s: waiting){
				status.append(s + "\n");
			}	
			
			status.append(String.format("\n\n=====Done Session=====\n"));
			for(CaptchaItem s: new ArrayList<CaptchaItem>(client.doneSession)){
				status.append(s.toString() + "\n");
			}
		}else {
			status.append(String.format("=====Active proxy client=====\n"));
			for(String key: proxyClients.keySet()){
				status.append(key + "\n");
			}
		}
		
		response.setContentType("text/plain");
		response.getWriter().write(status.toString());
	}
	
	public void welcome(HttpServletRequest request, HttpServletResponse response) throws IOException{
		static_serve("org/http/channel/server/static/welcome.html", "text/html", response);
	}
	
	/**
	 * 创建一个代理会话。
	 * @return
	 */
	private CaptchaItem newProxySession(){
		CaptchaItem s = new CaptchaItem();
		s.sid = "s" + nextSid();
		s.createTime = System.currentTimeMillis();
		return s;
	}
	
	private synchronized long nextSid(){
		return this.proxySessionId++;
	}
	
	/**
	 * 根据HTTP请求，分析属于哪一个代理的用户。
	 * @return
	 */
	private ProxyClient getProxyClient(HttpServletRequest request){
		return this.getProxyClient(request, false);
	}
	
	private ProxyClient getProxyClient(HttpServletRequest request, String name){
		return getProxyClient(request, name, false);
	}
	
	private ProxyClient getProxyClient(HttpServletRequest request, boolean autoCreate){
		return getProxyClient(request, null, autoCreate);
	}
	
	private ProxyClient getProxyClient(HttpServletRequest request, String serverName, boolean autoCreate){
		ProxyClient n = null;
		
		if(serverName == null){
			serverName = request.getParameter("g");
		}
		if(serverName == null){
			serverName = "default";
		}
		
		/**
		 * 清理过期的代理客户端。
		 */
		if(autoCreate){
			for(String key: new ArrayList<String>(this.proxyClients.keySet())){
				n = this.proxyClients.get(key);
				if(n == null)continue;
				if(System.currentTimeMillis() - n.lastActive.getTime() > 30 * 60 * 1000){
					this.proxyClients.remove(key);
				}
			}
			n = null;
		}
		
		if(this.proxyClients.size() < 50 && !this.proxyClients.containsKey(serverName)){
			log.info(String.format("create new proxy client for '%s'", serverName));
			n =	new ProxyClient();
			n.name = serverName;
			n.accessKey = request.getHeader(XAUTH); 
			this.proxyClients.put(serverName, n);
		}
		n = this.proxyClients.get(serverName);
		if(n == null){
			log.info(String.format("Not found proxy client for '%s'", serverName));
		}
		return n;
	}
	
	private CaptchaWorker getWorker(HttpServletRequest request, ProxyClient client){
		String name = request.getParameter("name");
		CaptchaWorker n = null;
		if(name == null){
			name = "client_" + request.getRemoteAddr();
		}

		if(!client.activeWorker.containsKey(name)){
			n = new CaptchaWorker();
			n.name = name;
			log.info(String.format("Creat new worker '%s' on %s", name, client.name));
			
			client.activeWorker.put(name, n);
		}
		
		n = client.activeWorker.get(name);
		return n;
	}
	
    protected void static_serve(String path, String mimeType, HttpServletResponse response) throws IOException{
	    response.setContentType(mimeType == null ? "text/html" : mimeType);
	    response.setCharacterEncoding("utf-8");
	    InputStream ins = this.getClass().getClassLoader().getResourceAsStream(path);
	    byte[] buffer = new byte[64 * 1024];
	    if(ins == null){
	    	//response.setStatus(HttpServletResponse.SC_NOT_FOUND);
	    	response.getWriter().println("Not found:" + path);
	    }else {
	    	if(response.getOutputStream() != null){
		    	for(int len = ins.read(buffer); len > 0; ){
		    		response.getOutputStream().write(buffer, 0, len);
		    		len = ins.read(buffer);
		    	}
	    	}else { //在Socket, 模式不能取到OutputStream.
	    		response.getWriter().println("Can't get OutputStream.");
	    	}
	    	ins.close();
	    }
    }	
}
