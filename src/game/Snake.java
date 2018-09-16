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
import java.nio.ByteBuffer;
import game.Direction;

// snake
public class Snake {
	public ArrayList<Point> body = new ArrayList<Point>();
	public boolean alive = false;
	public int direction;
	public int first, last;
	
	public Snake(SnakeSegment segment) {
		int dx = Direction.dx[segment.direction];
		int dy = Direction.dy[segment.direction];
		for (int i = 0; i < segment.length; ++i) 
			body.add(new Point(segment.x + dx * i, segment.y + dy * i));
		first = 0;
		last = body.size();
		direction = Direction.opposite(segment.direction);
		alive = true;
	}
	
	// decode the snake from a packed one 
	public Snake(byte[] packed) {
		ByteBuffer buffer = ByteBuffer.wrap(packed);
		alive = buffer.getInt() > 0;
		direction = buffer.getInt();
		first = buffer.getInt();
		last = buffer.getInt();
		int bodySize = buffer.getInt();
		for (int i = 0; i < bodySize; ++i)
			body.add(new Point(buffer.getInt(), buffer.getInt()));
	}	
	
	// length
	public int length() {
		return body.size();
	}
	
	// head (the part inside the hole is excluded)
	public Point head() {
		return body.get(first);
	}

	// next head when the snake moves forward (the part inside the hole is excluded)
	public Point nextHead() {
		return new Point(head().x + Direction.dx[direction], head().y + Direction.dy[direction]);
	}
	
	// next head when the snake moves forward (the part inside the hole is included)
	private Point nextHeadOverall() {
		return new Point(body.get(0).x + Direction.dx[direction], body.get(0).y + Direction.dy[direction]);
	}
	
	// move the snake forward
	public void next(boolean hasFood, boolean goingIn) {
		body.add(0, nextHeadOverall());
		if (goingIn) first++;
		if (!hasFood) {
			body.remove(body.size() - 1);
			if (last < body.size())
				last++;
		}
		else
			last++;
	}
	
	// turn the snake
	public void turn(boolean isRight) {
		direction = (direction + (isRight ? 1 : -1) + 4) % 4;
	}
	
	// make the snake dead
	public void die() {
		alive = false;
	}
	
	// encode the snake
	public byte[] pack() {
		ByteBuffer buffer = ByteBuffer.allocate((6 + body.size() * 2) * 4);
		buffer.putInt(buffer.capacity() - 4);
		buffer.putInt(alive ? 1 : 0);
		buffer.putInt(direction);
		buffer.putInt(first);
		buffer.putInt(last);
		buffer.putInt(body.size());
		for (Point p : body) {
			buffer.putInt(p.x);
			buffer.putInt(p.y);
		}
		return buffer.array();
	}
	
}

// snake segment
class SnakeSegment {
	int x, y, direction, length;
	public SnakeSegment(int x, int y, int direction, int length) {
		this.x = x;
		this.y = y;
		this.direction = direction;
		this.length = length;
	}
}
