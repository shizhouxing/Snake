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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;

import client.InformationClient;
import game.Game;
import game.GameListener;
import game.NoEnoughSpaceException;
import game.Ranklist;
import utils.Time;

// server
public class Server {
	private final int size = 25;
	private final int gameStartDelay = 5;
	private final int pauseResumeRequestTimeoutDelay = 10000;
			
	private ServerUI serverUI;
	private Game game;
	private Ranklist ranklist;
	private ServerSocket socketServer;
	private SocketHandler[] socketHandler = new SocketHandler[2];
	private int numPlayersJoined = 0;
	private String[] username = new String[2];
	private boolean[] requestedPauseResume = new boolean[2];
	private Timer timer = new Timer();
	private boolean finished = false;
	private boolean active = false;
	private int port, level, lives, speed;
	
	public Server(JFrame parent, int port, int level, int lives, int speed) {
		this.port = port;
		this.level = level;
		this.lives = lives;
		this.speed = speed;
		
		serverUI = new ServerUI(parent, this);
		serverUI.setVisible(true);
		
		start();
	}
	
	// restart
	public void restart() {
		if (!active) return;
		active = false;
		close();
		serverUI.display("Restarting...");
		numPlayersJoined = 0;
		finished = false;
		username[0] = username[1] = new String("PENDING...");
		requestedPauseResume[0] = requestedPauseResume[1] = false;
		start();
	}

	// create a server and a game
	private void start() {
		username[0] = username[1] = new String("PENDING...");
		requestedPauseResume[0] = requestedPauseResume[1] = false;
		
		ranklist = new Ranklist();
		ranklist.read(".ranklist");		
		
		try {
			String ip = InetAddress.getLocalHost().getHostAddress().toString();
			serverUI.display("Your ip address: " + ip);
		} catch (UnknownHostException e) {}
		
		new Thread() {
			public void run() {
				try {
					int delay = (int)(Math.pow(5, (5 - speed) / 5.) * 200);
					createGame(port, level, lives, delay);	
				} catch (IOException e) {
					if (active) {
						StringWriter error = new StringWriter();
						e.printStackTrace(new PrintWriter(error));
						serverUI.display(error.toString());
						serverUI.failedToCreate();
					}
				}
			}
		}.start();
	}
	
	// close the server
	public void close() {
		for (int i = 0; i < numPlayersJoined; ++i)
			socketHandler[i].close();
		if (socketServer != null)
			try { socketServer.close(); } catch (Exception e) {}
	}
	
	// create a game
	private void createGame(int port, int level, int numSnakes, int driveSnakeDelay) throws IOException{
		game = new Game(size, level, numSnakes, driveSnakeDelay);
		
		game.setListener(new GameListener() {
			public void revive(int player) {
				Thread thread = socketHandler[player].sendSignal(InformationServer.REVIVE);
				try { thread.join(); } catch (Exception e) {}
			}
			
			public void refresh() {
				refreshAll();
			}
			
			public void die(int player) {
				Thread thread = socketHandler[player].sendSignal(InformationServer.DIE);
				try { thread.join(); } catch (Exception e) {}
			}
			
			public void finish(int winner) {
				finishGame(winner);
			}
		});
		
		socketServer = new ServerSocket(port);
		serverUI.display("Server is listening at port " + port);
		Thread[] thread = new Thread[2];
		
		active = true;
		
		// wait for players
		while (numPlayersJoined < 2) {
			socketHandler[numPlayersJoined] = new SocketHandler(
				socketServer.accept(), numPlayersJoined);
			serverUI.display(
				"Player " + (numPlayersJoined + 1) + 
				" (username: " + username[numPlayersJoined] + ") joined the game");
			numPlayersJoined++;
			if (numPlayersJoined == 2) {
				serverUI.display(
					"Both two players have joined the game.\n" + 
					"No more players will be accepted until you restart the server."
				);
			}
		}
		
		// get prepared 
		for (int i = 0; i < 2; ++i) 
			thread[i] = socketHandler[i].getPrepared();
		for (int i = 0; i < 2; ++i) 
			try { thread[i].join(); } catch (Exception e) {}
		
		// send ranklist
		for (int i = 0; i < 2; ++i) 
			thread[i] = socketHandler[i].sendRanklist();
		for (int i = 0; i < 2; ++i) 
			try { thread[i].join(); } catch (Exception e) {}		
		
		// let two players enter the game UI after both two have joined the game
		for (int i = 0; i < 2; ++i)
			thread[i] = socketHandler[i].enterGame(username[i], username[1 - i]);
		for (int i = 0; i < 2; ++i) 
			try { thread[i].join(); } catch (Exception e) {}
		
		// countdown
		new Thread() {
			public void run() {
				int gameStartCountdown = gameStartDelay;
				while (gameStartCountdown > 0) {
					for (int i = 0; i < 2; ++i) 
						thread[i] = socketHandler[i].sendCountdown(gameStartCountdown);
					for (int i = 0; i < 2; ++i) 
						try { thread[i].join(); } catch (Exception e) {}
					try { Thread.sleep(1000); } catch (Exception e) {}
					gameStartCountdown--;
				}
				
				for (int i = 0; i < 2; ++i) 
					thread[i] = socketHandler[i].startGame();
				for (int i = 0; i < 2; ++i) 
					try { thread[i].join(); } catch (Exception e) {}
				
				serverUI.display("Game started.");
			}
		}.start();
		
		// listen to clients
		for (int i = 0; i < 2; ++i)
			socketHandler[i].start();
	}
	
