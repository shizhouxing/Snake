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

package client;
import java.io.*;
import java.util.*;
import java.math.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import utils.Layout;
import utils.Colors;
import utils.Font;

// UI for waiting (when connecting to the server or waiting for the other opponent)
public class WaitingUI extends JFrame {
	private WaitingPanel panel;
	
	public WaitingUI(JFrame parent) {
		setContentPane(panel = new WaitingPanel());
		setMinimumSize(new Dimension(500, 100));
		pack();
		setTitle("Client");
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	public void setText(String text) {
		panel.labelText.setText(text);
		revalidate();
		repaint();
	}
	
	class WaitingPanel extends JPanel {
		JLabel labelText;
		
		public WaitingPanel() {
			setLayout(new GridBagLayout());
			setBackground(Colors.BACKGROUND_LIGHT);
			setBorder(new EmptyBorder(10, 20, 0, 20));
			labelText = new JLabel("Connecting to the server");
			labelText.setForeground(Colors.BORDER);
			labelText.setFont(new Font(28));
			labelText.setHorizontalAlignment(SwingConstants.CENTER);
			add(labelText, Layout.getConstraints(0, 0, 1, 1, 1, 1, new Insets(0, 0, 20, 0)));
		}	
	}
	
}
