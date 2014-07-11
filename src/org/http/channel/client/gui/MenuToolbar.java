/*  
  * Copyright [2008] DeonWu@gmail.com 
  *  
  * Licensed under the Apache License, Version 2.0 (the "License");  
  * you may not use this file except in compliance with the License.  
  * You may obtain a copy of the License at  
  *      http://www.apache.org/licenses/LICENSE-2.0  
  * Unless required by applicable law or agreed to in writing, software  
  * distributed under the License is distributed on an "AS IS" BASIS,  
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  
  * See the License for the specific language governing permissions and  
  * limitations under the License. 
  *  
  * $ Name LastChangeRevision LastChangeDate LastChangeBy $ 
  * $Id$ 
  */ 
 
package org.http.channel.client.gui;

import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.KeyStroke;

import org.http.channel.client.gui.events.EventQueue;

public class MenuToolbar {
	public static final String CONNECTION = "Connection";
	public static final String SHOW_STATUS = "ProxyStatus";
	public static final String USER_SETTING = "UserSettings";
	public static final String EXIT = "Exit";
	public static final String ABOUT = "About";	
		
	//private ResourceBundle rb = new SimpleResourceBound();	
	private Map<String, BookAction> actions = new HashMap<String, BookAction>();
	private EventQueue events = null;
	
	public class BookAction extends AbstractAction {
		private static final long serialVersionUID = -6101997393914923387L;
		//private boolean processing = false;
		public Object attachedObject = null;
		public ActionEvent attachedEvent = null;
		
		public BookAction(String name, String icon, int accelerator){
			super(name, icon("org/notebook/gui/images/" + icon));
			if(accelerator > 0){
				this.putValue(ACCELERATOR_KEY, 
						KeyStroke.getKeyStroke(accelerator, ActionEvent.CTRL_MASK));
			}
			this.putValue(Action.ACTION_COMMAND_KEY, name);
			this.putValue(Action.NAME, i18n(name));
		}
		public void actionPerformed(ActionEvent event) {
			//this.SHORT_DESCRIPTION
			if(events != null){
				events.dispatchEvent(event);
			}
		}
		
		/**
		 * 用于系统托盘菜单.
		 * @return
		 */
		public MenuItem toAWTMenu(){
			MenuItem item = new MenuItem();
			item.setName((String)this.getValue(Action.ACTION_COMMAND_KEY));
			item.setLabel((String)this.getValue(Action.ACTION_COMMAND_KEY));
			final AbstractAction action = this; 
			item.addActionListener(new ActionListener(){
				@Override
				public void actionPerformed(ActionEvent e) {
					action.actionPerformed(e);
				}});			
			return item;
		}
	}
	
	public MenuToolbar(EventQueue events){
		this();
		this.events = events;
	}
		
	public void $(String name, String icon, int accelerator){
		actions.put(name, new BookAction(name, icon, accelerator));
	}
	public BookAction $(String name){
		if(!actions.containsKey(name))
			throw new Error("Not found action by name '" + name + "'");
		return actions.get(name);
	}

	public MenuToolbar(){
		$(CONNECTION, "", 0);
		$(USER_SETTING, "", 0);
		$(SHOW_STATUS, "", 0);
		$(EXIT, "", 0);		
		$(ABOUT, "", 0);
	}
	
	/**
	 * 连接设置
	 * 用户设置
	 * @return
	 */
	public JMenuBar getMenuBar(){
		JMenuBar menubar = new JMenuBar();
		JMenu fileMenu = new JMenu(i18n("File"));

		fileMenu.add($(CONNECTION));
		fileMenu.add($(SHOW_STATUS));
		fileMenu.add($(USER_SETTING));
		
		fileMenu.addSeparator();
		fileMenu.add($(EXIT));

		menubar.add(fileMenu);
		
		JMenu aboutMenu = new JMenu(i18n("Help"));
		aboutMenu.add($(ABOUT));		

		menubar.add(fileMenu);
		menubar.add(aboutMenu);
		
		//toolMenu.setName("工具");
		
		//menubar.getMenu(0).getName()		
		return menubar;
	}
		
	public PopupMenu getTrayMenu(){
	    final PopupMenu menu = new PopupMenu();
	    menu.addSeparator();
	    menu.add($(EXIT).toAWTMenu());
	    
		return menu;
	}	

	public static ImageIcon icon(String name){
		ImageIcon icon = null;
		try{
			icon = new ImageIcon(MenuToolbar.class.getClassLoader().getResource(name));
		}catch(Exception e){
			System.out.println("failed to load:" + name);
			//e.printStackTrace();
		}
		return icon;
	}
	
	public String i18n(String key){
		return key;
	}
}

