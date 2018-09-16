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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import client.Client;
import client.Music;
import utils.Layout;
import utils.Spinner;

// UI for joining a game
public class JoinUI extends JFrame {
	final private String MUSIC_PATH_SETTING = ".music";
	private JFrame parent;
	private JoinPanel panel;
	private JFrame _this;
	private Music music = null;
	
	public JoinUI(JFrame parent) {
		this.parent = parent;
		_this = this;
		setContentPane(panel = new JoinPanel());
		pack();
		setTitle("Join Game");
		setMinimumSize(new Dimension(500, 165));
		setLocationRelativeTo(null);
		panel.buttonJoin.requestFocusInWindow();
		panel.buttonJoin.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				join();
			}
		});
		panel.buttonCancel.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				close();
			}
		});
		panel.buttonChooseMusic.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileFilter(new FileNameExtensionFilter("wav music file", "wav"));				
		        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
		        	File file = fileChooser.getSelectedFile();
		        	music = new Music(file);
		        	if (!music.isValid()) {
		    	        JOptionPane.showMessageDialog(
		    	        		null, 
		    	        		"This music file is invalid!",
		    	        		"Invalid music file", JOptionPane.ERROR_MESSAGE
		        		);
		    	        music = null;
		    	        return;
		        	}
		        	panel.textFieldMusic.setText(file.getName());
		        	setMusicPath(file.getPath());
		        }
			}
		});
		panel.buttonClearMusic.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				setMusicPath("");
				panel.textFieldMusic.setText("");
				music = null;
			}
		});
		KeyListener keyListener = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if (code == KeyEvent.VK_ENTER)
					join();
			}
		};
		panel.textFieldMusic.addKeyListener(keyListener);
		panel.textFieldUsername.addKeyListener(keyListener);
		panel.textFieldServerAddress.addKeyListener(keyListener);
		((JSpinner.NumberEditor)panel.spinnerServerPort.getEditor()).getTextField()
			.addKeyListener(keyListener);	
		
		// loaded previously saved music path 
		try {
			BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(MUSIC_PATH_SETTING)));
			String line = reader.readLine();
			reader.close();
			File file = new File(line);
			music = new Music(file);
			if (!music.isValid()) {
				music = null;
				panel.textFieldMusic.setText("");
			}
			else
				panel.textFieldMusic.setText(file.getName());
		} catch (IOException e) {}
		
		addWindowListener(new WindowAdapter() {
			@Override
		    public void windowClosing(WindowEvent windowEvent) {
				close();
		    }
		});		
	}
	
	// cancel and close the window
	private void close() {
		setVisible(false);
		parent.setVisible(true);		
	}
	
	// set music path
	private void setMusicPath(String path) {		
		try {
			BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(MUSIC_PATH_SETTING)));
			writer.write(path + "\n");
			writer.close();
		} catch (IOException e) {}    	
	}
	
	// join the game
	private void join() {
		String username = panel.textFieldUsername.getText();
		if (username.length() == 0) {
	        JOptionPane.showMessageDialog(
	        		null, 
	        		"Username should not be empty!",
	        		"Invalid username", JOptionPane.ERROR_MESSAGE
    		);
	        return;
		}
		else if (username.equals("System")) {
	        JOptionPane.showMessageDialog(
	        		null, 
	        		"Username should not be \"System\"!",
	        		"Invalid username", JOptionPane.ERROR_MESSAGE
    		);
	        return;
		}
		setVisible(false);
		String address = panel.textFieldServerAddress.getText();
		int port = (int)panel.spinnerServerPort.getValue();
		new Client(parent, _this, username, address, port, music);		
	}
	
	class JoinPanel extends JPanel {
		JTextField textFieldUsername, textFieldServerAddress, textFieldMusic;
		JSpinner spinnerServerPort;
		JButton buttonChooseMusic, buttonClearMusic, buttonJoin, buttonCancel;
		
		public JoinPanel() {
			setLayout(new GridBagLayout());
			setBorder(new EmptyBorder(10, 20, 10, 20));
			add(renderUsername(), Layout.getConstraints(0, 0, 1, 1, 1, 0));
			add(renderServer(), Layout.getConstraints(0, 1, 1, 1, 1, 0));
			add(renderMusic(), Layout.getConstraints(0, 2, 1, 1, 1, 0));
			add(renderButtons(), Layout.getConstraints(0, 3, 1, 1, 0, 0));
		}
		
		// for setting username
		private JPanel renderUsername() {
			JPanel container = new JPanel();
			container.setLayout(new GridBagLayout());
			container.add(new JLabel("Username: "), Layout.getConstraints(0, 0, 1, 1, 0, 0));
			textFieldUsername = new JTextField();
			container.add(textFieldUsername, Layout.getConstraints(1, 0, 1, 1, 1, 1));
			return container;
		}
		
		// for setting server address and port
		private JPanel renderServer() {
			JPanel container = new JPanel();
			container.setLayout(new GridBagLayout());
			container.add(new JLabel("Server address: "), Layout.getConstraints(0, 0, 1, 1, 0, 0));
			textFieldServerAddress = new JTextField("127.0.0.1");
			textFieldServerAddress.setHorizontalAlignment(SwingConstants.CENTER);
			container.add(textFieldServerAddress, Layout.getConstraints(1, 0, 1, 1, 1, 1));
			container.add(new JLabel("Port: "), Layout.getConstraints(2, 0, 1, 1, 0, 0, new Insets(0, 10, 0, 0)));
			spinnerServerPort = Spinner.getSpinner(9000, 1024, 65535, true);
			container.add(spinnerServerPort, Layout.getConstraints(3, 0, 1, 1, 0, 0));
			return container;
		}
		
		// for setting background music
		private JPanel renderMusic() {
			JPanel container = new JPanel();
			container.setLayout(new GridBagLayout());
			container.add(new JLabel("Background music: "), Layout.getConstraints(0, 0, 1, 1, 0, 0));
			textFieldMusic = new JTextField();
			textFieldMusic.setEditable(false);
			container.add(textFieldMusic, Layout.getConstraints(1, 0, 1, 1, 1, 1));
			buttonChooseMusic = new JButton("Choose");
			container.add(buttonChooseMusic, Layout.getConstraints(2, 0, 1, 1, 0, 0));
			buttonClearMusic = new JButton("Clear");
			container.add(buttonClearMusic, Layout.getConstraints(3, 0, 1, 1, 0, 0));
			return container;
		}
		
		// join/cancel buttons
		private JPanel renderButtons() {
			JPanel container = new JPanel();
			container.setLayout(new GridBagLayout());
			container.add(buttonJoin = new JButton("Join"), Layout.getConstraints(0, 0, 1, 1, 0, 0));
			container.add(buttonCancel = new JButton("Cancel"), Layout.getConstraints(1, 0, 1, 1, 0, 0));
			container.setBorder(new EmptyBorder(10, 0, 0, 0));
			return container;
		}		
	}
}
