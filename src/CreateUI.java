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
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import server.Server;
import server.ServerUI;
import utils.Colors;
import utils.Layout;
import utils.Spinner;

// UI for creating a new game
public class CreateUI extends JFrame{
	private JFrame parent;
	private CreatePanel panel;
	
	public CreateUI(JFrame parent) {
		this.parent = parent;
		setContentPane(panel = new CreatePanel());
		pack();
		setMinimumSize(new Dimension(320, 165));
		setTitle("Create Game");
		setLocationRelativeTo(null);
		panel.buttonCreate.requestFocusInWindow();	
		panel.buttonCreate.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				create();
			}
		});
		panel.buttonCancel.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				close();
			}
		});
		KeyListener keyListener = new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				int code = e.getKeyCode();
				if (code == KeyEvent.VK_ENTER)
					create();
			}
		};
		((JSpinner.NumberEditor)panel.spinnerLives.getEditor()).getTextField()
			.addKeyListener(keyListener);
		((JSpinner.NumberEditor)panel.spinnerPort.getEditor()).getTextField()
			.addKeyListener(keyListener);	
		
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
	
	// create a server
	private void create() {
		setVisible(false);				
		int level = panel.comboBoxLevel.getSelectedIndex() + 1;
		int lives = (int)panel.spinnerLives.getValue();
		int port = (int)panel.spinnerPort.getValue();
		int speed = panel.sliderSnakeSpeed.getValue();
		new Server(parent, port, level, lives, speed);		
	}
	
	class CreatePanel extends JPanel {
		JComboBox<String> comboBoxLevel;
		JSpinner spinnerLives, spinnerPort;
		JSlider sliderSnakeSpeed;
		JButton buttonCreate, buttonCancel;
		
		public CreatePanel() {
			setLayout(new GridBagLayout());
			setBorder(new EmptyBorder(10, 20, 10, 20));
			add(renderLevelLives(), Layout.getConstraints(0, 0, 1, 1, 1, 0));
			add(renderSnakeSpeed(), Layout.getConstraints(0, 1, 1, 1, 1, 0));
			add(renderServerPort(), Layout.getConstraints(0, 2, 1, 1, 0, 0));
			add(renderButtons(), Layout.getConstraints(0, 3, 1, 1, 0, 0));
		}
		
		// for setting level and number of lives
		private JPanel renderLevelLives() {
			JPanel container = new JPanel();
			container.setLayout(new GridBagLayout());
			container.add(new JLabel("Level: "), Layout.getConstraints(0, 0, 1, 1, 0, 0));
			comboBoxLevel = new JComboBox<String>();
			for (int i = 1; i <= 10; ++i) 
				comboBoxLevel.addItem("Level " + Integer.toString(i));
			comboBoxLevel.setSelectedIndex(4);
			comboBoxLevel.setBorder(new EmptyBorder(0, 0, 0, 10));
			container.add(comboBoxLevel, Layout.getConstraints(1, 0, 1, 1, 1, 1));
			container.add(new JLabel("Lives (1~100): "), Layout.getConstraints(2, 0, 1, 1, 0, 0));
			spinnerLives = Spinner.getSpinner(5, 1, 100, false);
			container.add(spinnerLives, Layout.getConstraints(3, 0, 1, 1, 0, 0));
			return container;
		}
		
		// for setting snake speed
		private JPanel renderSnakeSpeed() {
			JPanel container = new JPanel();
			container.setLayout(new GridBagLayout());
			container.add(new JLabel("Snake speed: "), Layout.getConstraints(0, 0, 1, 1, 0, 0));
			sliderSnakeSpeed = new JSlider(JSlider.HORIZONTAL, 0, 10, 5);
			container.add(sliderSnakeSpeed, Layout.getConstraints(1, 0, 1, 1, 1, 1));
			return container;
		}
		
		// for setting server port
		private JPanel renderServerPort() {
			JPanel container = new JPanel();
			container.setLayout(new GridBagLayout());
			container.add(new JLabel("Server port (1024~65535): "), Layout.getConstraints(0, 0, 1, 1, 0, 0));
			spinnerPort = Spinner.getSpinner(9000, 1024, 65535, true);
			container.add(spinnerPort, Layout.getConstraints(1, 0, 1, 1, 0, 0));
			return container;
		}
		
		// create/cancel buttons
		private JPanel renderButtons() {
			JPanel container = new JPanel();
			container.setLayout(new GridBagLayout());
			container.add(buttonCreate = new JButton("Create"), Layout.getConstraints(0, 0, 1, 1, 0, 0));
			container.add(buttonCancel = new JButton("Cancel"), Layout.getConstraints(1, 0, 1, 1, 0, 0));
			container.setBorder(new EmptyBorder(10, 0, 0, 0));
			return container;
		}
	}
}
