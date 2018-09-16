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

import java.io.*;
import java.util.*;
import java.math.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import client.GameUI;
import client.Music;
import client.Playback;
import server.ServerUI;
import utils.Colors;
import utils.Font;
import utils.Layout;

// UI of the landing page
public class SnakeUI extends JFrame{
	private SnakeUI home;
	
	public SnakeUI() {
		setContentPane(new SnakePanel());		
		pack();
		setTitle("Snake");
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(370, 520));
		setPreferredSize(new Dimension(370, 520));
		home = this;
	}

	class SnakePanel extends JPanel {
		public SnakePanel() {
			setLayout(new GridBagLayout());
			setBackground(Colors.BACKGROUND_LIGHT);
			setBorder(new EmptyBorder(20, 50, 40, 50));
			add(new SnakeContainer(), Layout.getConstraints(0, 0, 1, 1, 1, 0));
		}
	}
	
	class SnakeContainer extends JPanel {
		public SnakeContainer() {
			setLayout(new GridBagLayout());
			setOpaque(false);
			Insets insets = new Insets(0, 0, 0, 0);
			add(renderTitle(), Layout.getConstraints(0, 0, 1, 1, 1, 0, insets));
			add(new SnakeButtons(), Layout.getConstraints(0, 1, 1, 1, 1, 1, insets));
		}
		
		private JLabel renderTitle() {
			JLabel title = new JLabel("Snake");
			title.setHorizontalAlignment(SwingConstants.CENTER);
			title.setForeground(Colors.BORDER);
			title.setFont(new java.awt.Font("Open Sans", Font.BOLD, 85));
			return title;
		}
	}
	
	// buttons
	class SnakeButtons extends JPanel {
		JLabel buttonCreate, buttonJoin, buttonPlayback, buttonExit;
		
		public SnakeButtons() {
			setLayout(new GridBagLayout());
			setOpaque(false);
			Insets insets = new Insets(10, 0, 15, 0);
			add(buttonCreate = renderButton("Create Game"), Layout.getConstraints(0, 0, 1, 1, 1, 1, insets));
			add(buttonJoin = renderButton("Join Game"), Layout.getConstraints(0, 1, 1, 1, 1, 1, insets));
			add(buttonPlayback = renderButton("Playback"), Layout.getConstraints(0, 2, 1, 1, 1, 1, insets));
			add(buttonExit = renderButton("Exit"), Layout.getConstraints(0, 3, 1, 1, 1, 1, insets));
			buttonCreate.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					home.setVisible(false);
					CreateUI createUI = new CreateUI(home);
					createUI.setVisible(true);
				}
			});		
			buttonJoin.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					home.setVisible(false);
					JoinUI joinUI = new JoinUI(home);
					joinUI.setVisible(true);
				}
			});		
			buttonPlayback.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (!new File(".archive").exists())
						new File(".archive").mkdir();
					JFileChooser fileChooser = new JFileChooser(new File(".archive"));
					fileChooser.setFileFilter(new FileNameExtensionFilter("snake archive file", "snake"));				
			        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			        	File file = fileChooser.getSelectedFile();
			        	home.setVisible(false);
			        	new Playback(home, file);
			        }
				}
			});	
			buttonExit.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					System.exit(0);
				}
			});
		}
		
		private JLabel renderButton(String text) {
			JLabel button = new JLabel(text);
			button.setOpaque(true);
			button.setHorizontalAlignment(SwingConstants.CENTER);
			button.setBackground(Colors.BORDER);
			button.setForeground(Color.white);
			button.setBorder(new EmptyBorder(10, 5, 10, 5));
			button.setFont(new Font(28));
			button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			return button;
		}
	}
}