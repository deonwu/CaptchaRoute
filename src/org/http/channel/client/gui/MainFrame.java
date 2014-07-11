package org.http.channel.client.gui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import org.http.channel.client.gui.xui.XUIContainer;

public class MainFrame extends JFrame{

	public void initPanel(final XUIContainer xui){
		final JFrame win = this;
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	    addWindowListener(new WindowAdapter(){
	    	public void windowOpened(WindowEvent e){
	    		if(xui.eventQueue != null){
	    			xui.eventQueue.fireEvent(EventsHandler.OPEN_MAINFRAME, 
	    					win);
	    		}	    		
	    	}
	    	
	    	public void windowClosing(WindowEvent e){
	    		//this.windowOpened(e)
	    		if(xui.eventQueue != null){
	    			xui.eventQueue.fireEvent(EventsHandler.HIDDEN_MAINFRAME, 
	    					win);
	    		}
	    	}
	    });	
	}
}
