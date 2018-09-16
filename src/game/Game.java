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
import java.util.*;
import java.util.Timer;
import java.nio.ByteBuffer;

// game controller
public class Game {
	public static final int EMPTY = 0;
	public static final int FOOD = -1;
	public static final int HOLE = -2;
	public static final int WALL = -3;
	public static final int NUM_FOODS = 2;

	public static int reviveSnakeDelay = 3000;
	private int driveSnakeDelay = 200;
	private int generateFoodDelay = 2000;
	private int unkennelSnakeDelay = 2000;
	
	int size;
	int minAvailableDistance = 5;
	int initialSnakeLength = 2;
	
	public ArrayList<Wall> walls = new ArrayList<Wall>();;
	public ArrayList<Point> holes = new ArrayList<Point>();
	public ArrayList<Point> foods = new ArrayList<Point>();;
	public Snake[] snakes = new Snake[2];
	public int[][] grid;
	public int[] numSnakes = new int[2];
	public int[] score = new int[2];
	private GameListener listener;
	private SnakeDriver[] snakeDriver = new SnakeDriver[2];
	private ReviveSnakeTask[] reviveSnakeTask = new ReviveSnakeTask[2];
	private UnkennelSnakeTask[] unkennelSnakeTask = new UnkennelSnakeTask[2];
	private GenerateFoodTask generateFoodTask;
	private long[] reviveTimeLeft = new long[2];
	private long[] unkennelTimeLeft = new long[2];
	private Timer timer = new Timer();
	private Random random = new Random();
	private boolean paused = false;
	
	public Game(int size, int level, int numSnakes, int driveSnakeDelay) {
		this.size = size;
		this.driveSnakeDelay = driveSnakeDelay;
		score[0] = score[1] = 0;
		
		updateGrid();
		
		// generate the game
		try {
			generateWalls(2 * level, 2, size / 3);
			generateWalls(level / 2, 1,  1);
			generateHoles(2 + level / 3);
			generateFoods(NUM_FOODS);
			generateSnakes(numSnakes);
		} catch (NoEnoughSpaceException e) {
			System.out.println(e);
		}
	}
	
	private byte[] readFromByteBuffer(ByteBuffer buffer) {
		int length = buffer.getInt();
		byte[] arr = new byte[length];
		buffer.get(arr, 0, length);
		return arr;
	}
	
	// decode from a packed game (when a new game state is received from the server)
	public Game(byte[] packed) {
		ByteBuffer buffer = ByteBuffer.wrap(packed);
		
		size = buffer.getInt();
		for (int i = 0; i < 2; ++i) {
			numSnakes[i] = buffer.getInt();
			score[i] = buffer.getInt();
		}
		int sizeWalls = buffer.getInt();
		for (int i = 0; i < sizeWalls; ++i)
			walls.add(new Wall(readFromByteBuffer(buffer)));
		int sizeHoles = buffer.getInt();
		for (int i = 0; i < sizeHoles; ++i)
			holes.add(new Point(readFromByteBuffer(buffer)));
		int sizeFoods = buffer.getInt();
		for (int i = 0; i < sizeFoods; ++i)
			foods.add(new Point(readFromByteBuffer(buffer)));
		int sizeSnakes = buffer.getInt();
		for (int i = 0; i < sizeSnakes; ++i)
			snakes[i] = new Snake(readFromByteBuffer(buffer));
		
		updateGrid();
	}
	
	public int getSize() {
		return size;
	}
	
	public boolean isPaused() {
		return paused;
	}
	
	// pause the game and stop the scheduled tasks
	public void pause() {
		synchronized (this) {
			paused = true;
			for (int i = 0; i < 2; ++i) 
				if (snakes[i].alive && snakes[i].first < snakes[i].last)
					endDriver(i);
				else if (snakes[i].alive && snakes[i].first >= snakes[i].last) {
					if (unkennelSnakeTask[i] != null) {
						unkennelTimeLeft[i] = 
								unkennelSnakeTask[i].scheduledExecutionTime() - System.currentTimeMillis();
							unkennelSnakeTask[i].cancel();
					}
				}
				else {
					if (reviveSnakeTask[i] != null) {
						reviveTimeLeft[i] = 
								reviveSnakeTask[i].scheduledExecutionTime() - System.currentTimeMillis();
							reviveSnakeTask[i].cancel();
					}
				}     
			if (foods.size() == 0)
				if (generateFoodTask != null)
					generateFoodTask.cancel();
		}
	}
	