	// refresh the states of both two clients
	private void refreshAll() {
		Thread[] threads = new Thread[numPlayersJoined];
		for (int i = 0; i < numPlayersJoined; ++i) {
			final int idx = i;
			threads[i] = new Thread() {
				public void run() {
					socketHandler[idx].refresh();	
				}
			};
		}
		for (int i = 0; i < numPlayersJoined; ++i) 
			threads[i].start();
		for (int i = 0; i < numPlayersJoined; ++i)
			try { threads[i].join(); } catch (Exception e) {}
	}
	
	// finish the game
	private void finishGame(int winner) {
		for (int i = 0; i < 2; ++i)  
			ranklist.add(username[i], game.score[i]);
		ranklist.write(".ranklist");
		finished = true;
		Thread[] threads = new Thread[2];
		for (int i = 0; i < 2; ++i) {
			final int idx = i;
			threads[i] = new Thread() {
				public void run() {
					if (idx == winner)
						socketHandler[idx].finish(0);
					else 
						socketHandler[idx].finish(1);
				}
			};
		}
		for (int i = 0; i < numPlayersJoined; ++i) 
			threads[i].start();
		for (int i = 0; i < numPlayersJoined; ++i)
			try { threads[i].join(); } catch (Exception e) {}	
		
		serverUI.display("Game finished (Winner: " + username[winner] + ").");		
	}
	
	// socket handler to communicate with the clients
	class SocketHandler extends Thread {
		protected DataInputStream input;
		protected DataOutputStream output;
		private Socket socket;
		private int player;
		private boolean disconnected = false;
		TimerTask pauseResumeRequestTimeoutTask;
		
		public SocketHandler(Socket socket, int player) {
			super();
			this.socket = socket;
			this.player = player;
			try {
				input = new DataInputStream(socket.getInputStream());
				output = new DataOutputStream(socket.getOutputStream());
				int length = input.readInt();
				byte[] usernameBytes = new byte[length];
				input.read(usernameBytes, 0, length);
				username[player] = new String(usernameBytes);
				refresh();
				output.writeInt(InformationServer.CONNECTED.ordinal());		
			} catch (IOException e) {
				disconnect(e);
			}
		}
		
		// close
		public void close() {
			try { socket.close(); } catch (Exception e) {}
		}
		
		// notify the client to enter the game
		public Thread enterGame(String usernameMine, String usernameOpponent) {
			Thread thread = new Thread() {
				public void run() {
					byte[] usernameMineBytes = usernameMine.getBytes();
					byte[] usernameOpponentBytes = usernameOpponent.getBytes();					
					try {
						synchronized (output) {
							output.writeInt(InformationServer.ENTER.ordinal());
							output.writeInt(usernameMineBytes.length);
							output.write(usernameMineBytes);
							output.writeInt(usernameOpponentBytes.length);
							output.write(usernameOpponentBytes);
						}
						
					} catch (IOException e) {
						disconnect(e);
					}
					
					Thread thread = sendMessage(
						"System", Time.getTime(), 
						"Game information:\n" + 
						"Level: " + level + "; Speed: " + speed + "/10"
					);
					try { thread.join(); } catch (Exception e) {}					
					
				}
			};
			thread.start();
			return thread;
		}
		
		// notify the client to show countdown
		public Thread sendCountdown(int countdown) {
			Thread thread = new Thread() {
				public void run() {			
					try {
						synchronized (output) {
							output.writeInt(InformationServer.COUNTDOWN.ordinal());
							output.writeInt(countdown);
						}
						
					} catch (IOException e) {
						disconnect(e);
					}
				}
			};
			thread.start();
			return thread;
		}		
		
		// notify the client to get prepared
		public Thread getPrepared() {
			Thread thread = new Thread() {
				public void run() {
					try {
						while (true) {
							int type = input.readInt();
							if (type == InformationClient.PREPARED.ordinal())
								break;
						}		
					} catch (IOException e) {
						disconnect(e);
					}
				}
			};
			thread.start();
			return thread;
		}
		
