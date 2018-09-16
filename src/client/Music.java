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
import java.math.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.*;
import javax.swing.*;

// music player
public class Music {
	private boolean valid = true;
	private AudioClip clip = null;
	
	public Music(File file) {
		URL url = null;
		try {              
			if (file.canRead())
				url = file.toURI().toURL();
			else 
				valid = false;
		} catch (MalformedURLException e) {
			valid = false;
		}
		if (valid)
			clip = Applet.newAudioClip(url);
	}
	
	public boolean isValid() {
		return valid;
	}
	
	public void start() {
		if (clip != null)
			clip.loop();
	}
	
	public void stop() {
		if (clip != null)
			clip.stop();
	}
}