	// resume the game
	public void resume() {
		synchronized (this) {
			for (int i = 0; i < 2; ++i) 
				if (snakes[i].alive && snakes[i].first < snakes[i].last)
					startDriver(i);
				else if (snakes[i].alive && snakes[i].first >= snakes[i].last) {
					if (unkennelSnakeTask[i] != null)
						timer.schedule(unkennelSnakeTask[i] = new UnkennelSnakeTask(i), unkennelTimeLeft[i]);
				}
				else {
					if (reviveSnakeTask[i] != null)
						timer.schedule(reviveSnakeTask[i] = new ReviveSnakeTask(i), reviveTimeLeft[i]);
				}
 			if (foods.size() == 0)
				timer.schedule(generateFoodTask = new GenerateFoodTask(), generateFoodDelay);
			paused = false;
			System.out.println("resumed");
		}
	}
	
	// set a listener
	public void setListener(GameListener listener) {
		this.listener = listener;
	}
	
	public boolean hasSnakesLeft(int player) {
		return numSnakes[player] > 0;
	}
	
	public boolean isAlive(int player) {
		return snakes[player].alive;
	}
	
	// update the grid with wall/food/hole/snake locations
	private void updateGrid() {
		if (grid == null)
			grid = new int[size][size];
		synchronized (grid) {
			for (int i = 0; i < size; ++i)
				for (int j = 0; j < size; ++j) 
					grid[i][j] = EMPTY;
			for (Point food : foods) 
				grid[food.x][food.y] = FOOD;
			for (Point hole : holes) 
				grid[hole.x][hole.y] = HOLE;
			for (Wall wall : walls)
				for (int k = 0; k < wall.length; ++k)
					grid[wall.x + wall.dx * k][wall.y + wall.dy * k] = WALL;
			for (int i = 0; i < 2; ++i) 
				if (snakes[i] != null)
					for (int j = snakes[i].first; j < snakes[i].last; ++j) {
						Point p = snakes[i].body.get(j);
						grid[snakes[i].body.get(j).x][p.y] = i + 1;
					}
		}
	}
	
	// add a wall
	private void addWall(Wall wall) {
		walls.add(wall);
		updateGrid();
	}

	// whether a wall candidate is valid
	private boolean isValidWall(Wall wall) {
		boolean[][] tmp = new boolean[size][size];
		for (int i = 0; i < size; ++i) 
			for (int j = 0; j < size; ++j)
				tmp[i][j] = grid[i][j] == WALL;
		for (int k = 0; k < wall.length; ++k) {
			int x = wall.getX(k);
			int y = wall.getY(k);
			if (!(x >= 0 && y >= 0 && x < size && y < size)) return false;
			if (grid[x][y] != 0) return false;
			tmp[x][y] = true;
		}
		for (int i = 0; i < size; ++i) 
			for (int j = 0; j < size; ++j) {
				int cnt = 0;
				for (int d = 0; d < 4; ++d) {
					int x = i + Direction.dx[d];
					int y = j + Direction.dy[d];
					if (x >= 0 && y >= 0 && x < size && y < size && tmp[x][y]) 
						++cnt;
				}
				if (cnt > 2)
					return false;
		}
		return true;
	}
	
	// check whether the wall candidates are valid
	private ArrayList<Wall> checkWalls(ArrayList<Wall> wallCandidates) {
		ArrayList<Wall> _wallCandidates = wallCandidates;
		wallCandidates = new ArrayList<Wall>();
		for (Wall wall : _wallCandidates)
			if (isValidWall(wall))
				wallCandidates.add(wall);
		return wallCandidates;
	}
	
