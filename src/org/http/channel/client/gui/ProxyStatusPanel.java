package org.http.channel.client.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.http.channel.client.gui.xui.XUIContainer;

/**
 * Internet: http://www.deonwu84.com:8080
 * Internal: http://www.deonwu84.com:8080
 * Requested: xxx
 * Active user: xxx
 * Updated: xxxx
 * @author deon
 *
 */
public class ProxyStatusPanel extends JPanel {

	public void initPanel(final XUIContainer xui){
		this.setLayout(new BorderLayout());	
        final JTextField name = new JTextField("");
        final JTextField internal = new JTextField("");
        final JTextField requestCount = new JTextField("");
        final JTextField activeUser = new JTextField("");
        final JTextField updated = new JTextField("");
        
        JLabel nameLabel = new JLabel("外部域名: ");
        JLabel internalLabel = new JLabel("内部地址: ");
        JLabel proxyLabel = new JLabel("转发次数: ");
        JLabel userLabel = new JLabel("活动用户: ");
        JLabel updatedLabel = new JLabel("状态: ");
        
        name.setEditable(false);
        internal.setEditable(false);
        requestCount.setEditable(false);
        activeUser.setEditable(false);
        updated.setEditable(false);
                
        JPanel textControlsPane = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        //GridBagConstraints c = new GridBagConstraints();

        textControlsPane.setLayout(gridbag);

        JLabel[] labels = {nameLabel, internalLabel,  proxyLabel, userLabel, updatedLabel};
        JTextField[] textFields = {name, internal, requestCount, activeUser, updated};
        addLabelTextRows(labels, textFields, gridbag, textControlsPane);

        //textControlsPane.add(actionLabel, c);
        /*
        textControlsPane.setBorder(BorderFactory.createCompoundBorder(
                                	BorderFactory.createTitledBorder("设置信息"),
                                	BorderFactory.createEmptyBorder(5,5,5,5)));
        */
        add(textControlsPane, BorderLayout.CENTER);
        
        //this.addComponentListener(l)
        this.addComponentListener(new ComponentAdapter(){
        	public void componentShown(ComponentEvent e){
        		if(xui.eventQueue != null){
        			xui.eventQueue.fireEvent(EventsHandler.SHOW_STATUS, e.getSource());
        		}
        	}
        });        
        /**
         * 注册GUI控件到XUI.
         */
        xui.addComponent(EventsHandler.STATUS_REMOTE, name);
        xui.addComponent(EventsHandler.STATUS_LOCAL, internal);        
        xui.addComponent(EventsHandler.STATUS_REQUEST, requestCount);
        xui.addComponent(EventsHandler.STATUS_ACTIVE_USER, activeUser);
        xui.addComponent(EventsHandler.STATUS_UPDATED, updated);
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