		// the client is disconnected
		protected void disconnect(Exception e) {
			if (disconnected) return;
			disconnected = true;
			serverUI.display(
				"Player " + (player + 1) + 
				" (username: " + username[player] + ") has disconnected from the game");
		}
		
		// send a signal to the client
		public Thread sendSignal(InformationServer info) {
			Thread thread = new Thread() {
				public void run() {
					try {
						synchronized (output) {
							output.writeInt(info.ordinal());
						}
					} catch (IOException e) {
						disconnect(e);
					}
				}
			};
			thread.start();
			return thread;
		}
		
		// send the ranklist to the client
		public Thread sendRanklist() {
			Thread thread = new Thread() {
				public void run() {
					byte[] ranklistPacked = ranklist.pack();
					try {
						synchronized (output) {
							output.writeInt(InformationServer.RANKLIST.ordinal());
							output.write(ranklistPacked);
						}
					} catch (IOException e) {
						disconnect(e);
					}
				}
			};
			thread.start();
			return thread;
		}
		
		// refresh the game state of the client
		public void refresh() {
			byte[] gamePacked = game.pack(player);			
			try {
				synchronized (output) {
					output.writeInt(InformationServer.REFRESH.ordinal());
					output.write(gamePacked);
				}
			} catch (IOException e) {
				disconnect(e);
			}
		}
		
		// finish the game
		public void finish(int winner) {			
			try {
				sendRanklist();
				synchronized (output) {
					output.writeInt(InformationServer.FINISH.ordinal());
					output.writeInt(winner);
				}
			} catch (IOException e) {
				disconnect(e);
			}
		}
		
		// start the game
		public Thread startGame() {
			Thread thread = new Thread() {
				public void run() {
					try {
						synchronized (output) {
							output.writeInt(InformationServer.START.ordinal());
						}
					} catch (IOException e) {
						disconnect(e);
					}
					game.startDriver(player);
				}
			};
			thread.start();
			return thread;	
		}
		
		// send a text message
		public Thread sendMessage(String username, String time, String message) {
			Thread thread = new Thread() {
				public void run() {
					byte[] usernameBytes = username.getBytes();
					byte[] timeBytes = time.getBytes();
					byte[] messageBytes = message.getBytes();
					try {
						synchronized (output) {
							output.writeInt(InformationServer.MESSAGE.ordinal());
							output.writeInt(usernameBytes.length);
							output.write(usernameBytes);
							output.writeInt(timeBytes.length);
							output.write(timeBytes);
							output.writeInt(messageBytes.length);
							output.write(messageBytes);
						}
					} catch (IOException e) {
						disconnect(e);
					}
				}
			};
			thread.start();
			return thread;
		}		

		// turn the snake
		private void turn(int player, boolean isRight) {
			if (!game.isAlive(player)) return;
			game.turn(player, isRight);
		}
		
		// receive a text message
		private void receiveMessage(String time, String message) {
			Thread[] threads = new Thread[2];
			threads[0] = socketHandler[player].sendMessage(username[player], time, message);
			threads[1] = socketHandler[1 - player].sendMessage(username[player], time, message);
			for (int i = 0; i < 2; ++i)
				try { threads[i].join(); } catch (Exception e) {}
		}
					