	// generate new walls
	public void generateWalls(int numWalls, int minWallLength, int maxWallLength) 
			throws NoEnoughSpaceException {
		ArrayList<Wall> wallCandidates = new ArrayList<Wall>();
		for (int i = 0; i < size; ++i) 
			for (int j = 0; j < size; ++j) {
				for (int k = Math.max(1, minWallLength); 
						k <= Math.min(size - i, maxWallLength); ++k) 
					wallCandidates.add(new Wall(i, j, k, Wall.Direction.HORIZONTAL));
				for (int k = Math.max(1, minWallLength); 
						k <= Math.min(size - j, maxWallLength); ++k) 
					wallCandidates.add(new Wall(i, j, k, Wall.Direction.VERTICAL));
			}
		wallCandidates = checkWalls(wallCandidates);
		for (int k = 0; k < numWalls; ++k) {
			if (wallCandidates.size() == 0) 
				throw new NoEnoughSpaceException(
					"No enough space to generate all walls. Only generated " + k + " walls.");
			int idx = random.nextInt(wallCandidates.size());
			Wall wall = wallCandidates.get(idx);
			addWall(wall);
			
			// generate the second segment of a wall
			if (maxWallLength > 1 && k + 1 < numWalls && random.nextInt(2) > 0)  {
				ArrayList<Wall> neighborWallCandidates = new ArrayList<Wall>();
				int x1 = wall.x, y1 = wall.y;
				int x2 = wall.getX(wall.length - 1), y2 = wall.getY(wall.length - 1);
				for (Wall nextWall : wallCandidates) 
					if (nextWall.direction != wall.direction) {
						boolean neighbor = false;
						for (int i = 0; i < nextWall.length; ++i)
							for (int d = 0; d < 4; ++d) {
								int x = nextWall.getX(i) + Direction.dx[d];
								int y = nextWall.getY(i) + Direction.dy[d];
								if (x == x1 && y == y1 || x == x2 && y == y2) 
									neighbor = true;
							}
						if (!neighbor) continue;
						if (isValidWall(nextWall))
							neighborWallCandidates.add(nextWall);						
					}
				if (neighborWallCandidates.size() > 0) {
					k++;
					idx = random.nextInt(neighborWallCandidates.size());
					addWall(neighborWallCandidates.get(idx));
				}
			}
			wallCandidates = checkWalls(wallCandidates);
		}
	}
	
	// get maximum available distance when going straight from some point (among four directions) 
	private int getMaxAvailableDistance(Point hole) {
		int maxDistance = 0;
		for (int k = 0; k < 4; ++k) {
			int x = hole.x + Direction.dx[k], y = hole.y + Direction.dy[k];
			int d = 0;
			while (x >= 0 && y >= 0 && x < size && y < size && grid[x][y] == EMPTY) {
				x += Direction.dx[k];
				y += Direction.dy[k];
				d++;
			}
			if (d > maxDistance) maxDistance = d;
		}
		return maxDistance;
	}
	
	// whether some point is valid to be a hole
	private boolean isValidHole(Point hole) {
		if (grid[hole.x][hole.y] != EMPTY) return false;
		return getMaxAvailableDistance(hole) >= minAvailableDistance;
	}
	
	// whether the hole candidates are valid
	private ArrayList<Point> checkHoles(ArrayList<Point> holeCandidates) {
		ArrayList<Point> _holeCandidates = holeCandidates;
		holeCandidates = new ArrayList<Point>();
		for (Point hole : _holeCandidates)
			if (isValidHole(hole))
				holeCandidates.add(hole);
		return holeCandidates;
	}
	
	// add a new hole
	private void addHole(Point hole) {
		holes.add(hole);
		updateGrid();
	}
	
