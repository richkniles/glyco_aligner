package edu.ucsf.library.sprot.ewapvdf;

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

public class SWTTableOutputStream extends OutputStream
{
	Table table;
	TableItem curRow;
	int col;
	String curCellString = "";
	
	public SWTTableOutputStream(Table table)
	{
		super();
		this.table = table;
		col = 0;
		while(table.getItemCount() > 0)
			table.remove(0);
		curRow = new TableItem(table, SWT.NONE);
		curRow.setText("");
	}

	public void write(int b) throws IOException 
	{
		if (b == '\t')
		{
			curRow.setText(col, curCellString);
			curCellString = "";
			col++;
			if (table.getColumnCount() <= col)
			{
				TableColumn c = new TableColumn(table, SWT.NONE);
				c.setResizable(true);
				c.setMoveable(true);
				String colName = "" + (char) ('A' + col);
				if (col >= 26)
					colName = "" + (char) ('A' + (col/26-1)) + (char) ('A' + ((col+1)%26-1));
				c.setText(colName);
				c.setWidth(50);
			}

		}
		else if (b == '\n')
		{
			curRow.setText(col, curCellString);
			curCellString = "";
			curRow = new TableItem(table, SWT.NONE);
			col = 0;
		}
		else if (b == '\r')
			return;
		else
		{
			//String curCellString = curRow.getText(col);
			curCellString += (char) b;
			//curRow.setText(col, curCellString);
		}

	}
	
	
}
