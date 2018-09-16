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

package game;
import java.io.*;
import java.util.*;
import java.math.*;
import java.nio.ByteBuffer;
import java.awt.*;
import javax.swing.*;

import game.Point;

// ranklist
public class Ranklist {
	private final int numTop = 5;
	private ArrayList<User> users;
	private Comparator<User> comparator;
	
	public Ranklist() {
		users = new ArrayList<User>();
		comparator = new Comparator<User>() {
			public int compare(User a, User b) {
				return b.getScore() - a.getScore();
			}
		};
	}
	
	// decode from a packed ranklist (when receiving the ranklist from the server)
	public Ranklist(byte[] packed) {
		this();
		try {
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(new ByteArrayInputStream(packed)));
			String line = reader.readLine();
			int n = Integer.parseInt(line);
			for (int i = 0; i < n; ++i) {
				String name = reader.readLine();
				int score = Integer.parseInt(reader.readLine());
				add(name, score);
			}
		} catch (Exception e) {}
	}
	
	public ArrayList<User> getUsers() {
		return users;
	}
	
	// encode the ranklist (used before sending the ranklist to clients)
	public byte[] pack() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(Integer.toString(users.size()) + "\n");
		for (int i = 0; i < users.size(); ++i) {
			stringBuffer.append(users.get(i).getName() + "\n");
			stringBuffer.append(Integer.toString(users.get(i).getScore()) + "\n");
		}
		byte[] bytes = stringBuffer.toString().getBytes();
		ByteBuffer buffer = ByteBuffer.allocate(bytes.length + 4);
		buffer.putInt(bytes.length);
		buffer.put(bytes);
		return buffer.array();
	}
	
	// add the score of a player
	public void add(String name, int score) {
		boolean added = false;
		for (int i = 0; i < users.size(); ++i)
			if (users.get(i).getName().equals(name)) {
				users.get(i).setScore(Math.max(users.get(i).getScore(), score));
				added = true;
				break;
			}
		if (!added)
			users.add(new User(name, score));
		users.sort(comparator);
		while (users.size() > numTop) 
			users.remove(numTop);
	}
	
	// read from local file
	public void read(String filename) {
		try {
			BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(filename)));
			String line = reader.readLine();
			int n = Integer.parseInt(line);
			for (int i = 0; i < n; ++i) {
				String name = reader.readLine();
				int score = Integer.parseInt(reader.readLine());
				add(name, score);
			}
			reader.close();
		} catch (IOException e) {}
	}
	
	// write the ranklist to a local file
	public void write(String filename) {
		try {
			BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(filename)));
			writer.write(Integer.toString(users.size()) + "\n");
			for (int i = 0; i < users.size(); ++i) {
				writer.write(users.get(i).getName() + "\n");
				writer.write(Integer.toString(users.get(i).getScore()) + "\n");
			}
			writer.close();
		} catch (IOException e) {}
	}
}


