package org.http.channel.client;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.http.channel.Version;
import org.http.channel.client.gui.GUIMain;
import org.http.channel.settings.Settings;

public class Main {
	private Log log = LogFactory.getLog("gate");
	public static final String version = Version.getVersion();
	
	public static final String VERSION = "version";
	public static final String REMOTE = "remote";
	public static final String LOCAL = "local";
	public static final String GUI = "gui";
	
	public static void main(String[] args) throws IOException{
		Options options = new Options();
		options.addOption(GUI, false, "Run with gui mode.");
		options.addOption(VERSION, false, "show version.");
		options.addOption(REMOTE, true, "the remote URL of proxy.");
		options.addOption(LOCAL, true, "the local URL of proxy.");

		CommandLine cmd = null;
		
		try{
			CommandLineParser parser = new PosixParser();
			cmd = parser.parse(options, args);			
		}catch(ParseException e){
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Gate", options);
			System.exit(-1);
		}
		
		if(cmd.hasOption(VERSION)){
			System.out.println("Gate " + Version.getVersion());
			return;
		}else if(cmd.hasOption(GUI)){
			GUIMain.main(args);
			return;
		}else {
			initLog4jFile("client.log");
			Settings s = new Settings("client");
			s.putSetting(Settings.REMOTE_DOMAIN, cmd.getOptionValue(REMOTE, ""));
			s.putSetting(Settings.INTERNAL_DOMAIN, cmd.getOptionValue(LOCAL, ""));
			new ProxyClient(s).run();
		}

		//System.out.println("Stopped.");
	}
	
	private static void initLog4jFile(String name){
		//LogFactory.getLog("main");
		org.apache.log4j.Logger root = org.apache.log4j.Logger.getRootLogger();
		try {
			root.addAppender(new org.apache.log4j.DailyRollingFileAppender(root.getAppender("S").getLayout(),
					"logs/" + name, 
					".yy-MM-dd"));
		} catch (IOException e) {
			System.out.println("Failed to add file appender.");
			// TODO Auto-generated catch block
		}
		
		root.info(Version.getName() + " " + Version.getVersion());
		root.info("build at " + Version.getBuildDate());
		root.info("java.home:" + System.getProperty("java.home"));
		root.info("java.runtime.version:" + System.getProperty("java.runtime.version"));
		root.info("java.runtime.name:" + System.getProperty("java.runtime.name"));
		
	}	
}
