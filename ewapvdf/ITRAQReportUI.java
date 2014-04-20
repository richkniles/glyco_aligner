package edu.ucsf.library.sprot.ewapvdf;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.graphics.*;

import com.intrinsyc.license.a;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeSet;


public class ITRAQReportUI 
{
	static Shell theShell;
	static List theFileList;
	static List proteinItemsList;
	static List peptideItemsList;
	static Table previewTable;
	static Table labelAliasTable;
	static boolean needReportUpdate = false;
	static Button loadOnlyUsedPeptides;
	static Button collapseWinners;
	static Text minProtScoreText;
	static Text minPepScoreText;
	static String curFilterPath = "C:\\";
	static String curFASTA = "";
	
	static String statusMsg = "";

	static 		ITRAQReport theReport;


	static void restoreFilterPath()
	{
		try {
			BufferedReader in = new BufferedReader(new FileReader("cur path.txt"));
			curFilterPath = in.readLine();
			if (!(new File(curFilterPath).exists()))
					curFilterPath = "C:";
		} catch (Exception e) {
			// ignore
		}
	}
	static void saveCurFilterPath()
	{
		try {
			PrintStream out = new PrintStream(new FileOutputStream("cur path.txt"));
			out.println(curFilterPath);
		} catch (Exception e) {
			// ignore
		}
	}
	public static boolean getCollapseWinners()
	{
		return collapseWinners.getSelection();
	}
	public static boolean getSummarizePeptides()
	{
		return summarizePeptides.getSelection();
	}
	public static boolean getUsedFlag()
	{
		return loadOnlyUsedPeptides.getSelection();
	}
	public static double getMinProtScore()
	{
		String t = minProtScoreText.getText();
		try {
			return Double.parseDouble(t);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
	public static double getMinPepScore()
	{
		String t = minPepScoreText.getText();
		try {
			return Double.parseDouble(t);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
	public static void updateProgressIndicator(int n)
	{
		System.out.print(".");
		setStatus(theStatusWindow.getText() + ".");
	}
	public static void reLoadFiles()
	{
		System.out.print("Aborting load");
		setStatus("Aborting load");
		FileLoader.abort();
		System.out.println();
		setStatus("");
		FileLoader.loadFiles(getFileList(), getUsedFlag(), getMinProtScore(), getMinPepScore(), getCollapseWinners());
		ITRAQReport.prevMasterList = null;
		needReportUpdate = true;
		ITRAQReport.abortReportPrinting = true;
	}
	private static void fillInputFilesTab(TabFolder tabFolder, TabItem inputFilesTab)
	{
		Composite inputFilesComposite = new Composite(tabFolder, SWT.NONE);
		inputFilesComposite.setLayout(new GridLayout(1, false));
		Composite buttonComposite = new Composite(inputFilesComposite, SWT.NONE);
		buttonComposite.setLayout(new GridLayout(4, false));
		final List fileList = new List(inputFilesComposite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		theFileList = fileList;
		fileList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		for (int i = 0; i < 20; i++)
			fileList.add("MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM");

		fileList.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				// make sure all files are selected when select a directory
				String sels[] = fileList.getSelection();
				for (int i = 0; i < sels.length; i++)
					if (! sels[i].startsWith("  "))
					{
						String path = sels[i];
						int j = 0;
						for (j = 0; j < fileList.getItemCount(); j++)
							if (fileList.getItem(j).equals(path))
								break;
						for (j++; j < fileList.getItemCount() && fileList.getItem(j).startsWith("   "); j++)
							fileList.select(j);
							
					}
			}
		});
		
		
		Button plusButton = new Button(buttonComposite, SWT.PUSH);
		plusButton.setText("  Add file(s)...  ");
		plusButton.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				FileDialog dialog = new FileDialog (theShell, SWT.OPEN + SWT.MULTI);
				String [] filterNames = new String [] {"Peptide Summary Files"};
				dialog.setFilterPath(curFilterPath);
				String [] filterExtensions = new String [] {"*.txt"};
				dialog.setFilterNames (filterNames);
				dialog.setFilterExtensions (filterExtensions);
				String ff = dialog.open();
				String path = dialog.getFilterPath();
				curFilterPath = path;
				saveCurFilterPath();
				String files[] = dialog.getFileNames();
				
				int insertLoc = fileList.getItemCount();
				// see if path already in list
				int pathLoc = -1;
				for (int i = 0; i < fileList.getItemCount(); i++)
					if (fileList.getItem(i).equals(path + ":"))
						pathLoc = i;
				if (pathLoc != -1)
					for (insertLoc = pathLoc+1; insertLoc < fileList.getItemCount(); insertLoc++)
						if (!fileList.getItem(insertLoc).startsWith(" "))
							break;
				if (pathLoc == -1 && files.length > 0)
					fileList.add(path + ":", insertLoc++);
				for (int i = 0; i < files.length; i++)
				{
					fileList.add("        " + files[i], insertLoc++);
					needReportUpdate = true;
					ITRAQReport.abortReportPrinting = true;
				}
				reLoadFiles();
			}
			
		});
		
		Button mascotButton = new Button(buttonComposite, SWT.PUSH);
		mascotButton.setText("Add Mascot results...");
		mascotButton.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				MascotResultsDialog dialog = new MascotResultsDialog (theShell, SWT.OPEN + SWT.MULTI);
				String ff[] = dialog.open();
				
				if (ff == null)
					return;
				