	// generate holes
	public void generateHoles(int numHoles) throws NoEnoughSpaceException {
		ArrayList<Point> holeCandidates = new ArrayList<Point>();
		for (int i = 0; i < size; ++i) 
			for (int j = 0; j < size; ++j) 
				holeCandidates.add(new Point(i, j));
		holeCandidates = checkHoles(holeCandidates);
		for (int k = 0; k < numHoles; ++k) {
			if (holeCandidates.size() == 0) 
				throw new NoEnoughSpaceException(
					"No enough space to generate all holes. Only generated " + k + " holes.");
			int idx = random.nextInt(holeCandidates.size());
			addHole(holeCandidates.get(idx));
			holeCandidates = checkHoles(holeCandidates);
		}
		updateGrid();
	}
	
	// add a food
	private void addFood(Point food) {
		foods.add(food);
		updateGrid();
	}
	
	// generate foods
	public void generateFoods(int numFood) throws NoEnoughSpaceException {
		ArrayList<Point> foodCandidates = new ArrayList<Point>();
		for (int i = 0; i < size; ++i) 
			for (int j = 0; j < size; ++j)
				if (grid[i][j] == EMPTY)
					foodCandidates.add(new Point(i, j));
		Collections.shuffle(foodCandidates);
		for (int k = 0; k < numFood; ++k) {
			if (k >= foodCandidates.size())
				throw new NoEnoughSpaceException(
					"No enough space to generate all foods. Only generated " + k + " foods.");
			addFood(foodCandidates.get(k));
		}
	}
	
	// generate snakes
	private Snake generateSnake() throws NoEnoughSpaceException {
		ArrayList<SnakeSegment> snakeCandidatesHard = new ArrayList<SnakeSegment>();
		ArrayList<SnakeSegment> snakeCandidatesSoft = new ArrayList<SnakeSegment>();
		for (int i = 0; i < size; ++i) 
			for (int j = 0; j < size; ++j)
				for (int d = 0; d < 4; ++d) {
					boolean valid = true;
					for (int k = 0; k < initialSnakeLength; ++k) {
						int x = i + Direction.dx[d] * k; 
						int y = j + Direction.dy[d] * k;
						if (!(x >= 0 && y >= 0 && x < size && y < size)) valid = false;
						else if (grid[x][y] != 0)
							valid = false;
					}
					boolean validHard = valid;
					for (int k = 1; k <= minAvailableDistance; ++k) {
						int x = i - Direction.dx[d] * k; 
						int y = j - Direction.dy[d] * k;
						if (!(x >= 0 && y >= 0 && x < size && y < size)) validHard = false;
						else if (grid[x][y] != 0)
							validHard = false;
					}
					if (validHard) 
						snakeCandidatesHard.add(new SnakeSegment(i, j, d, initialSnakeLength));
					else if (valid)
						snakeCandidatesSoft.add(new SnakeSegment(i, j, d, initialSnakeLength));
				}
		if (snakeCandidatesHard.size() == 0 && snakeCandidatesSoft.size() == 0)
			throw new NoEnoughSpaceException("No enough space to generate the new snake.");
		if (snakeCandidatesHard.size() > 0) 
			return new Snake(snakeCandidatesHard.get(random.nextInt(snakeCandidatesHard.size())));
		else
			return new Snake(snakeCandidatesSoft.get(random.nextInt(snakeCandidatesSoft.size())));
	}
	
	// use the next snake (when the current snake is dead)
	private void nextSnake(int player) throws NoEnoughSpaceException {
		endDriver(player);
		snakes[player] = generateSnake();
		numSnakes[player]--;
		updateGrid();
	}
	
	// generate snakes
	public void generateSnakes(int numSnakes) throws NoEnoughSpaceException {
		for (int i = 0; i < 2; ++i) 
			this.numSnakes[i] = numSnakes;
		for (int i = 0; i < 2; ++i)
			nextSnake(i);
	}
	
	// turn a snake
	public void turn(int player, boolean isRight) {
		if (!snakes[player].alive) return;
		if (snakes[player].first > 0) return;
		endDriver(player);
		synchronized (snakes[player]) {
			snakes[player].turn(isRight);
			next(player);
			listener.refresh();
		}
		startDriver(player);
	}
	
