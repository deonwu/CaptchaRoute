package org.http.channel.server.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.http.channel.server.ProxyServer;

public class CommandServlet extends HttpServlet{
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
    	doPost(request, response);
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
    	//response.getWriter().println("hello command");
    	String cmd = request.getRequestURI();
    	String[] tmps = cmd.split("/");
    	cmd = tmps[tmps.length - 1].toLowerCase().trim();
    	if(cmd.equals("request")){
    		ProxyServer.ins.forwardRequest(request, response);
    	}else if(cmd.equals("reponse")){
    		ProxyServer.ins.doneRequest(request, response);
    	}else if(cmd.equals("status")){
    		ProxyServer.ins.status(request, response);
    	}else if(cmd.equals("stop")){
    		//停止系统。
    		LogFactory.getLog("gate").warn("System shut down by '/~/stop'");
    		response.getWriter().println("Shut down");
    		System.exit(0);
    	}else {
    		ProxyServer.ins.welcome(request, response);
    	}
    }
}
