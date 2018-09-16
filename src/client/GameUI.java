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
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import javax.swing.event.ChangeListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.plaf.basic.BasicScrollBarUI;

import game.Direction;
import game.Snake;
import game.Wall;
import utils.Colors;
import utils.Font;
import utils.Layout;
import utils.Time;
import game.Game;
import game.Point;
import game.Ranklist;

// UI for the game window
public class GameUI extends JFrame {
	private final int SIDEBAR_BORDER_WIDTH = 5;
	private GameUIListener listener;
	private boolean paused = false;
	private Ranklist ranklist;
	private JFrame parent, _this;
	private Music music;
	private boolean isPlayback = false;
	
	int size;
	Game game = null;
	GameBoardContainer gameBoardContainer;
	GameBoard gameBoard;
	GameSidebar gameSidebar;
	
	public GameUI(JFrame parent, int size, Music music, boolean isPlayback) {
		this.parent = parent;
		this.size = size;
		this.music = music;
		this.isPlayback = isPlayback;
		_this = this;
		
		setContentPane(new GamePanel());
		
		// key events
		KeyAdapter keyListener = new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (game == null) return;
				int code = event.getKeyCode();
				if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT 
						|| code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN
						|| code == KeyEvent.VK_W || code == KeyEvent.VK_A
						|| code == KeyEvent.VK_S || code == KeyEvent.VK_D) {
					int d;
					if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A)
						d = Direction.WEST;
					else if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D)
						d = Direction.EAST;
					else if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W)
						d = Direction.NORTH;
					else
						d = Direction.SOUTH;
					if (d == (game.snakes[0].direction + 1) % 4) { // turn right
						if (listener != null)
							listener.turn(true);
					}
					else if (d == (game.snakes[0].direction + 3) % 4) { // turn left
						if (listener != null)
							listener.turn(false);
					}
				}
				else if (code == KeyEvent.VK_T) {
					gameSidebar.chat.textFieldInput.requestFocusInWindow();
				}
			}
		};
		gameBoardContainer.addKeyListener(keyListener);
		
		pack();
		setTitle("Snake");
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(755, 500));
		setBackground(Colors.BORDER);
		gameBoardContainer.setFocusable(true);
		gameBoardContainer.requestFocusInWindow();
	}
	
	// set a listener
	public void setListener(GameUIListener listener) {
		this.listener = listener;
	}
	
	// pause the game
	public void pause() {
		paused = true;
		gameSidebar.buttons.buttonPause.setVisible(false);
		gameSidebar.buttons.buttonResume.setVisible(true);
		repaint();
	}
	
	// resume the game
	public void resume() {
		paused = false;
		gameBoard.setMessage("");
		gameSidebar.buttons.buttonPause.setVisible(true);
		gameSidebar.buttons.buttonResume.setVisible(false);
		repaint();
	}
	
	// playback ended
	public void end() {
		gameSidebar.buttons.buttonPause.setVisible(false);
		gameSidebar.buttons.buttonResume.setVisible(true);
		repaint();
	}	
	
	// start the game
	public void start() {
		gameBoard.setMessage("");
		if (music != null)
			music.start();
	}
	
	// show countdown
	public void showCountdown(int countdown) {
		gameBoard.setMessage(Integer.toString(countdown));
	}
	
	// set the ranklist
	public void setRanklist(Ranklist ranklist) {
		this.ranklist = ranklist;
	}
	
	// set the usernames
	public void setUsername(String usernameMine, String usernameOpponent) {
		gameSidebar.status[0].labelUsername.setText(usernameMine);
		gameSidebar.status[1].labelUsername.setText(usernameOpponent);
		for (int i = 0; i < 2; ++i) {
			gameSidebar.status[i].labelUsername.revalidate();
			gameSidebar.status[i].labelUsername.repaint();
		}		
	}
	
	// update the window with the new game state
	public void sync(Game game) {
		this.game = game;
		for (int i = 0; i < 2; ++i)
			gameSidebar.setStatus(i, game.numSnakes[i], game.score[i]);
		revalidate();
		repaint();
	}
	
	// show a message in the chat box
	public void showMessage(String message) {
		gameSidebar.chat.showMessage("System", Time.getTime(), message);
	}	
	public void showMessage(String username, String time, String message) {
		gameSidebar.chat.showMessage(username, time, message);
	}
	
	// game finished
	public void finish() {
		gameSidebar.buttons.buttonPause.setVisible(false);
		gameSidebar.buttons.buttonResume.setVisible(false);
		gameSidebar.buttons.buttonHome.setVisible(true);
	}
	
	// reset progress bar
	public void resetProgress(int size) {
		gameSidebar.sliders.sliderProgress.setMaximum(size - 1);
		gameSidebar.sliders.sliderProgress.setValue(0);
		gameSidebar.sliders.sliderProgress.repaint();
	}
	
	// set value of the progress bar
	public void setProgress(int value) {
		gameSidebar.sliders.sliderProgress.setValue(value);
		gameSidebar.sliders.sliderProgress.repaint();
	}
	
	// get playback speed
	public double getDelay() {
		return Math.pow(5, (5 - gameSidebar.sliders.sliderSpeed.getValue()) / 5.); 
	}	
	
	class GamePanel extends JPanel {
		public GamePanel() {
			setLayout(new BorderLayout());
			setBackground(Colors.BORDER);
			add(gameBoardContainer = new GameBoardContainer(), "Center");
			add(gameSidebar = new GameSidebar(), "East");
		}
	}
	
	class GameBoardContainer extends JPanel {
		public GameBoardContainer() {
			setLayout(new BorderLayout());
			add(gameBoard = new GameBoard(), "Center");
			setBackground(Colors.BORDER);
				
			addComponentListener(new ComponentAdapter() {
				// when the window is resized
				public void componentResized(ComponentEvent e) {
					if (game == null) return;
					int width = getWidth() % game.getSize();
					int height = getHeight() % game.getSize();
					int borderLeft = Math.min(SIDEBAR_BORDER_WIDTH, width);
					width -= borderLeft;
					int borderRight = (width + 1) / 2;
					borderLeft += width / 2;
					setBorder(new EmptyBorder(
						height % game.getSize() / 2, 
						borderLeft, 
						(height % game.getSize() + 1) / 2, 
						borderRight
					));
					revalidate();
					repaint();
				}
			});
			
			addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					requestFocusInWindow();
				}
			});
		}
	}
	
	// game board
	class GameBoard extends JPanel {
		private int width, height, widthPerCell, heightPerCell;
		private Image iconFood, iconWallIsolated, iconHole;
		private Image[] iconWallCorner = new Image[4];
		private Image[] iconWallHead = new Image[4];
		private Image[] iconWallBody = new Image[4];
		private String message = "";
		
		public GameBoard() {
			setBackground(Colors.BORDER);
			setPreferredSize(new Dimension(800, 800));
			setMinimumSize(new Dimension(2 * size, 2 * size));
			iconFood = new ImageIcon(
				GameUI.class.getResource("/icons/food.png")).getImage();
			iconHole = new ImageIcon(
				GameUI.class.getResource("/icons/hole.png")).getImage();
			iconWallCorner[0] = new ImageIcon(
				GameUI.class.getResource("/icons/wall-corner.png")).getImage();
			iconWallHead[0] = new ImageIcon(
					GameUI.class.getResource("/icons/wall-head.png")).getImage();
			iconWallBody[0] = new ImageIcon(
					GameUI.class.getResource("/icons/wall-body.png")).getImage();
			iconWallIsolated = new ImageIcon(
					GameUI.class.getResource("/icons/wall-isolated.png")).getImage();			
			for (int i = 1; i < 4; ++i) {
				iconWallCorner[i] = rotateImage(iconWallCorner[0], i * 90);
				iconWallHead[i] = rotateImage(iconWallHead[0], i * 90);
				iconWallBody[i] = rotateImage(iconWallBody[0], i * 90);
			}
		}
		
		// show a message
		public void setMessage(String message) {
			this.message = message;
			repaint();
		}
		
		// rotate an icon file
		private Image rotateImage(Image image, int degree) {
			int w = image.getWidth(null);
			int h = image.getHeight(null);
		    BufferedImage buffer = new BufferedImage(
	    		w, h, BufferedImage.TYPE_INT_ARGB);
		    Graphics2D bufferG = buffer.createGraphics();
		    bufferG.drawImage(image, 0, 0, null);
		    bufferG.dispose();
			AffineTransformOp op = new AffineTransformOp(
				AffineTransform.getRotateInstance(Math.toRadians(degree), w / 2., h / 2.),
				AffineTransformOp.TYPE_BILINEAR
			);
			return op.filter(buffer, null);
		}
		
		// paint the window
		public void paint(Graphics g) {
			width = getWidth();
			height = getHeight();
			widthPerCell = width / size;
			heightPerCell = height / size;
			renderBackground(g);
			if (game != null) {
				renderWalls(g);
				renderHoles(g);
				renderFoods(g);
				renderSnakes(g);	
			}
			renderMessage(g);
		}
		
		// whether some cell is a wall
		private boolean isWall(int x, int y) {
			if (x >= 0 && y >= 0 && x < size && y < size) 
				return game.grid[x][y] == Game.WALL;
			else
				return false;
		}		
		
		// show a message
		private void renderMessage(Graphics g) {
			String message = "";
			Font font = new Font(160);
			if (this.message.length() > 0) {
				message = this.message;
				font = new Font(320);
			}
			else if (paused) {
				message = "PAUSED";
			}
			else if (game.snakes[0] != null) {
				Snake snake = game.snakes[0];
				if (snake.alive && snake.first >= snake.last)
					message = "IN HOLE";
			}
		    FontMetrics metrics = g.getFontMetrics(font);
		    int x = (width - metrics.stringWidth(message)) / 2;
		    int y = (int)(((height - metrics.getHeight()) * 0.45) + metrics.getAscent() - 20);
		    g.setFont(font);
		    g.setColor(Colors.SNAKE_MINE);
		    g.drawString(message, x, y);
		}
		
		// render game board background
		private void renderBackground(Graphics g) {
			for (int i = 0; i < size; ++i) 
				for (int j = 0; j < size; ++j) {
					g.setColor((i + j) % 2 == 0 ? Colors.CELL_DARK : Colors.CELL_LIGHT);
					g.fillRect(widthPerCell * i, heightPerCell * j, widthPerCell, heightPerCell);
				}
		}
		
		// render a wall cell
		private void renderWallCell(Graphics g, int x, int y) {
			Image icon = null;
			if (isWall(x - 1, y) && isWall(x + 1, y))
				icon = iconWallBody[0];
			else if (isWall(x, y - 1) && isWall(x,  y + 1))
				icon = iconWallBody[1];
			else if (isWall(x, y + 1) && isWall(x + 1, y))
				icon = iconWallCorner[0];
			else if (isWall(x - 1, y) && isWall(x, y + 1))
				icon = iconWallCorner[1];
			else if (isWall(x - 1, y) && isWall(x, y - 1))
				icon = iconWallCorner[2];
			else if (isWall(x, y - 1) && isWall(x + 1, y))
				icon = iconWallCorner[3];
			else if (isWall(x + 1, y))
				icon = iconWallHead[0];
			else if (isWall(x, y + 1))
				icon = iconWallHead[1];
			else if (isWall(x - 1, y))
				icon = iconWallHead[2];
			else if (isWall(x, y - 1))
				icon = iconWallHead[3];
			else
				icon = iconWallIsolated;
			g.drawImage(
				icon, (int)(widthPerCell * x), (int)(heightPerCell * y), 
				widthPerCell, heightPerCell, null
			);	
		}
		
		// render walls
		private void renderWalls(Graphics g) {
			for (Wall wall : game.walls) {
				for (int i = 0; i < wall.length; ++i)
					renderWallCell(g, wall.getX(i), wall.getY(i));
			}
		}
		
		// render holes
		private void renderHoles(Graphics g) {
			g.setColor(Color.white);
			for (Point hole : game.holes)
				if (hole.x == 0 || hole.y == 0 || hole.x + 1 == size || hole.y + 1 == size) 
					g.drawImage(
						iconHole, 
						(int)(widthPerCell * hole.x - widthPerCell * 0.1), 
						(int)(heightPerCell * hole.y - widthPerCell * 0.15), 
						(int)(widthPerCell * 1.2), (int)(heightPerCell * 1.2), null
					);		
				else
					g.drawImage(
						iconHole, 
						(int)(widthPerCell * hole.x - widthPerCell * 0.4), 
						(int)(heightPerCell * hole.y - widthPerCell * 0.6), 
						(int)(widthPerCell * 1.8), (int)(heightPerCell * 1.8), null
					);		
		}
		
		// render foods
		private void renderFoods(Graphics g) {
			g.setColor(Color.yellow);
			for (Point food : game.foods) {
				if (food.x == 0 || food.y == 0 || food.x + 1 == size || food.y + 1 == size) {
					g.drawImage(
						iconFood,
						(int)(widthPerCell * (food.x)), 
						(int)(heightPerCell * (food.y)), 
						(int)(widthPerCell * 1), 
						(int)(heightPerCell * 1),
						null
					);		
				}
				else {
					g.drawImage(
						iconFood,
						(int)(widthPerCell * (food.x - 0.25)), 
						(int)(heightPerCell * (food.y - 0.25)), 
						(int)(widthPerCell * 1.5), 
						(int)(heightPerCell * 1.5),
						null
					);		
				}
			}
		}
		
		// render a snake cell
		private void renderSnakeCell(Graphics g, int x, int y, int d) {
			int startAngle = 90;
			if (d == Direction.NORTH) {
				g.fillRect(
					widthPerCell * x + 1, heightPerCell * y, 
					widthPerCell - 2, (heightPerCell + 1) / 2
				);
				startAngle = 180;
			}
			else if (d == Direction.SOUTH) {
				g.fillRect(
					widthPerCell * x + 1, heightPerCell * y + heightPerCell / 2, 
					widthPerCell - 2, (heightPerCell + 1) / 2
				);
				startAngle = 0;
			}
			else if (d == Direction.WEST) {
				g.fillRect(
					widthPerCell * x, heightPerCell * y + 1,
					(widthPerCell + 1) / 2, heightPerCell - 2
				);
				startAngle = 270;
			}
			else {
				g.fillRect(
					widthPerCell * x + widthPerCell / 2, heightPerCell * y + 1,
					(widthPerCell + 1) / 2, heightPerCell - 2
				);
				startAngle = 90;
			} 
			g.fillArc(
				widthPerCell * x + 1, heightPerCell * y + 1, 
				widthPerCell - 2, heightPerCell - 2, 
				startAngle, 180
			);			
		}

		// render the nose tril of the same
		private void renderSnakeNosetril(Graphics g, Color color, int x, int y) {
			int w = widthPerCell / 8, h = heightPerCell / 8;
			g.setColor(color);
			g.fillOval(x - w / 2, y - h / 2, w, h);	
		}
		
		// render the eyes of the snake
		private void renderSnakeEye(Graphics g, Color color, Color colorDark, int x, int y) {
			int w = widthPerCell / 2, h = heightPerCell / 2;
			g.setColor(color);
			g.fillOval(x, y, w, h);
			int dw = Math.max(1, (int)(w * 0.1));
			int dh = Math.max(1, (int)(h * 0.1));
			x += dw; w -= dw * 2;
			y += dh; h -= dh * 2;
			g.setColor(Color.white);
			g.fillOval(x, y, w, h);
			dw = Math.max(1, (int)(w * 0.3));
			dh = Math.max(1, (int)(h * 0.3));
			x += dw; w -= dw * 2;
			y += dh; h -= dh * 2;
			g.setColor(colorDark);
			g.fillOval(x, y, w, h);
		}
		
		// render the head of the snake
		private void renderSnakeHead(Graphics g, Snake snake, Color color, Color colorDark) {
			int x = snake.head().x;
			int y = snake.head().y;
			int eyeX1, eyeY1, eyeX2, eyeY2;
			eyeX1 = eyeY1 = eyeX2 = eyeY2 = 0;
			if (snake.direction == Direction.NORTH || snake.direction == Direction.SOUTH) {
				if (x > 0 && x + 1 < size) {
					eyeX1 = x * widthPerCell - widthPerCell / 8;
					eyeX2 = (x + 1) * widthPerCell + widthPerCell / 8 - widthPerCell / 2;
				}
				else {
					eyeX1 = x * widthPerCell;
					eyeX2 = x * widthPerCell + widthPerCell / 2;
				}
				if (snake.direction == Direction.NORTH)
					eyeY1 = eyeY2 = y * heightPerCell + heightPerCell / 2;
				else 
					eyeY1 = eyeY2 = y * heightPerCell;
			}
			else {
				if (y > 0 && y + 1 < size) {
					eyeY1 = y * heightPerCell - heightPerCell / 8;
					eyeY2 = (y + 1) * heightPerCell + heightPerCell / 8 - heightPerCell / 2;
				}
				else {
					eyeY1 = y * heightPerCell;
					eyeY2 = y * heightPerCell + heightPerCell / 2;					
				}
				if (snake.direction == Direction.WEST)
					eyeX1 = eyeX2 = x * widthPerCell + widthPerCell / 2;
				else 
					eyeX1 = eyeX2 = x * widthPerCell;				
			}
			renderSnakeEye(g, color, colorDark, eyeX1, eyeY1);
			renderSnakeEye(g, color, colorDark, eyeX2, eyeY2);
			
			int dx = Direction.dx[snake.direction], dy = Direction.dy[snake.direction];
			if (snake.direction == Direction.NORTH || snake.direction == Direction.SOUTH) {
				renderSnakeNosetril(
					g, colorDark, 
					x * widthPerCell + widthPerCell / 2 - widthPerCell / 8,
					y * heightPerCell + heightPerCell / 2 + heightPerCell / 4 * dy
				);
				renderSnakeNosetril(
					g, colorDark, 
					x * widthPerCell + widthPerCell / 2 + widthPerCell / 8,
					y * heightPerCell + heightPerCell / 2 + heightPerCell / 4 * dy
				);
			}
			else {
				renderSnakeNosetril(
					g, colorDark, 
					x * widthPerCell + widthPerCell / 2 + widthPerCell / 4 * dx,
					y * heightPerCell + heightPerCell / 2 - heightPerCell / 8
				);
				renderSnakeNosetril(
					g, colorDark, 
					x * widthPerCell + widthPerCell / 2 + widthPerCell / 4 * dx,
					y * heightPerCell + heightPerCell / 2 + heightPerCell / 8
				);
			}					
		}
		
		// render the snake cell when it is at a hole
		private void renderSnakeCellAtHole(Graphics g, Point p, Color color, int direction) {
			int x = p.x, y = p.y;
			if (direction == Direction.NORTH)
				g.fillRect(
					widthPerCell * x, heightPerCell * (y + 1) - heightPerCell / 3, widthPerCell, heightPerCell / 3);
			else if (direction == Direction.SOUTH) 
				g.fillRect(widthPerCell * x, heightPerCell * y, widthPerCell, heightPerCell / 3);
			else if (direction == Direction.WEST)
				g.fillRect(
					widthPerCell * (x + 1) - widthPerCell / 3, heightPerCell * y, widthPerCell / 3, heightPerCell);
			else
				g.fillRect(widthPerCell * x, heightPerCell * y, widthPerCell / 3, heightPerCell);
		}
		
		// render a snake
		private void renderSnake(Graphics g, Snake snake, Color color, Color colorDark) {
			if (snake == null) return;
			if (!snake.alive)
				color = Color.black;
			g.setColor(color);
			
			// render the snake cell at the hole
			if (snake.first < snake.last && snake.first > 0) 
				renderSnakeCellAtHole(g, snake.body.get(snake.first - 1), color, snake.direction);
			else if (snake.first < snake.last && snake.last < snake.length()) {
				int direction = Direction.opposite(snake.direction);
				if (snake.last > 0) {
					Point pre = snake.body.get(snake.last - 1);
					Point last = snake.body.get(snake.last);
					for (int i = 0; i < 4; ++i) 
						if ((new Point(last.x + Direction.dx[i], last.y + Direction.dy[i])).equals(pre))
							direction = Direction.opposite(i);
				}
				renderSnakeCellAtHole(g, snake.body.get(snake.last), color, direction);
			}
			for (int i = snake.first; i < snake.last; ++i) {
				int x = snake.body.get(i).x;
				int y = snake.body.get(i).y;
				for (int j = i - 1; j <= i + 1; j += 2) 
					if (j >= 0 && j < snake.body.size())
						for (int d = 0; d < 4; ++d) 
							if (new Point(x + Direction.dx[d], y + Direction.dy[d]).equals(snake.body.get(j)))
								renderSnakeCell(g, x, y, d);	
			}
			
			if (snake.first == 0)
				renderSnakeHead(g, snake, color, colorDark);	
		}
		
		// render the snakes
		private void renderSnakes(Graphics _g) {
			Graphics2D g = (Graphics2D) _g;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			renderSnake(g, game.snakes[0], Colors.SNAKE_MINE, Colors.SNAKE_DARK_MINE);
			renderSnake(g, game.snakes[1], Colors.SNAKE_OPPONENT, Colors.SNAKE_DARK_OPPONENT);
		}
	}

	// game sidebar
	class GameSidebar extends JPanel {
		public static final int width = 250;
		
		GameStatus[] status = new GameStatus[2];
		GameButtons buttons;
		GameChat chat;
		GameSliders sliders;
		
		public GameSidebar() {
			setLayout(new GridBagLayout());
			setBorder(BorderFactory.createMatteBorder(0, SIDEBAR_BORDER_WIDTH, 0, 0, Colors.BORDER));
			add(status[1] = new GameStatus(), Layout.getConstraints(0, 0, 1, 1, 0, 0));
			add(buttons = new GameButtons(), Layout.getConstraints(0, 1, 1, 1, 0, 0));
			if (isPlayback) {
				add(sliders = new GameSliders(), Layout.getConstraints(0, 2, 1, 1, 0, 0));
				add(status[0] = new GameStatus(), Layout.getConstraints(0, 3, 1, 1, 0, 0));
				add(chat = new GameChat(), Layout.getConstraints(0, 4, 1, 1, 0, 1));
			}
			else {
				add(status[0] = new GameStatus(), Layout.getConstraints(0, 2, 1, 1, 0, 0));
				add(chat = new GameChat(), Layout.getConstraints(0, 3, 1, 1, 0, 1));
			}
			setMinimumSize(new Dimension(GameSidebar.width, 0));
			setMaximumSize(new Dimension(GameSidebar.width, 10000));
		}
		
		public void setStatus(int player, int numSnakes, int score) {
			status[player].labelLives.setText(String.valueOf(numSnakes + 1));
			status[player].labelScore.setText(String.valueOf(score));
		}
	}
	
	class GameStatus extends JPanel {
		Text labelUsername, labelLives, labelScore;
		
		public GameStatus() {
			setBackground(Colors.CELL_DARK);
			setLayout(new GridBagLayout());
			add(labelUsername = new Text("username", 20), Layout.getConstraints(0, 0, 1, 1, 1, 0));
			add(renderLives(), Layout.getConstraints(0, 1, 1, 1, 1, 0));
			add(renderScore(), Layout.getConstraints(0, 2, 1, 1, 1, 1));
			setPreferredSize(new Dimension(GameSidebar.width, 150));
			setMinimumSize(new Dimension(GameSidebar.width, 150));
			setMaximumSize(new Dimension(GameSidebar.width, 150));			
		}
		
		private JPanel renderLives() {
			labelLives = new Text("0", 24);
			ImageIcon icon = new ImageIcon(
				new ImageIcon(GameUI.class.getResource("/icons/life.png"))
				.getImage()
				.getScaledInstance(24, 24,  java.awt.Image.SCALE_SMOOTH));
			JPanel container = new JPanel();
			container.setLayout(new BorderLayout());
			container.setBorder(new EmptyBorder(0, 0, 0, 0));
			container.setOpaque(false);
			JPanel wrapper = new JPanel();
			wrapper.setOpaque(false);
			wrapper.setBorder(new EmptyBorder(0, 0, 0, 0));
			JLabel labelIcon = new JLabel(icon);
			labelIcon.setBorder(new EmptyBorder(0, 0, 0, 0));
			wrapper.add(new JLabel(icon));
			labelLives.setBorder(new EmptyBorder(0, 10, 0, 0));
			wrapper.add(labelLives);
			container.add(wrapper, "Center");
			return container;
		}
		
		private JPanel renderScore() {
			labelScore = new Text("0", 48);
			ImageIcon icon = new ImageIcon(
				new ImageIcon(GameUI.class.getResource("/icons/food.png"))
				.getImage()
				.getScaledInstance(36, 36, java.awt.Image.SCALE_SMOOTH));
			JPanel container = new JPanel();
			container.setLayout(new GridBagLayout());
			container.setBorder(new EmptyBorder(0, 0, 10, 0));
			container.setOpaque(false);
			JPanel wrapper = new JPanel();
			wrapper.setLayout(new BorderLayout());
			wrapper.setOpaque(false);
			wrapper.add(new JLabel(icon), "West");
			labelScore.setBorder(new EmptyBorder(0, 15, 0, 0));
			wrapper.add(labelScore, "Center");
			container.add(wrapper, Layout.getConstraints(0, 0, 1, 1, 0, 1));
			return container;
		}
		
		class Text extends JLabel {
			public Text(String text, int fontSize) {
				super(text);
				setFont(new Font(fontSize));
				setForeground(Color.white);
				setHorizontalAlignment(SwingConstants.CENTER);
				setVerticalAlignment(SwingConstants.CENTER);
				setAlignmentX(Component.CENTER_ALIGNMENT);
			}
		}
	}
	
	class GameSliders extends JPanel {
		JSlider sliderProgress, sliderSpeed;
		
		public GameSliders() {
			setLayout(new GridBagLayout());
			setBackground(Colors.CELL_LIGHT);
			setPreferredSize(new Dimension(GameSidebar.width, 100));
			setMinimumSize(new Dimension(GameSidebar.width, 100));
			setMaximumSize(new Dimension(GameSidebar.width, 100));	
			add(renderText("Progress:"), Layout.getConstraints(0, 0, 1, 1, 1, 1));
			add(sliderProgress = new JSlider(0, 10, 5), Layout.getConstraints(0, 1, 1, 1, 1, 1));
			add(renderText("Speed:"), Layout.getConstraints(0, 2, 1, 1, 1, 1));
			add(sliderSpeed = new JSlider(0, 10, 5), Layout.getConstraints(0, 3, 1, 1, 1, 1));
			
			sliderProgress.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent evt) {
					if (listener != null)
						listener.changeProgress(sliderProgress.getValue());	
				}
			});
			sliderSpeed.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent evt) {
					if (listener != null)
						listener.changeSpeed(sliderSpeed.getValue());	
				}
			});
			
		}
		
		private JLabel renderText(String text) {
			JLabel label = new JLabel(text);
			label.setFont(new Font(16));
			label.setForeground(Color.white);
			label.setBorder(new EmptyBorder(0, 10, 0, 10));
			return label;
		}
	}
	
	class GameButtons extends JPanel {
		GameButton buttonResume, buttonPause, buttonRank, buttonHome;
		
		public GameButtons() {
			setLayout(new GridBagLayout());
			setBackground(Colors.CELL_LIGHT);
			if (isPlayback)
				setBorder(new EmptyBorder(15, 90, 15, 80));
			else
				setBorder(new EmptyBorder(15, 45, 15, 45));
			setPreferredSize(new Dimension(GameSidebar.width, 100));
			setMinimumSize(new Dimension(GameSidebar.width, 100));
			setMaximumSize(new Dimension(GameSidebar.width, 100));
			buttonResume = new GameButton("/icons/play.png");
			buttonPause = new GameButton("/icons/pause.png");
			buttonHome = new GameButton("/icons/home.png");
			buttonRank = new GameButton("/icons/rank.png");
			add(buttonPause, Layout.getConstraints(0, 0, 1, 1, 1, 1, new Insets(0, 0, 0, 10)));
			add(buttonResume, Layout.getConstraints(0, 0, 1, 1, 1, 1, new Insets(0, 0, 0, 10)));
			add(buttonHome, Layout.getConstraints(0, 0, 1, 1, 1, 1, new Insets(0, 0, 0, 10)));
			add(buttonRank, Layout.getConstraints(1, 0, 1, 1, 1, 1, new Insets(0, 10, 0, 0)));
			
			buttonPause.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (listener != null)
						listener.requestPause();
				}
			});			
			buttonResume.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (listener != null)
						listener.requestResume();
				}
			});		
			buttonHome.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					if (music != null)
						music.stop();
					_this.setVisible(false);
					listener.close();  
					parent.setVisible(true);
				}
			});
			buttonRank.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					RanklistUI ranklistUI = new RanklistUI(ranklist);
					ranklistUI.setVisible(true);
				}
			});		
			
			buttonResume.setVisible(false);
			if (isPlayback)
				buttonRank.setVisible(false);
		}
	}
	
	class GameButton extends JLabel {
		private Image icon;
		
		public GameButton(String iconURL) {
			icon = new ImageIcon(GameUI.class.getResource(iconURL)).getImage();
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));			
		}
		
		public void paint(Graphics _g) {
			Graphics2D g = (Graphics2D) _g;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(Colors.BORDER);
			g.fillOval(0, 0, getWidth(), getHeight());
			_g.drawImage(icon, 20, 20, getWidth() - 40, getHeight() - 40, null);
		}
	}
	
	class GameChat extends JPanel {
		private Font font;
		JTextArea textAreaHistory;
		JTextField textFieldInput;
		JLabel buttonSend;
		
		public GameChat() {
			font = new Font(16);
			setLayout(new GridBagLayout());
			add(renderHistory(), Layout.getConstraints(0, 0, 1, 1, 1, 1));
			add(renderInputBox(), Layout.getConstraints(0, 1, 1, 1, 0, 0));
			setMinimumSize(new Dimension(GameSidebar.width, 0));
			setMaximumSize(new Dimension(GameSidebar.width, 10000));						
		}
		
		public void showMessage(String username, String time, String message) {
			String newText = username + "  [" + time + "]:\n" + message + "\n\n";
			textAreaHistory.setText(textAreaHistory.getText() + newText);
			textAreaHistory.setCaretPosition(textAreaHistory.getDocument().getLength());
		}
		
		private JComponent renderHistory() {
			textAreaHistory = new JTextArea();
			textAreaHistory.setBackground(Colors.CELL_LIGHTER);
			textAreaHistory.setForeground(Colors.CHAT_TEXT);
			textAreaHistory.setBorder(new EmptyBorder(5, 5, 5, 5));
			textAreaHistory.setFont(font);
			textAreaHistory.setLineWrap(true);
			textAreaHistory.setWrapStyleWord(true); 
			textAreaHistory.setEditable(false);
			textAreaHistory.setFocusable(false);

			UIManager.put("ScrollBar.trackHighlight", Color.red);
			JScrollPane scroll = new JScrollPane(textAreaHistory);
			scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			scroll.setBorder(BorderFactory.createEmptyBorder());
			scroll.getVerticalScrollBar().setUI(new ScrollBar());
			return scroll;
		}
		
		private JPanel renderInputBox() {
			JPanel inputBox = new JPanel();
			inputBox.setLayout(new GridBagLayout());
			inputBox.setBorder(
				BorderFactory.createMatteBorder(4, 0, 0, 0, Colors.BORDER));			
			inputBox.add(renderInput(), Layout.getConstraints(0, 0, 1, 1, 1, 1));
			inputBox.add(renderButtonSend("Send"), Layout.getConstraints(1, 0, 1, 1, 0, 0));
			if (isPlayback)
				inputBox.setVisible(false);
			return inputBox;
		}
		
		private JTextField renderInput() {
			textFieldInput = new JTextField();
			textFieldInput.setBorder(new EmptyBorder(2, 5, 2, 5));
			textFieldInput.setBackground(Colors.CELL_LIGHTER);
			textFieldInput.setForeground(Colors.CHAT_TEXT);
			textFieldInput.setFont(new Font(16));
			textFieldInput.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					int code = e.getKeyCode();
					if (code == KeyEvent.VK_ENTER) 
						sendMessage();
					else if (code == KeyEvent.VK_ESCAPE)
						gameBoardContainer.requestFocusInWindow();
				}
			});			
			return textFieldInput;
		}
		
		private JLabel renderButtonSend(String text) {
			buttonSend = new JLabel(text);
			buttonSend.setOpaque(true);
			buttonSend.setBackground(Colors.BORDER);
			buttonSend.setForeground(Color.white);
			buttonSend.setBorder(new EmptyBorder(0, 5, 0, 5));
			buttonSend.setFont(new Font(16));
			buttonSend.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			buttonSend.setBorder(new EmptyBorder(0, 5, 2, 5));
			buttonSend.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					sendMessage();
				}
			});
			return buttonSend;
		}
		
		private void sendMessage() {
			listener.sendMessage(Time.getTime(), textFieldInput.getText());
			textFieldInput.setText("");
		}
		
		class ScrollBar extends BasicScrollBarUI {
			@Override
			protected JButton createIncreaseButton(int orientation) {
	            return createEmptyButton();
	        }
			
			@Override
	        protected JButton createDecreaseButton(int orientation) {
	            return createEmptyButton();
	        }

	        private JButton createEmptyButton() {
	            JButton button = new JButton();
	            button.setPreferredSize(new Dimension(0, 0));
	            button.setMinimumSize(new Dimension(0, 0));
	            button.setMaximumSize(new Dimension(0, 0));
	            return button;
	        }		
	        
			@Override
		    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
		    	g.setColor(Colors.CELL_LIGHTER);
		    	g.fillRect(r.x, r.y, r.width, r.height);
		    	g.setColor(Colors.CELL_DARK);
		    	g.fillRect(r.x, r.y, 2, r.height);
		    }

		    @Override
		    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
		    	g.setColor(Colors.CELL_DARK);
		    	g.fillRect(r.x, r.y, r.width, r.height);
		    }
		}		
		
	}		
}

interface GameUIListener {
	public void turn(boolean isRight);
	public void sendMessage(String time, String message);
	public void requestPause();
	public void requestResume();
	public void close();
	public void changeProgress(int value);
	public void changeSpeed(int value);
}