				int insertLoc = fileList.getItemCount();
				for (int i = 0; i < ff.length; i++)
				{
					fileList.add("        " + ff[i], insertLoc++);
					needReportUpdate = true;
					ITRAQReport.abortReportPrinting = true;
				}
				reLoadFiles();
			}
			
		});

		
		
		Composite protScComp = new Composite(buttonComposite, SWT.NONE);
		protScComp.setLayout(new GridLayout(2, false));
		Label protScoreLabel = new Label(protScComp, SWT.NONE);
		protScoreLabel.setText("Min prot \"unused\" score: ");
		Text protScoreText = new Text(protScComp, SWT.NONE);
		protScoreText.setText("             ");
		protScoreText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent arg0) {
				reLoadFiles();
			}}
		);
		minProtScoreText = protScoreText;
		
		Composite pepScoreComp = new Composite(buttonComposite, SWT.NONE);
		pepScoreComp.setLayout(new GridLayout(2, false));
		Label pepScoreLabel = new Label(pepScoreComp, SWT.NONE);
		pepScoreLabel.setText("Min peptide \"conf\" score: ");
		Text pepScoreText = new Text(pepScoreComp, SWT.NONE);
		pepScoreText.setText("       ");
		pepScoreText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent arg0) {
				reLoadFiles();
			}}
		);
		minPepScoreText = pepScoreText;

		
		
		
		Button minusButton = new Button(buttonComposite, SWT.PUSH);
		minusButton.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				int i;
				while((i = fileList.getSelectionIndex()) != -1)
				{
					fileList.remove(i);
					needReportUpdate = true;
					ITRAQReport.abortReportPrinting = true;
				}
			}
		});
		minusButton.setText("  Remove file(s)  ");
		
		//Composite spacer = new Composite(buttonComposite, SWT.NONE);
		
		Button collapseWinnersButton = new Button(buttonComposite, SWT.CHECK);
		collapseWinnersButton.setText("Collapse groups with same winner.");
		collapseWinnersButton.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				reLoadFiles();
			}
		});
		collapseWinners = collapseWinnersButton;
		
