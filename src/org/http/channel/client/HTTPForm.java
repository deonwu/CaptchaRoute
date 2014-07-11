package org.http.channel.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HTTPForm {
	private Log log = LogFactory.getLog("gate");
	
	private static String boundary = "---------------------------" + randomString() + randomString() + randomString();
	public HttpURLConnection connection = null;
	public OutputStream out = null;
	
	
	public HTTPForm(URL form) throws IOException{
		connection = (HttpURLConnection)form.openConnection();
		connection.setChunkedStreamingMode(1024 * 64);
	    connection.setDoOutput(true);
	    connection.setRequestProperty("Content-Type",
	                                  "multipart/form-data; boundary=" + boundary);
	    
	    this.out = connection.getOutputStream();
	}
	
	public String read() throws IOException{
		submit();		
		return connection.getResponseMessage();
	}
	
	public void close(){
		connection.disconnect();
	}
	
		
	public void submit() throws IOException{
		newline();
	    boundary();
	    write("--");newline();
	    this.out.close();
	    //connection.disconnect();
	}
	
	public void startFileStream(String name, String filename, InputStream in) throws IOException {
	    boundary();
	    writeName(name);
	    write("; filename=\"");
	    write(filename);
	    write('"');
	    newline();
	    write("Content-Type: ");
	    String type = URLConnection.guessContentTypeFromName(filename);
	    if (type == null) type = "application/octet-stream";
	    write(type);newline();
	    newline();
	    this.out.flush();
	    
	    if(in != null){
		    byte[] buffer = new byte[10 * 1024];
		    for(int len = 0; len >= 0; ){
		    	len = in.read(buffer);
		    	if(len > 0){
		    		//log.info("write len:" + len);
		    		this.out.write(buffer, 0, len);
		    		this.out.flush();
		    	}
		    }
	    }
	}
	
	public String lastStatus(String name){
		return connection.getHeaderField(name);
	}		
	
	public void setParameter(String name, String value) throws IOException {
	    boundary();
	    writeName(name);
	    newline(); newline();
	    write(value);
	    newline();
	}
	
	public void write(byte[] b, int off, int len) throws IOException{
		//System.out.println("write len:" + len);
		this.out.write(b, off, len);
		this.out.flush();
	}
	
	private void boundary() throws IOException {
	    write("--");
	    write(boundary);
	}		
	
	private void writeName(String name) throws IOException {
	    newline();
	    write("Content-Disposition: form-data; name=\"");
	    write(name);
	    write('"');
	}		
	
	protected void write(String s) throws IOException {
		this.out.write(s.getBytes("UTF-8"));		    
	}

	protected void write(char s) throws IOException {
		this.out.write(s);		    
	}	
	
	protected void newline() throws IOException {
	    write("\r\n");
	}		

	protected static String randomString() {
		Random random = new Random();
		return Long.toString(random.nextLong(), 36);
	}		
}
