/***********************************************************************
    Snake
    
    Copyright (c) 2018 Zhouxing Shi <zhouxingshichn@gmail.com>
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
**********************************************************************/ 

package server;
import java.io.*;
import java.util.*;
import java.math.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import utils.Colors;
import utils.Layout;
import utils.Time;

// UI for the server
public class ServerUI extends JFrame {
	private JFrame parent;
	private ServerPanel panel;
	private Server server;
	private JFrame _this;
	
	public ServerUI(JFrame parent, Server server) {
		this.parent = parent;
		this.server = server;
		_this = this;
		setContentPane(panel = new ServerPanel());
		setMinimumSize(new Dimension(550, 400));
		pack();
		setTitle("Server");
		setLocationRelativeTo(null);
		addWindowListener(new WindowAdapter() {
			@Override
		    public void windowClosing(WindowEvent windowEvent) {
				close();
		    }
		});		
	}
	
	// close
	private void close() {
		_this.setVisible(false);
		server.close();
		parent.setVisible(true);		
	}
	
	public void failedToCreate() {
		display("Failed to create the server");
	}
	
	class ServerPanel extends JPanel {
		JTextArea textAreaStatus;
		JButton buttonRestart, buttonClose;
		
		public ServerPanel() {
			setLayout(new GridBagLayout());
			setBorder(new EmptyBorder(15, 15, 15, 15));
			textAreaStatus = new JTextArea();
			textAreaStatus.setEditable(false);
			JScrollPane scroll = new JScrollPane(textAreaStatus);
			scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			add(scroll, Layout.getConstraints(0, 0, 1, 1, 1, 1));
			JPanel buttons = new JPanel();
			buttons.setOpaque(false);
			buttons.setLayout(new GridBagLayout());
			buttonRestart = new JButton("Restart");
			buttonClose = new JButton("Close");
			buttons.add(buttonRestart, Layout.getConstraints(0, 0, 1, 1, 1, 1));
			buttons.add(buttonClose, Layout.getConstraints(1, 0, 1, 1, 1, 1));
			add(buttons, Layout.getConstraints(0, 1, 1, 1, 0, 0, new Insets(5, 0, 0, 0)));
			buttonRestart.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					server.restart();
				}
			});			
			buttonClose.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					close();
				}
			});
		}
	}
	
	// display a message
	public void display(String message) {
		message = "[" + Time.getTime() + "]: " + message;
		synchronized (panel.textAreaStatus) {
			panel.textAreaStatus.setText(panel.textAreaStatus.getText() + message + "\n");
		}
	}
}