//		Label spacer = new Label(buttonComposite, SWT.NONE);
//		spacer.setText("     ");
		
		Button usedButton = new Button(buttonComposite, SWT.CHECK);
		usedButton.setText("Include only \"used\" peptides");
		usedButton.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				reLoadFiles();
			}
		});
		loadOnlyUsedPeptides = usedButton;
		
		Composite fastaComposite = new Composite(buttonComposite, SWT.NONE);
		fastaComposite.setLayout(new GridLayout(3, false));
		Label fastaLabel = new Label(fastaComposite, SWT.NONE);
		fastaLabel.setText("FASTA file (for peptide context)");
		final Text fastaText = new Text(fastaComposite, SWT.NONE);
		fastaText.setText("                    ");

		Button fastaBrowseButton = new Button(fastaComposite, SWT.PUSH);
		fastaBrowseButton.setText("Browse...");
		fastaBrowseButton.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				FileDialog dialog = new FileDialog (theShell, SWT.OPEN + SWT.MULTI);
				String [] filterNames = new String [] {"FASTA Files"};
				String [] filterExtensions = new String [] {"*.fasta"};
				dialog.setFilterNames (filterNames);
				dialog.setFilterExtensions (filterExtensions);
				String ff = dialog.open();
				if (ff == null)
					return;
				String path = dialog.getFilterPath();
				curFilterPath = path;
				curFASTA = ff;
				System.out.println(ff);
				String fastaName = new File(curFASTA).getName();
				fastaText.setText(fastaName);
			}
			
		});
		
		
		
		
		
		
		
		
		
		
		
		
		inputFilesTab.setControl(inputFilesComposite);

		for (int i = 0; i < 20; i++)
			fileList.remove(0);

	}
	static ToolItem summarizePeptides;
	private static List fillReportItemsTab(TabFolder tabFolder, TabItem itemsTab, String[] itemsForMenu, String popupName)
	{
		Composite itemsComposite = new Composite(tabFolder, SWT.NONE);
		itemsComposite.setLayout(new GridLayout(1, false));
		
		final ToolBar bar = new ToolBar(itemsComposite, SWT.NONE );
		final ToolItem item = new ToolItem(bar, SWT.DROP_DOWN | SWT.BORDER);
		item.setText("  Add " + popupName + "  ");
		
		ToolItem spacer = new ToolItem(bar, SWT.SEPARATOR);

		final ToolItem deleteButton = new ToolItem(bar, SWT.PUSH);
		deleteButton.setText("  Remove  ");
		
		if (popupName.equals("Peptide items"))
		{
			summarizePeptides = new ToolItem(bar, SWT.CHECK);
			summarizePeptides.setText("Show each peptide once");
			summarizePeptides.addSelectionListener(new SelectionAdapter()
			{
				public void widgetSelected(SelectionEvent e)
				{
					needReportUpdate = true;
					ITRAQReport.abortReportPrinting = true;
				}
			});
		}
		
		final Menu reportItemsMenu = new Menu(theShell, SWT.POP_UP);
		MenuItem itm;// = new MenuItem(reportItemsMenu, SWT.PUSH);
		//itm.setText("SP Accession");
		//itm = new MenuItem(reportItemsMenu, SWT.PUSH);
		//itm.setText("Entry Name");

		for (int i = 0; i < itemsForMenu.length; i++)
		{
			itm = new MenuItem(reportItemsMenu, SWT.PUSH);
			itm.setText(itemsForMenu[i]);
			itm.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					System.out.println(((MenuItem)e.widget).getText());
				}
			});
		}
	    item.addListener(SWT.Selection, new Listener() {
		      public void handleEvent(Event event) {
		          Rectangle rect = item.getBounds();
		          Point pt = new Point(rect.x, rect.y + rect.height);
		          pt = bar.toDisplay(pt);
		          reportItemsMenu.setLocation(pt.x, pt.y);
		          reportItemsMenu.setVisible(true);
		      }
		    });
		
		final List reportItemsList = new List(itemsComposite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		reportItemsList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		itemsTab.setControl(itemsComposite);

		deleteButton.addSelectionListener(new SelectionAdapter()
		{
			public void widgetSelected(SelectionEvent e)
			{
				int i;
				while((i = reportItemsList.getSelectionIndex()) != -1)
				{
					reportItemsList.remove(i);
					needReportUpdate = true;
					ITRAQReport.abortReportPrinting = true;
				}
			}
		});
		reportItemsList.addMouseListener(new MouseListener(){

			public void mouseDoubleClick(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			public void mouseDown(MouseEvent arg0) {
				if (arg0.y >  reportItemsList.getItemHeight()*reportItemsList.getItemCount())
					reportItemsList.deselectAll();
			}

			public void mouseUp(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}});

		for (int i = 0; i < reportItemsMenu.getItemCount(); i++)
		{
			itm = reportItemsMenu.getItem(i);
			itm.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					if (((MenuItem)e.widget).getText().equals("Quant Peptides"))
					{
						if (getMinPepScore() > 0)
						{
							MessageBox m = new MessageBox(theShell, SWT.ICON_WARNING | SWT.OK);
							m.setMessage("Warning: using a peptide score threshold in the input files tab\nmay cause some peptides to not be read\nthus affecting the Quant Peptide count.");
							m.open();
						}
					}
					if (reportItemsList.getSelectionCount() == 0)
						reportItemsList.add(((MenuItem)e.widget).getText());
					else
						reportItemsList.add(((MenuItem)e.widget).getText(), reportItemsList.getSelectionIndex());
					needReportUpdate = true;
					ITRAQReport.abortReportPrinting = true;
				}
			});
		}		
		itemsComposite.pack();
		return reportItemsList;
	}

	private static void createPreviewTable(TabFolder tabFolder, TabItem previewTab)
	{
		final Composite comp = new Composite(tabFolder, SWT.NONE);

		previewTable = new Table(comp, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);		
		
		previewTable.setLinesVisible(true);
		previewTable.setHeaderVisible(true);
		
		//previewTable.setSize(10,10);
		previewTab.setControl(comp);
		for (int i = 0; i < 20; i++)
		{
			TableColumn c = new TableColumn(previewTable, SWT.NONE);
			c.setResizable(true);
			c.setMoveable(true);
			c.setText("" + (char) ('A' + i));
			c.setWidth(50);
		}
		for (int i = 0; i < 20; i++)
		{
			TableItem it = new TableItem(previewTable, SWT.NONE);
		}
		previewTable.pack();
		
		comp.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Rectangle area = comp.getClientArea();
				previewTable.setSize(area.width, area.height);
			}
		});
	    Listener sortListener = new Listener(){
			public void handleEvent(Event e) {
				TableColumn oldSortColumn = previewTable.getSortColumn();
				int oldDirection = previewTable.getSortDirection();
				
				TableColumn column = (TableColumn) e.widget;
				int direction = SWT.UP;
				if (column == oldSortColumn && oldDirection == SWT.UP)
					direction = SWT.DOWN;
				
				int sortColumn = -1;
				for (int i = 0; i < previewTable.getColumnCount(); i++)
					if (column == previewTable.getColumn(i))
						sortColumn = i;
				TableItem[] items = previewTable.getItems();
				int numHdrLines = theReport.getNumHeaderLines();
				int numBlankAtBottom = 0;
				for (int i = items.length-1; i > 0; i--)
				{
					boolean isBlank = true;
					for (int j = 0; j < previewTable.getColumnCount() && isBlank; j++)
					{
						if (!items[i].getText(j).trim().isEmpty())
							isBlank = false;
					}
					if (isBlank)
						numBlankAtBottom++;
				}
				String tableText[][] = new String[items.length-numHdrLines-numBlankAtBottom][previewTable.getColumnCount()];
				boolean isNumeric = true;
				for (int i = 0; i < items.length - numHdrLines - numBlankAtBottom; i++)
				{
					for (int j = 0; j < previewTable.getColumnCount(); j++)
					{
						tableText[i][j] = previewTable.getItem(i+numHdrLines).getText(j);
					}
					if (!tableText[i][sortColumn].trim().isEmpty())
					{
						try { double x = Double.parseDouble(tableText[i][sortColumn]); }
						catch (Exception ee) { 
							isNumeric = false; }
					}
				}
				Arrays.sort(tableText, new TableSortComparator(sortColumn, direction, isNumeric)); // not numeric;
				for (int i = 0; i < tableText.length; i++)
					for (int j = 0; j < tableText[i].length; j++)
						items[i + numHdrLines].setText(j, tableText[i][j]);
				previewTable.setSortColumn(column);
				previewTable.setSortDirection(direction);
			}
	    	
	    };
	    for (int i = 0; i < previewTable.getColumnCount(); i++)
	    	previewTable.getColumn(i).addListener(SWT.Selection, sortListener);

		
	}
	
	static String[] prevProteinItems = null;
	static String[] prevPeptideItems = null;
	static String[] prevInputFiles = null;
	static String[] prevLabelAliasList = null;
	
	private static boolean arraysEqual(String a[], String b[])
	{
		if (a == null || b == null && a != b)
			return false;
		if (a.length != b.length)
			return false;
		for (int i = 0; i < a.length; i++)
			if (!a[i].equals(b[i]))
				return false;
		return true;
	}
	static String[] getLabelAliasList()
	{
		String ret[] = new String[labelAliasTable.getItemCount()];
		
		for (int i = 0; i < ret.length; i++)
		{
			TableItem row = labelAliasTable.getItem(i);
			ret[i] = row.getText(0) + "\t" + row.getText(1) + "\t" + row.getText(2);
		}
		return ret;
	}
	static String[] getFileList()
	{
		ArrayList<String> files = new ArrayList<String>();
		String[] fileItems = theFileList.getItems();
		String path = "";
		for (String file : fileItems)
		{
			if (!file.startsWith(" "))
			{
				path = file.substring(0,file.length()-1) + "/";
				continue;
			}
			if (!file.endsWith(".dat"))
				file = path + file.trim();
			else
				file = file.trim();
			files.add(file);
		}
		return files.toArray(new String[0]);
	}
	
	public static void updateReport()
	{
		if (!needReportUpdate || !FileLoader.allFilesLoaded())
			return;
		needReportUpdate = false;
		
		if (theFileList.isDisposed() || theFileList.getItemCount() == 0)
			return;
		// see if info changed
/*		if (arraysEqual(prevInputFiles, getFileList()) 
				&& arraysEqual(prevProteinItems, proteinItemsList.getItems()) 
					&& arraysEqual(prevPeptideItems, peptideItemsList.getItems())
						&& arraysEqual(prevLabelAliasList, getLabelAliasList())
			)
			return;
*/		
		
		
		prevInputFiles = getFileList(); 
		prevProteinItems = proteinItemsList.getItems(); 
		prevPeptideItems = peptideItemsList.getItems();
		prevLabelAliasList = getLabelAliasList();
	
		if (prevInputFiles == null)
			return;
		
		if (arraysEqual(prevInputFiles, getFileList()))
			fillAliasTable(prevInputFiles, labelAliasTable);
		
		try 
		{
			if (theReport != null)
				ITRAQReport.abortReportPrinting = true;
			System.out.print("aborting printing");
			while(ITRAQReport.isReportPrinting)
			{
				if (!Display.getCurrent().readAndDispatch())
					Display.getCurrent().sleep();
				System.out.print(".");
			}
			System.out.println();
			theReport = new ITRAQReport(prevInputFiles, prevProteinItems, prevPeptideItems, prevLabelAliasList, getCollapseWinners(), getSummarizePeptides(), curFASTA);
		} 
		catch (Exception e1) 
		{
			if (e1.getMessage() != null)
			{
				MessageBox m = new MessageBox(theShell, SWT.ICON_ERROR | SWT.OK);
				m.setMessage(e1.getMessage());
				m.open();
			}
			e1.printStackTrace();
			return;
		}
		System.out.println("Printing report");
		setStatus("Printing report");
		PrintStream out = new PrintStream(new SWTTableOutputStream(previewTable));
		
		try 
		{
			ReportPrinter reportPrinter = new ReportPrinter(theReport, out, prevPeptideItems.length > 0);
			Display.getDefault().asyncExec(reportPrinter);

//			ReportPrinter.printReport(theReport, out, prevPeptideItems.length > 0);
//			theReport.printProteinReport(null, out, prevPeptideItems.length > 0);
		} 
		catch (Exception e) 
		{
			MessageBox b = new MessageBox(theShell, SWT.ICON_ERROR | SWT.OK);
			b.setMessage(e.getLocalizedMessage());
			b.setText("Error");
		}
	}
	private static MenuItem theFindMenuItem = null;
	
	private static void setUpMenus()
	{
		Menu bar = new Menu (theShell, SWT.BAR);
		theShell.setMenuBar (bar);


		MenuItem fileItem = new MenuItem (bar, SWT.CASCADE);
		fileItem.setText ("&File");
		//System.out.println(theShell.getMenuBar().getItem(0).getText());		
		Menu fileMenu = new Menu(theShell, SWT.DROP_DOWN);
		fileItem.setMenu(fileMenu);
		//MenuItem newItem = new MenuItem(fileMenu, SWT.PUSH);
		//newItem.setText("New...");
		//MenuItem openItem = new MenuItem(fileMenu, SWT.PUSH);
		//openItem.setText("Open...");
		MenuItem saveItem = new MenuItem(fileMenu, SWT.PUSH);
		saveItem.setText("Save report...");
		saveItem.addListener(SWT.Selection, new Listener(){

			public void handleEvent(Event arg0) {
				ITRAQReport theReport;
				try 
				{
					theReport = new ITRAQReport(prevInputFiles, prevProteinItems, prevPeptideItems, prevLabelAliasList, getCollapseWinners(), getSummarizePeptides(), curFASTA);
				} 
				catch (Exception e1) 
				{
					if (e1.getMessage() != null)
					{
						MessageBox m = new MessageBox(theShell, SWT.ICON_ERROR | SWT.OK);
						m.setMessage(e1.getMessage());
						m.open();
					}
					e1.printStackTrace();
					return;
				}
				
				try 
				{
					FileDialog saveDlog = new FileDialog(theShell, SWT.SAVE);
					saveDlog.setFilterPath(curFilterPath);
					String fileName = (new FileDialog(theShell, SWT.SAVE)).open();
					if (fileName == null)
						return;
					PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(fileName)));
					theReport.printProteinReport(null, out, prevPeptideItems.length > 0);
					out.close();
				} 
				catch (Exception e) 
				{
					MessageBox m = new MessageBox(theShell, SWT.ICON_ERROR | SWT.OK);
					m.setMessage(e.getMessage());
					m.open();
				}
				
			}
		});
		
		MenuItem editItem = new MenuItem (bar, SWT.CASCADE);
		editItem.setText ("&Edit");
		Menu editMenu = new Menu(theShell, SWT.DROP_DOWN);
		editItem.setMenu(editMenu);
		MenuItem copyItem = new MenuItem(editMenu, SWT.PUSH);
		copyItem.setText("&Copy");
		copyItem.setEnabled(false);
		MenuItem pasteItem = new MenuItem(editMenu, SWT.PUSH);
		pasteItem.setText("&Paste");
		pasteItem.setEnabled(false);
		
		MenuItem findItem = new MenuItem(editMenu, SWT.PUSH);
		theFindMenuItem = findItem;
		findItem.setEnabled(false);
		findItem.setText("&Find\tCtrl+F");
		
		if (System.getProperty("os.name").toLowerCase().indexOf( "mac" ) >= 0)
			findItem.setAccelerator(SWT.COMMAND + 'F');
		else
			findItem.setAccelerator(SWT.CTRL + 'F');
		
		findItem.addListener(SWT.Selection, new Listener(){

			public void handleEvent(Event arg0) 
			{
				// put up a dialog to get find text
				final Shell dlg = new Shell(theShell.getDisplay());
				dlg.setLayout(new GridLayout(1, false));
				Label l = new Label(dlg, SWT.NONE);
				l.setText("Find:");
				final Text t = new Text(dlg, SWT.NONE);
				t.setText("                                                                    ");
				Composite c = new Composite(dlg, SWT.NONE);
				c.setLayout(new GridLayout(2, false));
				
				Button ok = new Button(c, SWT.PUSH);
				ok.setText("Find");
				dlg.setDefaultButton(ok);
				ok.addSelectionListener(new SelectionListener(){

					String prevSearchString = "";
					int prevSearchLoc = -1;
					
					public void widgetDefaultSelected(SelectionEvent arg0) {
						// TODO Auto-generated method stub
						
					}

					public void widgetSelected(SelectionEvent arg0) {
						String searchString = t.getText().trim().toLowerCase();
						
						if (!searchString.equals(prevSearchString))
							prevSearchLoc = -1;
						
						prevSearchString = searchString;
						//dlg.close();
						boolean foundIt = false;
						for (int i = prevSearchLoc+1; i < previewTable.getItemCount(); i++)
						{
							TableItem itm = previewTable.getItem(i);
							for (int k = 0; k < previewTable.getColumnCount(); k++)
							{
								String xx = itm.getText(k).toLowerCase();
								if (xx.contains(searchString))
								{
									previewTable.select(i);
									previewTable.showSelection();
									prevSearchLoc = i;
									foundIt = true;
									break;
								}
							}
							if (foundIt)
								break;
						}
					}
					
				});
				
				Button cancel = new Button(c, SWT.PUSH);
				cancel.setText("Close");
				cancel.addSelectionListener(new SelectionListener(){

					public void widgetDefaultSelected(SelectionEvent arg0) {
						// TODO Auto-generated method stub
						
					}

					public void widgetSelected(SelectionEvent arg0) {
						dlg.close();
						
					}
					
				});
				
				dlg.pack();
				t.setText("");
				dlg.open();
			}
		
		});
		
		MenuItem helpItem = new MenuItem(bar, SWT.CASCADE);
		helpItem.setText("&Help");
		Menu helpMenu = new Menu(theShell, SWT.DROP_DOWN);
		helpItem.setMenu(helpMenu);
		MenuItem help = new MenuItem(helpMenu, SWT.PUSH);
		help.setText("Help");
		help.addListener(SWT.Selection, new Listener(){

			public void handleEvent(Event arg0) 
			{
				MessageBox b = new MessageBox(theShell, SWT.ICON_WARNING | SWT.OK);
				b.setMessage("Protein Aligner v 1.0\n" +
						"Step 1: Enter thresholds for protein and peptide scores.\n" +
						"Step 2: Select files for report.\n" +
						"        - use either Add files for Protein Pilot summary files (both Protein and Peptide reqd.)\n" +
						"          or Mascot results files.\n" +
						"        If using Pilot files, be sure to name the Protein and Peptide summary files identically.\n" +
						"Step 3: Select fasta file if doing glyosites (needed to get prot. sequences for context.)\n" +
						"Step 4: Select protein items in the Protein Items tab.\n" + 
						"Step 5: (Optional) Select peptide items to include in report.\n" + 
						"         - also can select to show each sequence/mods once.\n" + 
						"About 'Collapse groups with same winners:'\n" + 
						"     If unchecked, the master list will only collapse groups with exactly the same proteins\n" +
						"       so two protein groups from read from different search result files will only be considered\n" +
						"       the same if they have EXACTLY the same proteins.\n" +
						"     If checked, the program attempts to resolve differences in protein ids across\n" +
						"       different searches by looking to see if two groups have a common equivalent winner\n" +
						"       'Equivalent winners' are defined as top-scoring proteins in their group.\n" +
						"       So two groups coming from different result files will be considered the same if\n" +
						"       they have a common winner protein.  In that case, the protein group in the master list\n" +
						"       will only contain the proteins that were in common between both groups.\n" +
						"       Then, in using the master list to generate the report, it will report presence of \n" + 
						"       a protein group in one of the result files (column of the report) if the column contains\n" +
						"       it's own group with a winner protein in common with the winners of the master list group."
				); 
				b.setText("Warning");
				b.open();
			}
		});
	}

	private static class TheProgressIndicator extends ITRAQProgressIndicator
	{
		Display theDisplay = null;
		Shell progressDialog = null;
		ProgressBar bar = null;
		
		public TheProgressIndicator(Display display)
		{
			theDisplay = display;
			progressDialog = new Shell(theShell, SWT.DIALOG_TRIM);
			progressDialog.setLayout (new GridLayout (1, false));
			Label progressText = new Label (progressDialog, SWT.NONE);
			progressText.setText("Reading files");
			bar = new ProgressBar(progressDialog, SWT.SMOOTH);
			progressDialog.pack();
		}
		public void close()
		{
			progressDialog.close();
		}
		public void open()
		{
			progressDialog.open();
		}
		public void setIndValue(int n) 
		{
			bar.setSelection(n);
			while (!theDisplay.readAndDispatch ()) 
				theDisplay.sleep ();		}
	}
	static void fillAliasTable(String fileList[], Table aliasTable)
	{
		// first see if the table is empty and there's a file from which to fill it
		if (aliasTable.getItemCount() == 0 && fileList.length > 0)
		{
			String path = fileList[0].replaceAll((new File(fileList[0]).getName()), "");
			String keyFile = path + "LabelAliases.txt";
			File keys = new File(keyFile);
			if (keys.exists())
			{
				// open and read
				try {
					BufferedReader in = new BufferedReader(new FileReader(keys));
					String line;
					int lineNo = 0;
					while((line = in.readLine()) != null)
					{
						String spl[] = line.split("\t");
						if (spl.length != 3)
							continue;
						TableItem item = new TableItem(aliasTable, lineNo++);
						for (int i = 0; i < 3; i++)
							item.setText(i, spl[i]);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					setStatus(e.getLocalizedMessage());

				}
				return;
			}
		}
		//table not empty, so just check file list to see which labels we need aliases for

		for(String fn : fileList)
		{
			String shortName = (new File(fn)).getName();
			if (!shortName.contains("PeptideSummary"))
				continue;
			// see if we have it already
			boolean haveIt = false;
			for (int i = 0; i < aliasTable.getItemCount(); i++)
				if (aliasTable.getItem(i).getText(0).contains(shortName))
					haveIt = true;
			if (!haveIt)
			{
				try {
					String ratios[] = ITRAQReport.getFileRatios(new File(fn));
					TreeSet<String> labels = new TreeSet<String>();
					for (String ratio : ratios)
					{
						String ratioParts[] = ratio.split(":");
						labels.add(ratioParts[0]);
						labels.add(ratioParts[1]);
					}
					for (String label : labels)
					{
						//System.out.println("adding " + label);
						TableItem itm = new TableItem(aliasTable, SWT.NONE);
						itm.setText(new String[] {shortName, label, label});
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					setStatus(e.getLocalizedMessage());
				}
			}
		}
		
	}
	private static class TableSortComparator implements Comparator<String[]>
	{
		int sortColumn;
		int direction;
		boolean isNumeric;
		
		public TableSortComparator(int sortColumn, int direction, boolean isNumeric)
		{
			this.sortColumn = sortColumn;
			this.direction = direction;
			this.isNumeric = isNumeric;
		}
		public int compare(String[] a, String[] b) 
		{
			if (isNumeric)
			{
				
				double da = Double.MAX_VALUE;
				double db = Double.MAX_VALUE;
				try { da = Double.parseDouble(a[sortColumn]); } catch (NumberFormatException e) {}				
				try { db = Double.parseDouble(b[sortColumn]); } catch (NumberFormatException e) {}
				
				if (da > db)
					return direction==SWT.UP ? 1:-1;
				else if (db > da)
					return direction==SWT.UP ? -1:1;
				else
					return 0;
			}
			if (direction == SWT.UP)
				return a[sortColumn].compareTo(b[sortColumn]);
			else
				return b[sortColumn].compareTo(a[sortColumn]);
		}
		
	}
	static TableItem row;
	static void parseFileAndFillAliasTable(Table t, String file)
	{
		for (int i = 0; i < t.getItemCount(); i++)
		{
			TableItem itm = t.getItem(i);
			itm.setText(2, itm.getText(1));
		}
		BufferedReader in = null;
		try 
		{
			Exception badFileException = new Exception("File must be tab-delimited with three columns: FileKey, ReporterMass, Label");
			in = new BufferedReader(new FileReader(new File(file)));
			// read header
			String line = in.readLine();
			if (line == null)
				throw new Exception("Empty file");
			if (!line.toLowerCase().equals("filekey\treportermass\tlabel") 
					&& !line.toLowerCase().equals("file key\treporter mass\tlabel")
					&& !line.toLowerCase().equals("file\treporter\tlabel"))
				throw badFileException;
			int lineNo = 1;
			String[] fileList = getFileList();
			int countFilled = 0;
			while((line = in.readLine()) != null)
			{
				String spl[] = line.split("\t");
				if (spl.length != 3)
					throw new Exception("Line " + lineNo + " does not have 3 columns: " + line);
				// see if the key fits one of our input files;
				TreeSet<String> matchingFiles = new TreeSet<String>();
				for(String f : fileList)
					if (f.contains(spl[0]) && f.contains("ProteinSummary"))
						matchingFiles.add(f);
				//if (matchingFiles.isEmpty())
				//	throw new Exception("None of the input files matches " + spl[0]);
/* don't need to force single file matching.
 * 				if (matchingFiles.size() > 1)
				{
					String matchingFileList = "";
					for (String filem : matchingFiles)
					{
						if (matchingFileList.length() > 0)
							matchingFileList += ",";
						matchingFileList += filem;
					}
					throw new Exception(matchingFileList + " match " + spl[0] + ". \nFile keys need to be unambiguous.");
				}
*/
					// see if second column is 113-121
				boolean foundOne = false;
				for (int rep = 113; rep < 122 && ! foundOne; rep++)
				{
					if (rep == 120)
						continue;
					if (spl[1].equals("" + rep))
						foundOne = true;
				}
				if (!foundOne)
					throw new Exception(spl[1] + " is not a reporter mass on line " + lineNo);
				String label = spl[2];
				if (label.length() == 0)
					throw new Exception("No label for " + spl[1] + " on line " + lineNo + ": " + line);
				
				for (int i = 0; i < t.getItemCount(); i++)
				{
					TableItem itm = t.getItem(i);
					String ff = itm.getText(0);
					String rr = itm.getText(1);
					if (ff.contains(spl[0]) && rr.equals(spl[1]))
					{
						itm.setText(2, spl[2]);
						countFilled++;
					}
				}
			}
			if (countFilled < t.getItemCount())
			{
				MessageBox b = new MessageBox(t.getShell(), SWT.ICON_WARNING | SWT.OK);
				b.setMessage("Warning: Not all reporters were labeled.");
				b.setText("Warning");
				b.open();
			}
		} 
		catch (Exception e) 
		{
			MessageBox b = new MessageBox(t.getShell(), SWT.ICON_ERROR | SWT.OK);
			b.setMessage(e.getMessage());
			b.setText("Error reading reporter labels");
			b.open();
		}
		try {
			in.close();
		} catch (Exception e1) {
			setStatus(e1.getLocalizedMessage());

		}

	}
	static Table createAliasTable(TabFolder tabFolder, TabItem labelAliasesTab)
	{
		Composite composite = new Composite(tabFolder, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		Label label = new Label(composite, SWT.WRAP);
		label.setText("Columns in final report will appear labeled as defined here, and in the order defined here. Click column headers to change order.");
		Button loadFromFileButton = new Button(composite, SWT.PUSH);
		loadFromFileButton.setText("Load from file...");
		
		final Table aliasTable = new Table(composite, SWT.FULL_SELECTION | SWT.HIDE_SELECTION);
		aliasTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		aliasTable.setHeaderVisible(true);
		aliasTable.setLinesVisible(true);
		TableColumn fileColumn = new TableColumn(aliasTable, SWT.NONE);
		fileColumn.setText("File");
		fileColumn.setWidth(200);
		TableColumn labelColumn = new TableColumn(aliasTable, SWT.NONE);
		labelColumn.setText("Label");
		labelColumn.setWidth(100);
		TableColumn aliasColumn = new TableColumn(aliasTable, SWT.NONE);
		aliasColumn.setText("Name to use in report");
		aliasColumn.setWidth(200);

		loadFromFileButton.addSelectionListener(new SelectionAdapter()
			{
				public void widgetSelected(SelectionEvent e)
				{
					FileDialog dialog = new FileDialog (theShell, SWT.OPEN + SWT.SINGLE);
					String [] filterNames = new String [] {"Peptide Summary Files"};
					dialog.setFilterPath(curFilterPath);
					String [] filterExtensions = new String [] {"txt"};
					//dialog.setFilterNames (filterNames);
					//dialog.setFilterExtensions (filterExtensions);
					String ff = dialog.open();
					String path = dialog.getFilterPath();
					curFilterPath = path;
					saveCurFilterPath();
					parseFileAndFillAliasTable(aliasTable, ff);
					needReportUpdate = true;
					ITRAQReport.abortReportPrinting = true;
				}
			}
		);

		
		final TableEditor editor = new TableEditor(aliasTable);
	    editor.horizontalAlignment = SWT.LEFT;
	    editor.grabHorizontal = true;
	    editor.minimumWidth = 50;
	    final int EDITABLECOLUMN = 2;
	    aliasTable.addSelectionListener(new SelectionAdapter() {
	        public void widgetSelected(SelectionEvent e) {
	          // Clean up any previous editor control
	          Control oldEditor = editor.getEditor();
	          if (oldEditor != null)
	            oldEditor.dispose();

	          // Identify the selected row
	          TableItem item = (TableItem) e.item;
	          if (item == null)
	            return;
	          row = item;
	          
	          // The control that will be the editor must be a child of the
	          // Table
	          Text newEditor = new Text(aliasTable, SWT.NONE);
	          newEditor.setText(item.getText(EDITABLECOLUMN));
	          newEditor.addModifyListener(new ModifyListener() {
	        	  public void modifyText(ModifyEvent me) {
	        		  Text text = (Text) editor.getEditor();
	        		  editor.getItem().setText(EDITABLECOLUMN, text.getText());
	        		  needReportUpdate = true;
	        		  ITRAQReport.abortReportPrinting = true;
	        	  }
	          });
	          newEditor.addVerifyListener(new VerifyListener() {
	        	  public void verifyText(VerifyEvent me) {
	        		  Text text = (Text) editor.getEditor();
		              //System.out.println(text.getText());
	        	  }
	          });
	          newEditor.addKeyListener(new KeyAdapter(){
	        	  public void keyPressed(KeyEvent e)
	        	  {
	          		//System.out.println(e.character);
	          		if (e.character == SWT.CR) 
	          		{
	          			int curRow = 0;
	          			for (curRow = 0; curRow < aliasTable.getItemCount(); curRow++)
	          				if (aliasTable.getItem(curRow) == row)
	          					break;
	          			System.out.println(curRow);
	          			if (curRow == aliasTable.getItemCount()-1)
	          				return;
	          			//cursor.setSelection(table.getItem(curRow+1), EDITABLECOLUMN);
	          			///cursor.setVisible(true);
	          			// cursor.setFocus();
	          			Event newEvent = new Event();
	          			newEvent.item = aliasTable.getItem(curRow+1);
	          			aliasTable.notifyListeners(SWT.Selection, newEvent);
	          		}
	          		else
	          			super.keyPressed(e);
	          	}
	          });
	          newEditor.selectAll();
	          newEditor.setFocus();
	          editor.setEditor(newEditor, item, EDITABLECOLUMN);
	        }
	      });
	    Listener sortListener = new Listener(){
			public void handleEvent(Event e) {
				TableColumn oldSortColumn = aliasTable.getSortColumn();
				int oldDirection = aliasTable.getSortDirection();
				
				TableColumn column = (TableColumn) e.widget;
				int direction = SWT.UP;
				if (column == oldSortColumn && oldDirection == SWT.UP)
					direction = SWT.DOWN;
				
				int sortColumn = -1;
				for (int i = 0; i < aliasTable.getColumnCount(); i++)
					if (column == aliasTable.getColumn(i))
						sortColumn = i;
				TableItem[] items = aliasTable.getItems();
				String tableText[][] = new String[items.length][aliasTable.getColumnCount()];
				for (int i = 0; i < items.length; i++)
					for (int j = 0; j < aliasTable.getColumnCount(); j++)
						tableText[i][j] = aliasTable.getItem(i).getText(j);
				Arrays.sort(tableText, new TableSortComparator(sortColumn, direction, false)); // not numeric;
				for (int i = 0; i < tableText.length; i++)
					for (int j = 0; j < tableText[i].length; j++)
						items[i].setText(j, tableText[i][j]);
				aliasTable.setSortColumn(column);
				aliasTable.setSortDirection(direction);
				needReportUpdate = true;
      		  	ITRAQReport.abortReportPrinting = true;
			}
	    	
	    };
	    for (int i = 0; i < aliasTable.getColumnCount(); i++)
	    	aliasTable.getColumn(i).addListener(SWT.Selection, sortListener);
	    fillAliasTable(getFileList(), aliasTable);
	    labelAliasesTab.setControl(composite);
	    
		return aliasTable;
	}
	private static void fillTabFolder(TabFolder tabFolder)
	{
		final TabFolder theTabFolder = tabFolder;
		
		tabFolder.setSize(1000, 400);
		
		TabItem inputFilesTab = new TabItem (tabFolder, SWT.NONE);
		inputFilesTab.setText ("Input Files");
		fillInputFilesTab(tabFolder, inputFilesTab);
		
		TabItem proteinItemsTab = new TabItem (tabFolder, SWT.NONE);
		proteinItemsTab.setText ("Protein items to include in report");
		proteinItemsList = fillReportItemsTab(tabFolder, proteinItemsTab, ITRAQReport.proteinItems, "Protein items");
		
		TabItem peptideItemsTab = new TabItem (tabFolder, SWT.NONE);
		peptideItemsTab.setText ("Peptide items to include in report");
		peptideItemsList = fillReportItemsTab(tabFolder, peptideItemsTab, ITRAQReport.peptideItems, "Peptide items");
		
		TabItem labelAliasesTab = new TabItem(tabFolder, SWT.NONE);
		labelAliasesTab.setText("Names of quantitation labels");
		labelAliasTable = createAliasTable(tabFolder, labelAliasesTab);
		
		final TabItem previewTab = new TabItem(tabFolder, SWT.NONE);
		previewTab.setText("Preview of Report");
		
		createPreviewTable(tabFolder, previewTab);
		
		tabFolder.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e)
			{
				if (theTabFolder.getSelection()[0] == previewTab)
				{
					System.out.println("preview activated");
					boolean filesLoading = !FileLoader.allFilesLoaded();
					System.out.println("files " + (filesLoading?"":"not") + " loading.");
					boolean printing = ITRAQReport.isReportPrinting;
					theFindMenuItem.setEnabled(true);
					System.out.println("setting cursor");
					if (printing || filesLoading)
						theShell.setCursor(new Cursor(Display.getCurrent(), SWT.CURSOR_WAIT));
					else
						theShell.setCursor(new Cursor(Display.getCurrent(), SWT.CURSOR_ARROW));

					while(Display.getCurrent().readAndDispatch())
						;
				}
				else
				{
					System.out.println("preview deactivated");
					theFindMenuItem.setEnabled(false);
					theShell.setCursor(new Cursor(Display.getCurrent(), SWT.CURSOR_ARROW));
				}
			}
		});

	}