		// listen into messages from the client
		public void run() {
			try {
				while (true) {
					int type = input.readInt();
					// turn the snake
					if (type == InformationClient.TURN.ordinal()) {
						if (finished || game.isPaused()) continue;
						turn(player, input.readInt() > 0);
					}
					// text message
					else if (type == InformationClient.MESSAGE.ordinal()) {
						int lengthTime = input.readInt();
						byte[] timeBytes = new byte[lengthTime];
						input.read(timeBytes, 0, lengthTime);
						int lengthMessage = input.readInt();	
						byte[] messageBytes = new byte[lengthMessage];
						input.read(messageBytes, 0, lengthMessage);
						receiveMessage(new String(timeBytes), new String(messageBytes));
					} 
					// pause the game
					else if (type == InformationClient.PAUSE.ordinal() ) {
						if (game.isPaused()) continue;
						// already requested
						if (requestedPauseResume[player]) {
							Thread thread = socketHandler[player].sendMessage(
								"System", Time.getTime(), "You have already sent a pause request before.");
							try { thread.join(); } catch (Exception e) {}
						}
						// the other player has also requested
						else if (requestedPauseResume[1 - player]) {
							socketHandler[1 - player].pauseResumeRequestTimeoutTask.cancel();
							requestedPauseResume[1 - player] = false;
							game.pause();							
							Thread[] threads = new Thread[2];
							for (int i = 0; i < 2; ++i)
								threads[i] = socketHandler[i].sendSignal(InformationServer.PAUSE);
							for (int i = 0; i < 2; ++i) 
								try { threads[i].join(); } catch (Exception e) {}
							for (int i = 0; i < 2; ++i)
								threads[i] = socketHandler[i].sendMessage(
									"System", Time.getTime(), "Both players agreed to pause the game.");
							for (int i = 0; i < 2; ++i) 
								try { threads[i].join(); } catch (Exception e) {}							
						}
						// ask for the other player's confirmation
						else { 
							requestedPauseResume[player] = true;
							Thread[] threads = new Thread[2];
							threads[0] = socketHandler[1 - player].sendMessage(
								"System", Time.getTime(), 
								username[player] + " has sent you a pause request. If you would like to accept it, "
								+ "please click the pause button in 10 seconds.");
							threads[1] = sendMessage(
								"System", Time.getTime(),
								"Pause request sent."
							);
							for (int i = 0; i < 2; ++i) 
								try { threads[i].join(); } catch (Exception e) {}
							pauseResumeRequestTimeoutTask = new TimerTask() {
								public void run() {
									requestedPauseResume[player] = false;
									Thread[] threads = new Thread[2];
									threads[0] = socketHandler[player].sendMessage(
											"System", Time.getTime(), "Pause request expired.");
									threads[1] = socketHandler[1 - player].sendMessage(
											"System", Time.getTime(), 
											username[player] + "'s pause request has expired.");
									for (int i = 0; i < 2; ++i) 
										try { threads[i].join(); } catch (Exception e) {}
								}
							};
							timer.schedule(pauseResumeRequestTimeoutTask, pauseResumeRequestTimeoutDelay);
						}
					}
					// resume the game
					else if (type == InformationClient.RESUME.ordinal() ) {
						if (!game.isPaused()) continue;
						// already requested
						if (requestedPauseResume[player]) {
							Thread thread = socketHandler[player].sendMessage(
								"System", Time.getTime(), "You have already sent a resuming request before.");
							try { thread.join(); } catch (Exception e) {}
						}
						// the other player has also requested
						else if (requestedPauseResume[1 - player]) { 
							socketHandler[1 - player].pauseResumeRequestTimeoutTask.cancel();
							requestedPauseResume[1 - player] = false;				

							Thread[] threads = new Thread[2];
							for (int i = 0; i < 2; ++i)
								threads[i] = socketHandler[i].sendMessage(
									"System", Time.getTime(), "Both players agreed to resume the game.");
							for (int i = 0; i < 2; ++i) 
								try { threads[i].join(); } catch (Exception e) {}							
							
							new Thread() {
								public void run() {
									Thread[] threads = new Thread[2];
									int gameStartCountdown = gameStartDelay;
									while (gameStartCountdown > 0) {
										for (int i = 0; i < 2; ++i) 
											threads[i] = socketHandler[i].sendCountdown(gameStartCountdown);
										for (int i = 0; i < 2; ++i) 
											try { threads[i].join(); } catch (Exception e) {}
										try { Thread.sleep(1000); } catch (Exception e) {}
										gameStartCountdown--;
									}
									
									for (int i = 0; i < 2; ++i)
										threads[i] = socketHandler[i].sendSignal(InformationServer.RESUME);
									for (int i = 0; i < 2; ++i) 
										try { threads[i].join(); } catch (Exception e) {}

									game.resume();	
								}
							}.start();
						}
						// ask for the other player's confirmation
						else { 
							requestedPauseResume[player] = true;
							Thread[] threads = new Thread[2];
							threads[0] = socketHandler[1 - player].sendMessage(
								"System", Time.getTime(), 
								username[player] + " has sent you a resuming request. If you would like to accept it, "
								+ "please click the resuming button in 10 seconds.");
							threads[1] = sendMessage(
								"System", Time.getTime(),
								"Resuming request sent."
							);
							for (int i = 0; i < 2; ++i) 
								try { threads[i].join(); } catch (Exception e) {}
							pauseResumeRequestTimeoutTask = new TimerTask() {
								public void run() {
									requestedPauseResume[player] = false;
									Thread[] threads = new Thread[2];
									threads[0] = socketHandler[player].sendMessage(
											"System", Time.getTime(), "Resuming request expired.");
									threads[1] = socketHandler[1 - player].sendMessage(
											"System", Time.getTime(), 
											username[player] + "'s resuming request has expired.");
									for (int i = 0; i < 2; ++i) 
										try { threads[i].join(); } catch (Exception e) {}
								}
							};
							timer.schedule(pauseResumeRequestTimeoutTask, pauseResumeRequestTimeoutDelay);
						}
					}	
				}
			} catch (IOException e) {
				// the client is disconnected
				disconnect(e);
			}
		}		
	}
}