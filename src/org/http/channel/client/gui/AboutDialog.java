package org.http.channel.client.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.http.channel.Version;

public class AboutDialog extends JDialog {
	private String name = "";
	public AboutDialog(JFrame parent){
		super(parent, true);
		this.name = Version.getName();
		this.setTitle("关于 " + name);
		setContentPane(createAboutJPanel());
		//this.setPreferredSize(new Dimension(340, 200));
		setMinimumSize(new Dimension(340, 150));
		this.pack();
		this.setResizable(false);
	}
	
	/**
	 * 
	 * 	 * @return
	 */
	private JPanel createAboutJPanel(){
		JPanel p = new JPanel(new BorderLayout());
		
		JPanel aboutPanel = new JPanel(new BorderLayout());
		aboutPanel.setBackground(Color.white);
        final JLabel logo = new JLabel("");
        logo.setPreferredSize(new Dimension(32, 32));
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(Color.white);
        logoPanel.setPreferredSize(new Dimension(64, 32));
        logoPanel.add(logo, BorderLayout.CENTER); 
        aboutPanel.add(logoPanel, BorderLayout.WEST); 
        //logo.setIcon(NoteBookApp.icon("org/notebook/gui/images/application.png"));
        
        final JTextArea about = new JTextArea("");
        about.setEditable(false);
        final String aboutText = "\n" +  
        						 name + " HTTP从外向内翻墙.\n\n" +
        						 "版本: " + Version.getVersion() + "\n" +
        						 "开发者： 四无浪子\n" +
        						 "联系方式: wudalong@gmail.com" + 
        						 "\n\n";
        about.setText(aboutText);
        //logo.setPreferredSize(new Dimension(32, 32));
        //logo.setBorder(BorderFactory.createEtchedBorder());\
        about.setFont(new Font("Courier", Font.PLAIN, 14));
        aboutPanel.add(about, BorderLayout.CENTER); 
        
        
        JPanel buttons = new JPanel();
        JButton close = new JButton("OK");
        buttons.add(close);
        
        p.add(buttons, BorderLayout.NORTH);
        
        p.add(aboutPanel, BorderLayout.CENTER);
        p.add(buttons, BorderLayout.SOUTH);
        
        final JDialog dailog = this;
        close.addActionListener(
        		new ActionListener(){
        			@Override
        			public void actionPerformed(ActionEvent e) {
        				dailog.setVisible(false);
        				dailog.dispose();        				
        			}
		});
		
		return p;
	}
	
	   private static void createAndShowGUI() {
	        //Create and set up the window.
	        JFrame frame = new JFrame("TextSamplerDemo");
	        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);	        

	        //Add content to the window.
	        final AboutDialog s = new AboutDialog(frame);
	        JButton a = new JButton("test");
	        
	        a.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e) {
					s.setVisible(true);				
				}});
	        frame.setLayout(new FlowLayout());
	        frame.add(a);

	        frame.setPreferredSize(new Dimension(400, 150));
	        //Display the window.
	        frame.pack();
	        frame.setVisible(true);
	    }
	    
	    public static void main(String[] args) {
	        //Schedule a job for the event dispatching thread:
	        //creating and showing this application's GUI.
	    	//MsgBox message = new MsgBox(null, "Hey you user, are you sure ?", true);
	    	//Dialog.
			//System.setProperty("http.proxyHost", "10.144.1.10");
			//System.setProperty("http.proxyPort", "8080");
	    	
	        SwingUtilities.invokeLater(new Runnable() {
	            public void run() {
	                 //Turn off metal's use of bold fonts
	            	//UIManager.put("swing.boldMetal", Boolean.FALSE);
					createAndShowGUI();
	            }
	        });
	    }   	

}
