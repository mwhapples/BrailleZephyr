/* Copyright (C) 2015 American Printing House for the Blind Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.aph.braillezephyr;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

public class BZStyledText
{
	private final static char PARAGRAPH_END = 0xfeff;

	private StyledText styledText;

	private int linesPerPage = 25;
	private int charsPerLine = 40;
	String eol = System.getProperty("line.separator");

	private Color pageSeparatorColor;

	public BZStyledText(Shell shell)
	{
		styledText = new StyledText(shell, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		styledText.setLayoutData(new GridData(GridData.FILL_BOTH));
		KeyHandler keyHandler = new KeyHandler();
		styledText.addKeyListener(keyHandler);
		styledText.addVerifyKeyListener(keyHandler);
		styledText.addListener(SWT.Paint, new BZStyledTextPaintListener());

		Font font = new Font(styledText.getDisplay(), "SimBraille", 15, SWT.NORMAL);
		if(font != null)
			styledText.setFont(font);

		pageSeparatorColor = styledText.getDisplay().getSystemColor(SWT.COLOR_BLACK);
	}

	public int getLinesPerPage()
	{
		return linesPerPage;
	}

	public void setLinesPerPage(int linesPerPage)
	{
		this.linesPerPage = linesPerPage;
	}

	public int getCharsPerLine()
	{
		return charsPerLine;
	}

	public void setCharsPerLine(int charsPerLine)
	{
		this.charsPerLine = charsPerLine;
	}

	public Color getPageSeparatorColor()
	{
		return pageSeparatorColor;
	}

	public void setPageSeparatorColor(Color pageSeparatorColor)
	{
		this.pageSeparatorColor = pageSeparatorColor;
	}

	public Font getFont()
	{
		return styledText.getFont();
	}

	public void setFont(Font font)
	{
		styledText.setFont(font);
	}

	public void redraw()
	{
		styledText.redraw();
	}

	private boolean isFirstLineOnPage(int index)
	{
		return index % linesPerPage == 0;
	}

	private boolean isLastLineOnPage(int index)
	{
		return (index + 1) % linesPerPage == 0;
	}

	public void resetLinesPerPage(int linesPerPage)
	{
		this.linesPerPage = linesPerPage;
		for(int i = 1; i < styledText.getLineCount(); i++)
		{
			String line = styledText.getLine(i);

			if(line.length() > 0 && line.charAt(0) == PARAGRAPH_END)
				styledText.replaceTextRange(styledText.getOffsetAtLine(i), line.length(), line.substring(1));

			if(isFirstLineOnPage(i))
				styledText.replaceTextRange(styledText.getOffsetAtLine(i), 0, Character.toString((char)PARAGRAPH_END));
		}
	}

	public void getText(Writer writer) throws IOException
	{
		writer.write(styledText.getLine(0));
		for(int i = 1; i < styledText.getLineCount(); i++)
		{
			writer.write(eol);
			String line = styledText.getLine(i);
			if(line.length() > 0 && line.charAt(0) == PARAGRAPH_END)
			{
				writer.write(0xc);
				writer.write(line.substring(1));
			}
			else
				writer.write(line);
		}
		writer.flush();
	}

	public void setText(String text)
	{
		styledText.setText(text);
	}

	public void setText(Reader reader) throws IOException
	{
		boolean checkLinesPerPage = true;
		boolean changeFormFeed = true;
		char buffer[] = new char[65536];
		int cnt;

		styledText.setText("");
		eol = null;
		while((cnt = reader.read(buffer)) > 0)
		{
			if(checkLinesPerPage)
			{
				checkLinesPerPage = false;
				int lines = 0, i;
				outer:for(i = 0; i < cnt; i++)
				switch(buffer[i])
				{
				case '\r':

					if(eol == null)
						eol = new String("\r\n");
					break;

				case '\n':  lines++;  break;
				case 0xc:

					linesPerPage = lines;
					break outer;
				}

				if(eol == null)
					eol = new String("\n");
				if(i == cnt)
					changeFormFeed = false;
			}

			if(changeFormFeed)
			{
				for(int i = 0; i < cnt; i++)
				if(buffer[i] == 0xc)
					buffer[i] = PARAGRAPH_END;
			}

			styledText.append(new String(buffer, 0, cnt));
		}
	}

	private class KeyHandler implements KeyListener, VerifyKeyListener
	{
		char dotState = 0, dotChar = 0x2800;

		@Override
		public void keyPressed(KeyEvent event)
		{
			switch(event.character)
			{
			case 'f':

				dotState |= 0x01;
				dotChar |= 0x01;
				break;

			case 'd':

				dotState |= 0x02;
				dotChar |= 0x02;
				break;

			case 's':

				dotState |= 0x04;
				dotChar |= 0x04;
				break;

			case 'j':

				dotState |= 0x08;
				dotChar |= 0x08;
				break;

			case 'k':

				dotState |= 0x10;
				dotChar |= 0x10;
				break;

			case 'l':

				dotState |= 0x20;
				dotChar |= 0x20;
				break;

			}
			//System.out.printf("pressed  %c %x\n", dotChar, (int)dotState);
		}

		@Override
		public void keyReleased(KeyEvent event)
		{
			switch(event.character)
			{
			case 'f':

				dotState &= ~0x01;
				break;

			case 'd':

				dotState &= ~0x02;
				break;

			case 's':

				dotState &= ~0x04;
				break;

			case 'j':

				dotState &= ~0x08;
				break;

			case 'k':

				dotState &= ~0x10;
				break;

			case 'l':

				dotState &= ~0x20;
				break;

			}
			//System.out.printf("released %c %x\n", dotChar, (int)dotState);

			if(dotState == 0 && (dotChar & 0xff) != 0)
			{
				dotChar = asciiBraille.charAt((dotChar & 0xff));
				styledText.insert(Character.toString(dotChar));
				styledText.setCaretOffset(styledText.getCaretOffset() + 1);
				dotChar = 0x2800;
			}
		}

		private String asciiBraille = " A1B'K2L@CIF/MSP\"E3H9O6R^DJG>NTQ,*5<-U8V.%[$+X!&;:4\\0Z7(_?W]#Y)=";

		@Override
		public void verifyKey(VerifyEvent event)
		{
			//System.out.println("verify " + event);
			if(event.character > ' ' && event.character < 0x80)
				event.doit = false;
		}
	}

	private class BZStyledTextPaintListener implements Listener
	{
		@Override
		public void handleEvent(Event event)
		{
			event.gc.setForeground(pageSeparatorColor);
			int lineHeight = styledText.getLineHeight();
			int drawHeight = styledText.getClientArea().height;
			int drawWidth = styledText.getClientArea().width;
			int at = 0;
			for(int i = styledText.getTopIndex(); i < styledText.getLineCount(); i++)
			{
				at = styledText.getLinePixel(i);
				if(isFirstLineOnPage(i))
					event.gc.drawLine(0, at, drawWidth, at);
				if(at + lineHeight > drawHeight)
					break;
			}
		}
	}
}
