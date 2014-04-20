package edu.ucsf.library.sprot.ewapvdf;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class MascotResultsDialog  extends Dialog
{
	private static final String defaultMascotBaseUrl = "http://mascot.ckm.ucsf.edu/";
	private static final String defaultMascotUrlString = defaultMascotBaseUrl + "x-cgi/ms-review.exe?CalledFromForm=1&logfile=..%2Flogs%2Fsearches.log&start=-1&howMany=100&pathToData=&column=0&s0=1&s1=1&s2=1&s3=1&s4=1&s7=1&s8=1&s9=1&s10=1&s11=1&s12=1&s14=1&f0=&f1=&f2=&f3=&f4=&f5=&f6=&f7=&f8=&f9=&f10=&f11=&f12=&f13=&f14=";

	private long lastTableRedisplayTime = 0;
	
	public MascotResultsDialog(Shell shell) 
	{
		super(shell);
		setText("Choose Mascot Results to include");
	}

	public MascotResultsDialog(Shell arg0, int arg1) 
	{
		super(arg0, arg1);
		setText("Choose Mascot Results to include");
	}
	
	private static HashMap<String,String> mascotDatFiles = new HashMap<String,String>();

	public static String[][] getMascotSearchLog(String filters[]) 
	{
		ArrayList<ArrayList<String>> aTable = new ArrayList<ArrayList<String>>();
		String urlString = defaultMascotUrlString;
		
		for (int i = 0; filters != null && i < filters.length; i++)
			urlString = urlString.replaceFirst("f" + i + "=", "f" + i + "=" + filters[i]);

		try
		{
			URL url = new URL(urlString);
			URLConnection urlConn = url.openConnection();
			urlConn.setDoInput (true);
			urlConn.setDoOutput (false);
			urlConn.setUseCaches (false);		                 
			urlConn.setRequestProperty("Content-Type", "multipart/mixed; boundary=-----------------------------16838575810113");
			
			BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
	
			String line;
			int tableRow = -1, tableCol = 0;
			String msDataFileName = "MS Data File Name   ";
			while ((line = in.readLine()) != null)
			{
				if (line.startsWith("<TABLE CELLPADDING"))
					tableRow = 0;
				if (tableRow >= 0 && line.contains("<TD") && !line.contains("<input"))
				{
					if (tableCol == 0)
						aTable.add(new ArrayList<String>());
					aTable.get(tableRow).add(line.trim().replaceAll("<[^>]*>", "").trim());
					if (line.contains("../cgi/master_results.pl?file="))
						mascotDatFiles.put(aTable.get(tableRow).get(tableCol), line.substring(line.indexOf("file=") + 5, line.indexOf(".dat\"") +4));
					tableCol++;
				}
				if (tableRow >= 0 && tableCol > 0 && line.contains("</TR>"))
				{
					aTable.get(tableRow).add(msDataFileName);
					msDataFileName = "                                              ";
					tableRow++;
					tableCol = 0;
				}
				if (line.contains("</TABLE>"))
					tableRow = -1;
			}
		}
		catch (Exception e)
		{
			System.out.println(e.getLocalizedMessage());
		}
		String[][] retVal = new String[aTable.size()][];
		for (int row = 0; row < retVal.length; row++)
			retVal[row] = aTable.get(row).toArray(new String[0]);
		return retVal;
	}
	
	public void fillTable(Table table, String data[][])
	{
		String tablContent[][] = new String[data.length-1][data[0].length];
		for (int row = 1; row < data.length; row++)
			tablContent[row-1] = data[row];
//		Arrays.sort(tablContent, new SearchLogEntryComparator());
		for (int row = 1; row < data.length; row++)
		{
			data[row] = tablContent[row-1];
			TableItem item = new TableItem(table, SWT.NONE);
			for (int col = 0; col < data[row].length; col++)
			{
				item.setText(col, data[row][col]);
			}
		}
		for (int i=0; i<data[0].length; i++) {
			table.getColumn (i).pack ();
		}
		
		killFillerThread();
	
		dataFileCol = data[0].length-1;
		numRows = data.length-1;
		nextRowToFetchMSDatafile = nextRowToDisplayMSDatafile = 0;
		rowIDs = new String[numRows];
		msDataFiles = new String[numRows];
		for (int row = 0; row < numRows; row++)
		{
			rowIDs[row] = tablContent[row][0];
			msDataFiles[row] = "";
		}
		msDataFileFiller = new Thread(new FillMSDataFileNames(this.getParent().getDisplay()));
		msDataFileFiller.start();
	}
	
	//variables for displaying the file names
	int nextRowToFetchMSDatafile = 0;
	int nextRowToDisplayMSDatafile = 0;
	int numRows = 0;
	String rowIDs[] = null;
	String msDataFiles[] = null;
	Thread msDataFileFiller = null;
	boolean threadAbort = false;
	int dataFileCol = 0;
	
	void killFillerThread()
	{
		if (msDataFileFiller == null)
		{
			System.out.println("thread already dead.");
		}
		else
		{
			System.out.print("killing thread ");
			threadAbort = true;
			while (msDataFileFiller != null && msDataFileFiller.isAlive())
			{
				System.out.print(".");
				try{Thread.sleep(50);}catch(Exception e){}
			}
			threadAbort = false;
			System.out.println("dead");
		}
	}
	class FillMSDataFileNames implements Runnable
	{
		Display display;
		public FillMSDataFileNames(Display display)
		{
			super();
			this.display = display;
		}
		public void run()
		{
			while (nextRowToFetchMSDatafile < numRows)
			{
				if (threadAbort)
					return;
				if (msDataFiles[nextRowToFetchMSDatafile].isEmpty())
				{
					msDataFiles[nextRowToFetchMSDatafile] = getMSDataFile(mascotDatFiles.get(rowIDs[nextRowToFetchMSDatafile]));
					nextRowToFetchMSDatafile++;
					display.wake();
				}
				else
				{
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
	private String getMSDataFile(String datFile)
	{
		System.out.println("getting ms Data file for " + datFile);
		String msDataFile = "";
		
		try {
//			System.out.println(datFile);
			String urlx = defaultMascotBaseUrl + "cgi/export_dat_2.pl?file=..%2Fdata%2F" + datFile + "&do_export=1&prot_hit_num=1&prot_acc=1&pep_query=1&pep_rank=1&pep_isbold=1&pep_exp_mz=1&_showallfromerrortolerant=0&_onlyerrortolerant=0&_noerrortolerant=0&_show_decoy_report=0&export_format=CSV&_sigthreshold=0.05&REPORT=AUTO&_server_mudpit_switch=99999999&_ignoreionsscorebelow=.05&_showsubsets=0&_requireboldred=1&search_master=1&show_header=1&show_mods=1&show_params=1&show_format=1&protein_master=1&prot_score=1&prot_desc=1&prot_mass=1&prot_matches=1&prot_cover=1&peptide_master=1&pep_exp_mr=1&pep_exp_z=1&pep_calc_mr=1&pep_delta=1&pep_miss=1&pep_score=1&pep_expect=1&pep_seq=1&pep_var_mod=1&pep_scan_title=1";		
			URL url = new URL(urlx);
			URLConnection urlConn = url.openConnection();
			urlConn.setDoInput (true);
			urlConn.setDoOutput (false);
			urlConn.setUseCaches (false);		                 
			urlConn.setRequestProperty("Content-Type", "multipart/mixed; boundary=-----------------------------16838575810113");
			//DataInputStream in = new DataInputStream(urlConn.getInputStream());
			BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

			String line = "";
			while(true)
			{
				line  = in.readLine();
				//System.out.println(line);
				if (line == null)
					line = "";
				if (line.startsWith("Peak list data path"))
				{
					String spl[] = parseCSVLine(line);
					msDataFile = spl[1];
					break;
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return msDataFile.substring(msDataFile.lastIndexOf("\\")+1);
	}
	public static  String[] parseCSVLine(String line)
	{
		ArrayList<String> vals = new ArrayList<String>();
		
		int state = 0; // 0 = outside "...", 1 = inside "..."
		String value = "";
		for (int i = 0; i < line.length(); i++)
		{
			char c = line.charAt(i);
			
			if (state == 0 && c == ',')
			{
				if (i < line.length() - 1 && line.charAt(i+1) == '"')
				{
					i++;
					state = 1;
				}
				vals.add(value);
				value = "";
			}
			else if (state == 1 && c == '"' && (i == line.length()-1 || line.charAt(i+1) == ','))
			{
				//i++;
				state = 0;
				//vals.add(value);
				//value = "";
			}
			else if (state == 1 || c != ',')
				value += c;
			
		}
		vals.add(value);
		return (String[]) vals.toArray(new String[0]);
	}
	
	private void updateTable()
	{
		
	}
	private void sleep(int n)
	{
		try{Thread.sleep(n);}catch(Exception e){};
	}
	private class FilterListener implements FocusListener
	{
		Table table;
		Text  filters[];
		int column;
		
		public FilterListener(int column, Text[] filters, Table table)
		{
			this.column = column;
			this.table = table;
			this.filters = filters;
		}
		String filterText;
		public void focusGained(FocusEvent e)
		{
			filterText = filters[column].getText();
		}
		public void focusLost(FocusEvent e)
		{
			if (filterText.equals(filters[column].getText()))
				return;
			for (int row = table.getItemCount()-1; row >= 0; row--)
				table.remove(row);
			String filterText[] = new String[filters.length];
			for (int col = 0; col < filters.length; col++)
				filterText[col] = filters[col].getText();
			try {
				String newTableData[][] = getMascotSearchLog(filterText);
				fillTable(table, newTableData);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
 	}
	
	private class FilterModifyListener implements ModifyListener
	{
		Table table;
		Text  filters[];
		int column;
		long timelastcalled = 0;
		
		public FilterModifyListener(int column, Text[] filters, Table table)
		{
			this.column = column;
			this.table = table;
			this.filters = filters;
		}

		public void modifyText(ModifyEvent arg0) 
		{
			// notify main event loop to redisplay
			lastTableRedisplayTime = System.currentTimeMillis();
		}
		
	}
	
	private class FilterTraverseListener implements TraverseListener
	{
		public void keyTraversed(TraverseEvent e) {
			if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS || e.detail == SWT.TRAVERSE_RETURN) {
				e.doit = true;
			}
		}

	}
	
	public String[] open()
	{
		Table table = null;
		String[][] tableData = getMascotSearchLog(null);

		String result[] = null;
		
		Shell parent = getParent();
		Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		shell.setText(getText());
		shell.setLayout(new GridLayout(1, true));
		
		Composite filterBox = new Composite(shell, SWT.NONE);
		
		GridLayout filterBoxLayout = new GridLayout(tableData[0].length-1, false); // Create a n-column layout
		GridData filterBoxData = new GridData(GridData.FILL, SWT.FILL, true, false); // Instruct layout manager to fill both horizontally and vertically, and to grab excessive space
		filterBox.setLayout(filterBoxLayout);
		filterBox.setLayoutData(filterBoxData); // Associate griddata to composite

		
		// create the table with mascot results
		table = new Table (shell, SWT.CHECK | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
		table.setLinesVisible (true);
		table.setHeaderVisible (true);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = 200;
		table.setLayoutData(data);

		for (int i = 0; i < tableData[0].length; i++)
		{
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(tableData[0][i]);
			column.pack ();
		}
		fillTable(table, tableData);
		
		// set up the filters
		final Text filters[] = new Text[tableData[0].length-1];
		for (int col = 0; col < tableData[0].length-1; col++)
		{
			org.eclipse.swt.widgets.Label l = new org.eclipse.swt.widgets.Label(filterBox, SWT.SHADOW_NONE);
			l.setText(tableData[0][col]);
		}
		for (int col = 0; col < tableData[0].length-1; col++)
		{
			filters[col] = new Text(filterBox, SWT.BORDER | SWT.MULTI);
			//filters[col].addFocusListener(new FilterListener(col, filters, table));
			filters[col].addTraverseListener(new FilterTraverseListener());
			filters[col].addModifyListener(new FilterModifyListener(col, filters, table));
		}

		Composite lowerPane = new Composite(shell, SWT.NONE);
		lowerPane.setLayout(new GridLayout(2, true));
		
	    final Button buttonOK = new Button(lowerPane, SWT.PUSH);
	    buttonOK.setText("Ok");
	    buttonOK.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
	    final int[] state = {0};
	    buttonOK.addListener(SWT.Selection, new Listener() {
	        public void handleEvent(Event event) { state[0] = 1; }
	    });
	    Button buttonCancel = new Button(lowerPane, SWT.PUSH);
	    buttonCancel.setText("Cancel");
	    buttonCancel.addListener(SWT.Selection, new Listener() {
	        public void handleEvent(Event event) { state[0] = 2; }
	    });
	    shell.pack();
		shell.open();
		Display display = parent.getDisplay();
		while (!shell.isDisposed()) {
			
			// see if filter modified so would need to re display the table with new filters
			if (lastTableRedisplayTime > 0 && System.currentTimeMillis() - lastTableRedisplayTime > 500)
			{
				for (int row = table.getItemCount()-1; row >= 0; row--)
					table.remove(row);
				String filterText[] = new String[filters.length];
				for (int col = 0; col < filters.length; col++)
					filterText[col] = filters[col].getText();
				String newTableData[][] = getMascotSearchLog(filterText);
				fillTable(table, newTableData);
				lastTableRedisplayTime = 0;
			}
			if (!display.readAndDispatch())			{
				while (nextRowToDisplayMSDatafile < nextRowToFetchMSDatafile)
				{
					if(!msDataFiles[nextRowToDisplayMSDatafile].isEmpty())
					{
						table.getItem(nextRowToDisplayMSDatafile).setText(dataFileCol, msDataFiles[nextRowToDisplayMSDatafile]);
						nextRowToDisplayMSDatafile++;
					}
					try { Thread.sleep(10); } catch (InterruptedException e) { }
				}
				display.sleep ();
			}

			if (state[0] == 1)
			{
				int n = 0;
				for (int i = 0; i < table.getItemCount(); i++)
					if (table.getItem(i).getChecked())
						n++;
				result = new String[n];
				int j = 0;
				for (int i = 0; i < table.getItemCount(); i++)
					if (table.getItem(i).getChecked())
						result[j++] = mascotDatFiles.get(table.getItem(i).getText(0));
				shell.dispose();
			}
			if (state[0] == 2)
				shell.dispose();
		}
		killFillerThread();
		return result;
	}


}