	// eat a food
	private void eatFood(Point p) {
		ArrayList<Point> _foods = foods;
		foods = new ArrayList<Point>();
		for (Point food : _foods)  
			if (!food.equals(p))
				foods.add(food);
		if (foods.size() == 0) {
			generateFoodTask = new GenerateFoodTask();
			timer.schedule(generateFoodTask, generateFoodDelay);
		}
	}
	
	// move the snake the next location
	public int next(int player) {
		synchronized (snakes[player]) {
			Snake snake = snakes[player];
			
			//snake.event = null; 
			if (!snake.alive) 
				return 0;
			if (snake.first > 0 && snake.first >= snake.last)
				return 0;
			
			// check death
			Point nextHead = snake.nextHead();
			int status = WALL;
			if (nextHead.x >= 0 && nextHead.y >= 0 && nextHead.x < size && nextHead.y < size)
				status = grid[nextHead.x][nextHead.y];
			boolean die = status == WALL || status > 0;
			Snake opponent = snakes[1 - player];
			for (int i = opponent.first; i < opponent.last; ++i)
				if (opponent.body.get(i).equals(nextHead))
					die = true;
			if (die) {
				snake.die();
				if (hasSnakesLeft(player)) {
					reviveSnakeTask[player] = new ReviveSnakeTask(player);
					timer.schedule(reviveSnakeTask[player], reviveSnakeDelay);
					listener.die(player);
				}			
				else {
					numSnakes[player] = -1;
					listener.refresh();
					listener.finish(1 - player);
					pause(); // to cancel scheduled tasks
				}
				return -1;
			}
			
			if (snake.alive) {
				if (status == EMPTY) {
					snake.next(false, false);
				}
				else if (status == FOOD) {
					snake.next(true, false);
					score[player]++;
					eatFood(snake.head());
				}
				else if (status == HOLE) {
					snake.next(false, true);
					if (snake.first >= snake.last) {
						endDriver(player);
						unkennelSnakeTask[player] = new UnkennelSnakeTask(player);
						timer.schedule(unkennelSnakeTask[player], unkennelSnakeDelay);
					}
				}
			}
		}
		
		updateGrid();	
		
		return 0;
	}
	
	// revive a snake
	public int revive(int player) {
		try {
			nextSnake(player);
			startFromHole(player);
		} catch (NoEnoughSpaceException e) {
			System.out.println(e);
			return -1;
		}
		return 0;
	}
	
	// encode the game (used before sending the game to clients)
	public byte[] pack(int player) {
		ArrayList<byte[]> packedWalls = new ArrayList<byte[]>();
		ArrayList<byte[]> packedHoles = new ArrayList<byte[]>();
		ArrayList<byte[]> packedFoods = new ArrayList<byte[]>();
		ArrayList<byte[]> packedSnakes = new ArrayList<byte[]>();
		
		for (Wall wall : walls)
			packedWalls.add(wall.pack());
		for (Point hole : holes)
			packedHoles.add(hole.pack());
		for (Point food : foods)
			packedFoods.add(food.pack());
		packedSnakes.add(snakes[player].pack());
		packedSnakes.add(snakes[1 - player].pack());
		
		int size = 9 * 4;
		for (byte[] arr : packedWalls)
			size += arr.length;
		for (byte[] arr : packedHoles)
			size += arr.length;
		for (byte[] arr : packedFoods)
			size += arr.length;
		for (byte[] arr : packedSnakes)
			size += arr.length;
		
		ByteBuffer buffer = ByteBuffer.allocate(size + 4);
		buffer.putInt(buffer.capacity() - 4);
		buffer.putInt(this.size);
		buffer.putInt(numSnakes[player]);
		buffer.putInt(score[player]);
		buffer.putInt(numSnakes[1 - player]);
		buffer.putInt(score[1 - player]);
		buffer.putInt(packedWalls.size());
		for (byte[] arr : packedWalls)
			buffer.put(arr);
		buffer.putInt(packedHoles.size());
		for (byte[] arr : packedHoles)
			buffer.put(arr);
		buffer.putInt(packedFoods.size());
		for (byte[] arr : packedFoods)
			buffer.put(arr);
		buffer.putInt(packedSnakes.size());
		for (byte[] arr : packedSnakes)
			buffer.put(arr);
		return buffer.array();
	}
	
