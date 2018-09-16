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

import client.WaitingUI.WaitingPanel;
import game.Ranklist;
import game.User;
import utils.Colors;
import utils.Layout;
import utils.Font;

// UI for the ranklist
public class RanklistUI extends JFrame {
	private Ranklist ranklist;
	private JPanel body;
	
	public RanklistUI(Ranklist ranklist) {
		this.ranklist = ranklist;
		setContentPane(new RanklistPanel());
		setMinimumSize(new Dimension(360, 325));
		pack();
		setTitle("Ranklist");
		setLocationRelativeTo(null);	
	}
	
	class RanklistPanel extends JPanel {
		public RanklistPanel() {
			setLayout(new GridBagLayout());
			setBackground(Colors.BACKGROUND_LIGHT);
			setBorder(new EmptyBorder(20, 20, 20, 20));
			add(renderTitle(), Layout.getConstraints(0, 0, 1, 1, 1, 0, new Insets(0, 0, 10, 0)));
			ArrayList<User> users = ranklist.getUsers();
			body = new JPanel();
			body.setOpaque(false);
			body.setLayout(new GridBagLayout());
			body.add(
				renderText("#"), 
				Layout.getConstraints(0, 0, 1, 1, 0, 1, new Insets(5, 10, 5, 10)));
			body.add(
				renderText("Username"), 
				Layout.getConstraints(1, 0, 1, 1, 1, 1, new Insets(5, 10, 5, 10)));
			body.add(
				renderText("Score"), 
				Layout.getConstraints(2, 0, 1, 1, 0, 1, new Insets(5, 10, 5, 10)));
			for (int i = 0; i < users.size(); ++i) {
				body.add(
					renderText(Integer.toString(i + 1)), 
					Layout.getConstraints(0, i + 1, 1, 1, 0, 1, new Insets(5, 10, 5, 10)));
				body.add(
					renderText(users.get(i).getName()), 
					Layout.getConstraints(1, i + 1, 1, 1, 1, 1, new Insets(5, 10, 5, 10)));
				body.add(
					renderText(Integer.toString(users.get(i).getScore())), 
					Layout.getConstraints(2, i + 1, 1, 1, 0, 1, new Insets(5, 10, 5, 10)));
			}
			add(body, Layout.getConstraints(0, 1, 1, 1, 1, 0));
		}
		
		private JLabel renderText(String text) {
			JLabel label = new JLabel(text);
			label.setOpaque(false);
			label.setFont(new Font(20));
			label.setForeground(Colors.BORDER);
			label.setHorizontalAlignment(SwingConstants.CENTER);
			return label;
		}
		
		private JLabel renderTitle() {
			JLabel title = new JLabel("RANKLIST");
			title.setFont(new Font(32));
			title.setForeground(Colors.BORDER);
			title.setOpaque(false);
			title.setHorizontalAlignment(SwingConstants.CENTER);
			return title;
		}
	}
}
