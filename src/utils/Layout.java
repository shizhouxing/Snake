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
import java.io.*;
import java.util.*;
import java.math.*;
import java.awt.*;
import javax.swing.*;

// GridBagLayout helper
public class Layout {
	public static GridBagConstraints getConstraints(
			int gridx, int gridy, int gridwidth, int gridheight, 
			double weightx, double weighty) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = gridx;
		c.gridy = gridy;
		c.gridwidth = gridwidth;
		c.gridheight = gridheight;
		c.weightx = weightx;
		c.weighty = weighty;
		c.fill = GridBagConstraints.BOTH;
		return c;
	}	
	
	public static GridBagConstraints getConstraints(
			int gridx, int gridy, int gridwidth, int gridheight, 
			double weightx, double weighty, Insets insets) {
		GridBagConstraints c = getConstraints(
			gridx, gridy, gridwidth, gridheight, weightx, weighty);
		c.insets = insets;
		return c;
	}		
}
