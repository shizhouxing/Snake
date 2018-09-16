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
import javax.swing.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import game.Game;
import game.Ranklist;
import server.InformationServer;
import utils.Time;

// client
public class Client {
	private final int connectionTimeout = 5000;
	private SocketHandler socketHandler;
	private GameUI gameUI = null;
	private Game game;
	private Music music;
	private boolean isTurning = false;
	private boolean started = false;
	private boolean finished = false;
	private boolean connected = false;
	private WaitingUI waitingUI;
	private JFrame home;
	private String[] username = new String[2];
	private long startTime;
	private DataOutputStream saver;
	
	// create a client and try connecting to the server
	public Client(JFrame home, JFrame parent, String username, String address, int port, Music music) {
		this.home = home;
		this.username[0] = username;
		this.music = music;
		
		waitingUI = new WaitingUI(parent);
		waitingUI.setVisible(true);
		
		new Thread() {
			public void run() {
				try {
					joinGame(address, port);
				} catch (Exception e) {
					waitingUI.setVisible(false);
			        JOptionPane.showMessageDialog(
		        		null, 
		        		"Cannot connect to the server!",
		        		"Error", JOptionPane.ERROR_MESSAGE
	        		);
			        parent.setVisible(true);
				}
			}
		}.start();
		
		if (!(new File(".archive/").exists())) 
			new File(".archive/").mkdir();
		
		try {
			File file = new File(".archive/" + Time.getDateTime() + "-" + username + ".snake");
			int cnt = 0;
			while (file.exists()) {
				file = new File(".archive/" + Time.getDateTime() + "-" + username + "-" + cnt + ".snake");
				cnt++;
			}
			saver = new DataOutputStream(new FileOutputStream(file));
		} catch (Exception e) {}
	}
	
	// connect to the server
	void joinGame(String host, int port) throws UnknownHostException, IOException {
		socketHandler = new SocketHandler(new Socket(host, port));
		
		new Thread() {
			public void run() {
				try { sleep(connectionTimeout);} catch (Exception e) {}
				if (!connected) {
					waitingUI.setVisible(false);
			        JOptionPane.showMessageDialog(
			        		null, 
			        		"Cannot connect to the server!",
			        		"Timeout", JOptionPane.ERROR_MESSAGE
	        		);
			        home.setVisible(true);
				}
			}
		}.start();
		
		socketHandler.start();
	}
	
	// prepare the UI for the game
	void prepareGame() {
		gameUI = new GameUI(home, game.getSize(), music, false);
		gameUI.setListener(new GameUIHandler());
		refresh(game);
	}
	
	// refresh the game UI with the new game state
	void refresh(Game game) {
		this.game = game;
		if (gameUI != null)
			gameUI.sync(game);
	}
	
	// save
	void save(byte[] packed) {
		if (!started) return;
		try {
			saver.writeLong(System.currentTimeMillis() - startTime);
			saver.writeInt(packed.length);
			saver.write(packed);
		} catch (Exception e) {}
	}
	
	// listen to actions from the UI
	class GameUIHandler implements GameUIListener {
		public void turn(boolean isRight) {
			if (finished || !started || game.isPaused()) return;
			if (isTurning) return;
			isTurning = true;
			socketHandler.turn(isRight);
		}
		
		public void sendMessage(String time, String message) {
			socketHandler.sendMessage(time, message);
		}
		
		public void requestPause() {
			if (!started)
				gameUI.showMessage("Game has not started yet.");
			else if (!finished)
				socketHandler.requestPause();
		}
		
		public void requestResume() {
			if (finished || !started) return;			
			socketHandler.requestResume();
		}
		
		public void close() {
			socketHandler.disconnected = true;
			socketHandler.close();
		}
		
		public void changeProgress(int value) {}
		public void changeSpeed(int value) {}
	}
	
	// socket handler to communicate with the server
	class SocketHandler extends Thread {
		private Socket socket;
		private DataInputStream input;
		private DataOutputStream output;
		private boolean disconnected = false;
		
		public SocketHandler(Socket socket) {
			super();
			this.socket = socket;			
		}
		
		public void close() {
			try { socket.close(); } catch (Exception e) {}
		}
		
		// disconnected
		private void disconnect(Exception e) {
			if (disconnected) return;
			disconnected = true;
			game.pause();
			gameUI.finish();
	        JOptionPane.showMessageDialog(
        		null, 
        		"You have disconnected from the server.",
        		"Connection lost", JOptionPane.ERROR_MESSAGE
    		);
		}
		
		// turn the snake
		public void turn(boolean isRight) {
			new Thread() {
				public void run() {
					try {
						synchronized (output) {
							output.writeInt(InformationClient.TURN.ordinal());
							output.writeInt(isRight ? 1 : 0);
						}
					} catch (IOException e) {
						disconnect(e);
					}
					isTurning = false;
				}
			}.start();
		}
		
		// send a text message
		public void sendMessage(String time, String message) {
			new Thread() {
				public void run() {
					byte[] timeBytes = time.getBytes();
					byte[] messageBytes = message.getBytes();
					try {
						synchronized (output) {
							output.writeInt(InformationClient.MESSAGE.ordinal());
							output.writeInt(timeBytes.length);
							output.write(timeBytes);
							output.writeInt(messageBytes.length);
							output.write(messageBytes);
						}
					} catch (IOException e) {
						disconnect(e);
					}
				}
			}.start();
		}
		
