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

package utils;
import java.util.Calendar;
import java.text.SimpleDateFormat;

// to get time
public class Time {
	private static SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
	private static SimpleDateFormat dateTimeFormat = new SimpleDateFormat("YYYYMMDDHHmmss");

	
	public static String getTime() {
		return timeFormat.format(Calendar.getInstance().getTime());
	}
	
	public static String getDateTime() {
		return dateTimeFormat.format(Calendar.getInstance().getTime());
	}
}
