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
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

// spinner
public class Spinner {
	public static JSpinner getSpinner(int value, int min, int max, boolean allowInvalid) {
		SpinnerNumberModel spinnerModel = new SpinnerNumberModel(value, min, max, 1);
		JSpinner spinner = new JSpinner(spinnerModel);
		JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "#");
		spinner.setEditor(editor);
		JFormattedTextField textField = ((JSpinner.NumberEditor)spinner.getEditor()).getTextField();
		textField.setEditable(true);
		textField.setHorizontalAlignment(SwingConstants.CENTER);
		((NumberFormatter)((DefaultFormatterFactory)textField
			.getFormatterFactory()).getDefaultFormatter()).setAllowsInvalid(allowInvalid);
		return spinner;
	}
}