		// send a message when the UI is ready
		private void sendPrepared() {
			try {
				synchronized (output) {
					output.writeInt(InformationClient.PREPARED.ordinal());
				}
			} catch (IOException e) {
				disconnect(e);
			}
		}
		
		// request to pause the game
		public void requestPause() {
			try {
				synchronized (output) {
					output.writeInt(InformationClient.PAUSE.ordinal());
				}
			} catch (IOException e) {
				disconnect(e);
			}
		}
		
		// request to resume the game
		public void requestResume() {
			try {
				synchronized (output) {
					output.writeInt(InformationClient.RESUME.ordinal());
				}
			} catch (IOException e) {
				disconnect(e);
			}
		}		
		
		// listen into messages from the server
		public void run() {
			try {
				input = new DataInputStream(socket.getInputStream());
				output = new DataOutputStream(socket.getOutputStream());
				
				// send username to the server
				byte[] usernameBytes = username[0].getBytes();
				output.writeInt(usernameBytes.length);
				output.write(usernameBytes);
				
				while (true) {
					int type = input.readInt();
					if (type == InformationServer.CONNECTED.ordinal()) {
						// connected to the server
						connected = true;
						waitingUI.setText("Waiting for your opponent");
						prepareGame();
						sendPrepared();
					}
					else if (type == InformationServer.ENTER.ordinal()) {
						// enter the game
						waitingUI.setVisible(false);
						int lengthUsernameMine = input.readInt();
						byte[] usernameMineBytes = new byte[lengthUsernameMine];
						input.read(usernameMineBytes, 0, lengthUsernameMine);
						int lengthUsernameOpponent = input.readInt();
						byte[] usernameOpponentBytes = new byte[lengthUsernameOpponent];
						input.read(usernameOpponentBytes, 0, lengthUsernameOpponent);
						username[0] = new String(usernameMineBytes);
						username[1] = new String(usernameOpponentBytes);
						gameUI.setUsername(username[0], username[1]);
						gameUI.setVisible(true);
					}
					else if (type == InformationServer.COUNTDOWN.ordinal()) {
						// show countdown
						int countdown = input.readInt();
						gameUI.showCountdown(countdown);
						System.out.println("Countdown " + countdown);
					}
					else if (type == InformationServer.START.ordinal()) {
						// start the game
						System.out.println("Start");
						gameUI.start();
						started = true;
						startTime = System.currentTimeMillis();
						
						// save usernames
						for (int i = 0; i < 2; ++i) {
							byte[] bytesUsername = username[i].getBytes();
							saver.writeInt(bytesUsername.length);
							saver.write(bytesUsername);
						}
						
						byte[] packedWithLength = game.pack(0);
						byte[] packed = new byte[packedWithLength.length - 4];
						for (int i = 4; i < packedWithLength.length; ++i)
							packed[i - 4] = packedWithLength[i];
						save(packed);
					}
					else if (type == InformationServer.REFRESH.ordinal()) {
						// refresh the game state
						int length = input.readInt();
						byte[] packed = new byte[length];
						input.read(packed, 0, length);
						try {
							refresh(new Game(packed));
							save(packed);
						} catch (Exception e) {}
					}
					else if (type == InformationServer.DIE.ordinal()) {
						// died
						gameUI.showMessage(
							"System", Time.getTime(), 
							"Your current snake has died. A new one will go out of some hole in " +
							Integer.toString(Game.reviveSnakeDelay / 1000) + " seconds."
						);
					}
					else if (type == InformationServer.MESSAGE.ordinal()) {
						// receive a text message
						int lengthUsername = input.readInt();
						usernameBytes = new byte[lengthUsername];
						input.read(usernameBytes, 0, lengthUsername);
						int lengthTime = input.readInt();
						byte[] timeBytes = new byte[lengthTime];
						input.read(timeBytes, 0, lengthTime);
						int lengthMessage = input.readInt();	
						byte[] messageBytes = new byte[lengthMessage];
						input.read(messageBytes, 0, lengthMessage);
						gameUI.showMessage(
							new String(usernameBytes), 
							new String(timeBytes), 
							new String(messageBytes)
						);
					}			
					else if (type == InformationServer.PAUSE.ordinal()) {
						// pause the game
						gameUI.pause();
					}
					else if (type == InformationServer.RESUME.ordinal()) {
						// resume the game
						gameUI.resume();
					}
					else if (type == InformationServer.FINISH.ordinal() ) {
						// game finished
						int winner = input.readInt();
						finished = true;
						new Thread() {
							public void run() {
								if (winner == 0) {
							        JOptionPane.showMessageDialog(
							        		null, 
							        		"Congratulations!\nYou win the game!",
							        		"You win", JOptionPane.INFORMATION_MESSAGE
					        		);
								}
								else {
							        JOptionPane.showMessageDialog(
							        		null, 
							        		"Unfortunately...\nYou lose the game!",
							        		"You lose", JOptionPane.INFORMATION_MESSAGE
					        		);
								}								
							}
						}.start();
						
						saver.close();
						
						gameUI.showMessage(
							"System", Time.getTime(),
							"Game winner: " + username[winner]
						);
						gameUI.finish();
					}
					else if (type == InformationServer.RANKLIST.ordinal() ) {
						// receive the ranklist
						int length = input.readInt();
						byte[] packed = new byte[length];
						input.read(packed, 0, length);
						gameUI.setRanklist(new Ranklist(packed));
					}
				}
			} catch (IOException e) {
				disconnect(e);
			}
		}
	}
}
