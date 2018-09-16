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
import java.util.Timer;
import java.math.*;
import java.awt.*;
import javax.swing.*;

import game.Game;

// playback
public class Playback {
	private final long minDelay = 1000;
	private boolean paused = false;
	private ArrayList<Long> times = new ArrayList<Long>();
	private ArrayList<Game> games = new ArrayList<Game>();
	private DataInputStream input;
	private String[] username = new String[2];
	private GameUI gameUI;
	private int current = 0;
	private NextGameTimerTask nextGameTimerTask;
	private Timer timer = new Timer();
	
	public Playback(JFrame parent, File file) {
		// load the archive file
		try {
			input = new DataInputStream(new FileInputStream(file));
			
			for (int i = 0; i < 2; ++i) {
				int length = input.readInt();
				byte[] bytes = new byte[length];
				input.read(bytes);
				username[i] = new String(bytes);
			}
			
			while (true) {
				try {
					long time = input.readLong();
					int length = input.readInt();
					byte[] bytes = new byte[length];
					input.read(bytes);
					games.add(new Game(bytes));
					times.add(time);
				} catch (EOFException e) {
					break;
				}
			}
			
			if (games.isEmpty())
				throw new Exception("Empty archive file!");
			
		} catch (Exception e) {
	        JOptionPane.showMessageDialog(
	        		null, 
	        		"Unable to load the archive!",
	        		"Invalid archive file", JOptionPane.ERROR_MESSAGE
    		);
	        parent.setVisible(true);
		}
		
		gameUI = new GameUI(parent, games.get(0).getSize(), null, true);
		gameUI.setUsername(username[0], username[1]);
		gameUI.resetProgress(games.size());
		gameUI.sync(games.get(0));
		gameUI.setListener(new GameUIHandler());
		gameUI.setVisible(true);
		
		scheduleNext();
	}
	
	// schedule the next game state timer task
	private void scheduleNext() {
		if (current + 1 < games.size()) {
			long delay = (long)((times.get(current + 1) - times.get(current)) * gameUI.getDelay());
			timer.schedule(
				nextGameTimerTask = new NextGameTimerTask(), 
				Math.min(minDelay, delay)
			);
		}
		else {
	        JOptionPane.showMessageDialog(
	        		null, 
	        		"Game finished!",
	        		"End", JOptionPane.INFORMATION_MESSAGE
    		);	
	        gameUI.end();
	        paused = true;
		}
	}
	
	// next game state timer task
	class NextGameTimerTask extends TimerTask {
		public void run() {
			++current;
			if (current < games.size()) {
				gameUI.setProgress(current);
				gameUI.sync(games.get(current));
			}
			scheduleNext();
		}
	}
	
	// listen to actions from the UI
	class GameUIHandler implements GameUIListener {
		public void turn(boolean isRight) {}
		public void sendMessage(String time, String message) {}
		public void close() {}		
		
		public void requestPause() {
			nextGameTimerTask.cancel();
			gameUI.pause();
			paused = true;
		}
		
		public void requestResume() {
			if (current + 1 == games.size()) {
				gameUI.setProgress(current = 0);
				gameUI.sync(games.get(0));
			}
			scheduleNext();
			gameUI.resume();
			paused = false;
		}
		
		public void changeProgress(int value) {
			if (current == value) return;
			synchronized (this) {
				nextGameTimerTask.cancel();
				current = value;
				gameUI.sync(games.get(current));
				if (!paused) scheduleNext();
			}
		}
		
		public void changeSpeed(int value) {
			synchronized (this) {
				nextGameTimerTask.cancel();
				if (!paused) scheduleNext();
			}
		}
	}	
}