	// start the snake driver
	public void startDriver(int player) {
		snakeDriver[player] = new SnakeDriver(player);
		timer.schedule(snakeDriver[player], driveSnakeDelay, driveSnakeDelay);
	}
	
	// end the snake driver
	public void endDriver(int player) {
		if (snakeDriver[player] == null) return;
		snakeDriver[player].cancel();
	}
	
	// whether some hole is available for some snake to depart
	private boolean isAvailableHole(Point hole) {
		for (int i = 0; i < 2; ++i) 
			if (snakes[i].alive) {
				for (int j = snakes[i].first; j < snakes[i].last; ++j)
					if (snakes[i].body.get(j).equals(hole))
						return false;
			}
		return true;
	}
	
	// let the snake start from the hole
	private void startFromHole(int player) {
		ArrayList<Point> holeCandidates = new ArrayList<Point>();
		for (Point hole : holes)
			if (isAvailableHole(hole) && 
					getMaxAvailableDistance(hole) >= minAvailableDistance)
				holeCandidates.add(hole);
		if (holeCandidates.size() == 0) {
			int maxDistance = -1;
			Point candidate = null;
			for (Point hole : holes) 
				if (isAvailableHole(hole)) {
					int d = getMaxAvailableDistance(hole);
					if (d > maxDistance) {
						maxDistance = d;
						candidate = hole;
					}
				}
			holeCandidates.add(candidate);
		}
		Point holeOut = holeCandidates.get(random.nextInt(holeCandidates.size()));
		int best = 0, direction = 0;
		for (int d = 0; d < 4; ++d) {
			int cnt = 0;
			int x = holeOut.x + Direction.dx[d];
			int y = holeOut.y + Direction.dy[d];
			while (x >= 0 && y >= 0 && x < size && y < size && grid[x][y] == EMPTY) {
				++cnt;
				x += Direction.dx[d];
				y += Direction.dy[d];
			}
			if (cnt > best) {
				best = cnt;
				direction = d;
			}
		}
		synchronized (snakes[player]) {
			snakes[player].first = snakes[player].last = 0;
			snakes[player].direction = direction;
			int length = snakes[player].body.size();
			snakes[player].body = new ArrayList<Point>();
			for (int i = 0; i < length; ++i)
				snakes[player].body.add(new Point(
					holeOut.x - i * Direction.dx[direction],
					holeOut.y - i * Direction.dy[direction]
				));
			new SnakeDriver(player).run();
			startDriver(player);
		}		
	}
	
	// snake driver
	class SnakeDriver extends TimerTask {
		private int player;
		
		public SnakeDriver(int player) {
			this.player = player;
		}
		
		public void run() {
			if (paused) return;
			if (!isAlive(player)) return;
			next(player);
			listener.refresh();
		}
	}
	
	// timer task to revive a snake
	class ReviveSnakeTask extends TimerTask {
		private int player;
		
		public ReviveSnakeTask(int player) {
			this.player = player;
		}
		
		public void run() {
			if (paused) return;
			int res = revive(player);
			listener.refresh();
			if (res == -1) {
				listener.finish(1 - player);
				pause();
			}
			else
				listener.revive(player);
		}
	}
	
	// timer task to unkennel a snake
	class UnkennelSnakeTask extends TimerTask {
		private int player;
		
		public UnkennelSnakeTask(int player) {
			this.player = player;
		}
		
		public void run() {
			if (paused) return;
			startFromHole(player);
		}
	}
	
	// timer snake to generate a new food
	class GenerateFoodTask extends TimerTask {
		public void run() {
			if (paused) return;
			try {
				generateFoods(NUM_FOODS);
			} catch (NoEnoughSpaceException e) {
				System.out.println(e);
			}
			listener.refresh();
		}
	}
}
