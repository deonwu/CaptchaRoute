package org.http.channel.client.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.http.channel.client.gui.xui.XUIContainer;

public class LoginPanel extends JPanel{
	public void initPanel(final XUIContainer xui){
		this.setLayout(new BorderLayout());	
        final JTextField name = new JTextField("");
        final JPasswordField password = new JPasswordField("");
        final JTextField internal = new JTextField("");
        final JTextField proxy = new JTextField("");        
        
        JLabel nameLabel = new JLabel("外部域名: ");
        JLabel passwordLabel = new JLabel("注册密码: ");
        JLabel internalLabel = new JLabel("内部地址: ");
        JLabel proxyLabel = new JLabel("HTTP代理: ");
        
        nameLabel.setLabelFor(name);
        passwordLabel.setLabelFor(password);
        proxyLabel.setLabelFor(proxy);        
        
        JPanel textControlsPane = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        //GridBagConstraints c = new GridBagConstraints();

        textControlsPane.setLayout(gridbag);

        JLabel[] labels = {nameLabel, passwordLabel, internalLabel,  proxyLabel};
        JTextField[] textFields = {name, password, internal, proxy};
        addLabelTextRows(labels, textFields, gridbag, textControlsPane);

        //textControlsPane.add(actionLabel, c);
        textControlsPane.setBorder(BorderFactory.createCompoundBorder(
                                	BorderFactory.createTitledBorder("设置信息"),
                                	BorderFactory.createEmptyBorder(5,5,5,5)));
        
        JPanel buttons = new JPanel();
        
        JButton save = new JButton("保存");
        JButton close = new JButton("取消");
                
        buttons.add(save);
        buttons.add(close);
        
        add(textControlsPane, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        
        //this.addComponentListener(l)
        this.addComponentListener(new ComponentAdapter(){
        	public void componentShown(ComponentEvent e){
        		if(xui.eventQueue != null){
        			xui.eventQueue.fireEvent(EventsHandler.SHOW_SETTINGS, e.getSource());
        		}
        	}
        });
        
        /**
         * 注册GUI控件到XUI.
         */
        xui.addComponent(EventsHandler.REMOTE_DOMAIN, name);
        xui.addComponent(EventsHandler.PROXY_PASSWORD, password);
        xui.addComponent(EventsHandler.INTERNAL_DOMAIN, internal);
        xui.addComponent(EventsHandler.HTTP_PROXY, proxy);
        
        xui.addComponent(EventsHandler.SAVE_SETTINGS, save);
        xui.addComponent(EventsHandler.CLOSE_SETTINGS, close);
	}
	
    private void addLabelTextRows(JLabel[] labels, JTextField[] textFields,
            		GridBagLayout gridbag, Container container) {
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		int numLabels = labels.length;		
		for (int i = 0; i < numLabels; i++) {
			c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
			c.fill = GridBagConstraints.NONE;      //reset to default
			c.weightx = 0.0;                       //reset to default
			container.add(labels[i], c);
			
			c.gridwidth = GridBagConstraints.REMAINDER;     //end row
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			container.add(textFields[i], c);
		}
    }
}
