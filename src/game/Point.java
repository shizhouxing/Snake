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

// point
public class Point {
	public int x, y;
	
	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public Point(byte[] packed) {
		ByteBuffer buffer = ByteBuffer.wrap(packed);
		x = buffer.getInt();
		y = buffer.getInt();
	}
	
	public boolean equals(Point p) {
		return x == p.x && y == p.y;
	}
	
	public byte[] pack() {
		ByteBuffer buffer = ByteBuffer.allocate(3 * 4);
		buffer.putInt(buffer.capacity() - 4);
		buffer.putInt(x);
		buffer.putInt(y);
		return buffer.array();
	}
}
