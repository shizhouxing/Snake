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
import java.nio.ByteBuffer;

// wall
public class Wall {
	enum Direction {
		HORIZONTAL, VERTICAL
	}
	
	public int length;
	int x, y, dx, dy;
	Direction direction;
	
	private void setDXY() {
		if (direction == Direction.HORIZONTAL) {
			dx = 1; 
			dy = 0;
		}
		else {
			dx = 0;
			dy = 1;
		}
	}
	
	public Wall(int x, int y, int length, Direction direction) {
		this.x = x;
		this.y = y;
		this.length = length;
		this.direction = direction;
		setDXY();
	}
	
	// decode the wall from a packed one
	public Wall(byte[] packed) {
		ByteBuffer buffer = ByteBuffer.wrap(packed);
		x = buffer.getInt();
		y = buffer.getInt();
		length = buffer.getInt();
		direction = Direction.values()[buffer.getInt()];
		setDXY();
	}	
	
	public int getX(int i) {
		return x + dx * i;
	}
	
	public int getY(int i) {
		return y + dy * i;
	}
	
	// encode the wall
	public byte[] pack() {
		ByteBuffer buffer = ByteBuffer.allocate(5 * 4);
		buffer.putInt(buffer.capacity() - 4);
		buffer.putInt(x);
		buffer.putInt(y);
		buffer.putInt(length);
		buffer.putInt(direction.ordinal());
		return buffer.array();
	}
}