//	public static void main(String args[])
//	{
//		restoreFilterPath();
//		Display.setAppName("Pilot Results Aligner");
//		
//		Display display = new Display ();
//		
//		final Shell shell = new Shell (display);
//
//		FillLayout fillLayout = new FillLayout();
//		fillLayout.type = SWT.VERTICAL;
//		shell.setLayout(fillLayout);
//		
//		theShell = shell;
//		
//		setUpMenus();
//		
//		shell.setSize(1000,400);
//		
//		final TabFolder tabFolder = new TabFolder (shell, SWT.BORDER);
//		
//		fillTabFolder(tabFolder);
//		
//		final Label statusWindow = new Label(shell, SWT.BORDER);
//		statusWindow.setSize(100,100);
//		statusWindow.setText("status messages go here.");
//		GridData gd = new GridData();
//		
//		
//		//tabFolder.pack ();
//		shell.pack ();
//		shell.open ();
//
//		boolean wasBusy = false;
//		while (!shell.isDisposed ()) 
//		{
//			if (!display.readAndDispatch ()) 
//				display.sleep ();
//
//			updateReport();
//			
//			if (wasBusy && !ITRAQReport.isReportPrinting && FileLoader.allFilesLoaded())
//			{
//				theShell.setCursor(new Cursor(Display.getCurrent(), SWT.CURSOR_ARROW));
//				wasBusy = false;
//			}
//			else if (!ITRAQReport.isReportPrinting && FileLoader.allFilesLoaded())
//				wasBusy = true;
//
//		}
//		display.dispose ();
//
//	}
	static Label theStatusWindow;
	
	public static void setStatus(String text)
	{
		theStatusWindow.setText(text);
		statusMsg = text;
		Display.getDefault().readAndDispatch();
	}
	public static void main(String args[])
	{
		restoreFilterPath();
		Display.setAppName("Pilot Results Aligner");
		
		Display display = new Display ();
		
		final Shell shell = new Shell (display);

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;

		shell.setLayout(gridLayout);
		
		theShell = shell;
		
		setUpMenus();
		
		shell.setSize(1000,400);
		
		final TabFolder tabFolder = new TabFolder (shell, SWT.BORDER);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.verticalAlignment = GridData.FILL;
		tabFolder.setLayoutData(gridData);
		fillTabFolder(tabFolder);
		
		final Label statusWindow = new Label(shell, SWT.BORDER);
		statusWindow.setSize(100,400);
		theStatusWindow = statusWindow;
		statusWindow.setText("status messages go here.");
		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		statusWindow.setLayoutData(gridData);
		GridData gd = new GridData();
		
		
		//tabFolder.pack ();
		shell.pack ();
		shell.open ();

		boolean wasBusy = false;
		while (!shell.isDisposed ()) 
		{
			if (statusMsg != null)
				setStatus(statusMsg);
			
			if (!display.readAndDispatch ()) 
				display.sleep ();

			updateReport();
			
			if (wasBusy && !ITRAQReport.isReportPrinting && FileLoader.allFilesLoaded())
			{
				theShell.setCursor(new Cursor(Display.getCurrent(), SWT.CURSOR_ARROW));
				wasBusy = false;
			}
			else if (!ITRAQReport.isReportPrinting && FileLoader.allFilesLoaded())
				wasBusy = true;

		}
		display.dispose ();

	}

}
