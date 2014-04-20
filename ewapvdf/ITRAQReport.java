package edu.ucsf.library.sprot.ewapvdf;

import java.io.*;
import java.util.*;

import org.eclipse.swt.widgets.Display;

import jxl.Workbook;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;


import edu.ucsf.library.sprot.excel.XLOutputStream;
import edu.ucsf.library.util.FileUtil;


public class ITRAQReport 
{
	public static boolean abortFileLoad = false;
	public static boolean abortReportPrinting = false;
	public  boolean collapseWinners = false;
	public  boolean summarizePeptides = false;
	public static boolean isReportPrinting = false;
	public String fastaFile = "";
	public static HashMap<String,String> proteinSequenceCache = null;
	
	public static void setStatus(String text)
	{
		ITRAQReportUI.statusMsg = text;
	}
	
	static HashMap<String, ProteinGroupList> groupListCache = new HashMap<String, ProteinGroupList>();

	public ProteinGroupMasterList masterList;
	ReportColumnHandle columnHandles[];
	
	String leadInColumnsProteins[]; // printed prior to the data columns, things like accession, name etc.
	String leadInColumnsPeptides[]; // printed prior to the data columns, things like sequence mods etc.
	
	public class ReportColumnHandle
	{
		String sortString;
		String headers[];
		ProteinGroupList pgl;
		String proteinFieldName;
		int proteinColumnNo = -1;
		String peptideFieldName;
		int peptideColumnNo = -1;
		
		public ReportColumnHandle(String sortString, String[] headers,
				ProteinGroupList pgl, String proteinFieldName,
				String peptideFieldName) {
			super();
			this.sortString = sortString;
			this.headers = headers;
			this.pgl = pgl;
			this.proteinFieldName = proteinFieldName;
			this.peptideFieldName = peptideFieldName;
			
			Protein prot = pgl.groups.get("1").proteins.get(0);
			//Peptide pep  = pgl.groups.get("1").peptides.get(0);
			
			proteinColumnNo = prot.getColumnNumber(proteinFieldName);
			//peptideColumnNo = pep.getColumnNumber(peptideFieldName);
		}
		
	}
	
	static String[] getFileRatios(File peptideSummaryFile) throws Exception
	{
		BufferedReader in = new BufferedReader(new FileReader(peptideSummaryFile));
		String headerLine = in.readLine();
		in.close();
		String spl[] = headerLine.split("\t");
		ArrayList<String> ratios = new ArrayList<String>();
		for (int i = 0; i < spl.length; i++)
			if (spl[i].matches("((11[2-9])|121):((11[2-9])|121)"))
					ratios.add(spl[i]);
			
		return ratios.toArray(new String[0]);		
	}
	
	public static String proteinItems[] = {"SP Accession", "Entry Name", "Accession","Protein Name","Species","N","Max Unused", "Unused","Total","%Cov","%Cov(50)","%Cov(95)","Peptides(95%)","Quant Peptides","Ratios","PVal","EF","LowerCI","UpperCI","Biological Processes","Molecular Function","PANTHER ID"};
	public static String peptideItems[] = {"Sequence","Modifications","Glycosites (fasta reqd.)","Cleavages","Accessions","Names","Used","Annotation","Contrib","Conf","N","Unused","Total","%Cov","%Cov(50)","%Cov(95)","dMass","Prec MW","Prec m/z","Theor MW","Theor m/z","Theor z","Sc","Spectrum","Time","Ratios","%Err","Areas","Area Errs", "Background"};

	private static boolean isLeadIn(String x)
	{
		final String leadInColumnNames[] = {"SP Accession", "Entry Name", "Accession", "Protein Name", "Sequence", "Modifications", "Accessions", "Names", "Max Unused", "Glycosites (fasta reqd.)"};
		for (String licn : leadInColumnNames)
			if (licn.equals(x))
				return true;
		return false;
	}
	static boolean isMultiValueRatios(String x)
	{
		final String mvColumnNames[] = {"Ratios","%Err","EF","LowerCI","UpperCI"};
		
		for (String item : mvColumnNames)
			if (item.equals(x))
				return true;
		return false;
	}
	public String translateMultiValueItemWithRatio(String item, String thisRatio)
	{
		if (item.equals("Ratios"))
			return thisRatio;
		if (isMultiValueRatios(item))
			return item + " " + thisRatio;
		return "";
	}
	public String[] getMultiValueRatioItems(String[] x)
	{
		ArrayList<String> mvItems = new ArrayList<String>();
		for (String item : x)
			if (isMultiValueRatios(item))
				mvItems.add(item);
		return mvItems.toArray(new String[0]);
	}
	static boolean isMultiValueAreas(String x)
	{
		final String mvColumnNames[] = {"Area","Area Errs"};
		
		for (String item : mvColumnNames)
			if (item.equals(x))
				return true;
		return false;
	}
	public String[] getMultiValueAreaItems(String[] x)
	{
		ArrayList<String> mvItems = new ArrayList<String>();
		for (String item : x)
			if (isMultiValueAreas(item))
				mvItems.add(item);
		return mvItems.toArray(new String[0]);
	}
	public static boolean isSVItem(String x)
	{
		final String svColumnNames[] = {"N","Unused","Total","%Cov","%Cov(50)","%Cov(95)","Used","Annotation",
										"Contrib","Conf","dMass","Prec MW","Prec m/z","Theor MW",
										"Theor m/z","Theor z","Sc","Spectrum","Time","PrecursorSignal",
										"PrecursorElution","Background",
										
										"Peptides(95%)", "Quant Peptides"};
		
		for (String item : svColumnNames)
			if (item.equals(x))
				return true;
		return false;
	}
	static String[] getSingleValueItems(String[] items)
	{
		ArrayList<String> svis = new ArrayList<String>();
		
		for (String item : items)
			if (isSVItem(item))
				svis.add(item);
		return svis.toArray(new String[0]);
	}
	static String translateNameToSortString(String itemWratio, String file, String aliasList[])
	{
		String sortString = itemWratio;
		for (int i = 0; i < aliasList.length; i++)
		{
			String alias = aliasList[i];
			String spl[] = alias.split("\t");
			if (file.contains(spl[0]) && itemWratio.contains(spl[1]))
			{
				sortString = "" + (1000 + i);
				break;
			}
		}
		return sortString;
	}

	static String translateName(String itemWratio, String file, String aliasList[])
	{
		String transName = itemWratio;
		for (String alias : aliasList)
		{
			String spl[] = alias.split("\t");
			if (file.contains(spl[0]) && itemWratio.contains(spl[1]))
			{
				if (spl.length > 2)
					transName = transName.replaceAll(spl[1], spl[2]);
				else
				{
					System.out.println("Error translating name for alias = " + alias);
					ITRAQReportUI.setStatus("Error translating name for alias = " + alias);
				}
			}
		}
		return transName;
	}
	public static void readNeededFiles(String fileList[], boolean usedFlag, double minProtScore, double minPepScore, boolean collaspeWinners) throws Exception
	{		
		for (String file : fileList)
		{
			if (abortFileLoad)
				break;
			File f = new File(file);
			String fName = f.getName();
			if (fName.contains("PeptideSummary") && fName.endsWith(".txt") && !fName.startsWith(".") || fName.endsWith(".dat"))
			{
				String pepSum = file;
				String protSum = file.replaceAll("PeptideSummary", "ProteinSummary");
				if (groupListCache.get(pepSum + "::" + protSum) == null)
				{
					System.out.println("loading " + pepSum);
					setStatus("Loading " + pepSum);
					groupListCache.put(pepSum + "::" + protSum, new ProteinGroupList(protSum, pepSum, usedFlag, minProtScore, minPepScore));
					System.out.println(pepSum + " loaded.");
					setStatus(pepSum + " loaded.");
				}
			}
		}

	}
	public static ProteinGroupMasterList prevMasterList = null;
	public ITRAQReport(String fileList[], String proteinItems[], String peptideItems[], String aliasList[], boolean collapseWinners, boolean summarizePeptides, String fastaFile) throws Exception
	{
		abortReportPrinting = false;
		isReportPrinting = false;
		
		this.collapseWinners = collapseWinners;
		this.summarizePeptides = summarizePeptides;
		this.fastaFile = fastaFile;
		
		// get all the group lists
		TreeSet<String> reportFiles = new TreeSet<String>();

		int numCachedLists = groupListCache.size();
		int numListsFound = 0;
		
		for (String file : fileList)
		{
			File f = new File(file);
			String fName = f.getName();
			if (fName.contains("PeptideSummary") && fName.endsWith(".txt") && !fName.startsWith(".") || fName.endsWith(".dat"))
			{
				String pepSum = file;
				String protSum = file.replaceAll("PeptideSummary", "ProteinSummary");
				if (groupListCache.get(pepSum + "::" + protSum) == null)
				{
					System.out.println((new File(protSum)).getName() + "   " + (new File(pepSum)).getName());
					groupListCache.put(pepSum + "::" + protSum, new ProteinGroupList(protSum, pepSum));
				}
				else
					numListsFound++;
				reportFiles.add(pepSum + "::" + protSum);
			}
		}
		
		if (numCachedLists == numListsFound && numCachedLists != 0 && prevMasterList != null)
			masterList = prevMasterList;
		else
		{
			masterList = new ProteinGroupMasterList();
			masterList.collapseWinners = collapseWinners;
			for (String f : reportFiles)
			{
				ProteinGroupList gl = groupListCache.get(f);
				for (String N : gl.groups.keySet())
					masterList.add(gl.groups.get(N));
			}
			// remove all obsolete grouplists from the cache
			TreeSet<String> toDelete = new TreeSet<String>();
			for (String f : groupListCache.keySet())
				if (!reportFiles.contains(f))
					toDelete.add(f);
			for (String f : toDelete)
				groupListCache.remove(f);
		}
		prevMasterList = masterList;
		refreshSequenceCache();
				
		// set lead in items
		int countLeadInProteinColumns = 0,
		    countLeadInPeptideColumns = 0;
		
		for (String item : proteinItems)
			if (isLeadIn(item))
				countLeadInProteinColumns++;
		
		for (String item : peptideItems)
			if (isLeadIn(item))
				countLeadInPeptideColumns++;
		
		int countLeadInColumns = Math.max(countLeadInProteinColumns, countLeadInPeptideColumns+1);
		
		this.leadInColumnsProteins = new String[countLeadInColumns];
		int i = 0;
		for (String item : proteinItems)
			if (isLeadIn(item))
				this.leadInColumnsProteins[i++] = item;
		while (i < countLeadInColumns)
			this.leadInColumnsProteins[i++] = "";
		
		this.leadInColumnsPeptides = new String[countLeadInColumns];
		i = 0;
		this.leadInColumnsPeptides[i++] = "";
		for (String item : peptideItems)
			if (isLeadIn(item))
				this.leadInColumnsPeptides[i++] = item;
		while (i < countLeadInColumns)
			this.leadInColumnsPeptides[i++] = "";
		
		// next create the column handles for any single-value file-specific items
		String singleValProteinItems[] = getSingleValueItems(proteinItems);
		String singleValPeptideItems[] = getSingleValueItems(peptideItems);
		
		ArrayList<ReportColumnHandle> headers = new ArrayList<ReportColumnHandle>();
		for (i = 0; i < Math.max(singleValProteinItems.length, singleValPeptideItems.length); i++)
		{
			String protItem = "";
			if (i < singleValProteinItems.length)
				protItem = singleValProteinItems[i];
			String pepItem = "";
			if (i < singleValPeptideItems.length)
				pepItem = singleValPeptideItems[i];
			
			int j = 0;
			for (String f : reportFiles)
			{
				String fileName = (new File(f.split("::")[0])).getName();
				if (fileName.endsWith(".dat"))
				{
					String origFileName = fileName;
					fileName = groupListCache.get(f).fileName;
					fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
					fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
					fileName += "(" + origFileName + ")";
				}
				//fileName = fileName.substring(fileName.lastIndexOf("/")+1);
				fileName = fileName.replaceAll("PeptideSummary", "");
				fileName = fileName.replaceAll("\\.txt", "");
				String colhead = protItem + "(" + pepItem + ")";
				colhead = colhead.replaceAll("\\(\\)","" );
				String sortString = "" + (i*1000 + j);
				ReportColumnHandle h = new ReportColumnHandle(sortString, new String[] {fileName, colhead}, groupListCache.get(f), protItem, pepItem);
				headers.add(h);
			}
		}
		
		// create column handles for any multi-value items
		
		String mvProteinItems[] = getMultiValueRatioItems(proteinItems);
		String mvPeptideItems[] = getMultiValueRatioItems(peptideItems);
		for (i = 0; i < Math.max(mvProteinItems.length, mvPeptideItems.length); i++)
		{
			String protItem = "";
			if (i < mvProteinItems.length)
				protItem = mvProteinItems[i];
			String pepItem = "";
			if (i < mvPeptideItems.length)
				pepItem = mvPeptideItems[i];
			
			for (String f : reportFiles)
			{
				// find the file
				String f1 = f.split("::")[0];
				String fullName = "";
				for (String ff : fileList)
				{
					if (ff.endsWith(f1))
					{
						fullName = ff;
						break;
					}
				}
				
				String ratios[] = getFileRatios(new File(fullName));
				for (String ratio : ratios)
				{
					String protItemWRatio = "";
					if (protItem.equals("Ratios"))
						protItemWRatio = ratio;
					else
						protItemWRatio = protItem + " " + ratio;
					String pepItemWRatio = "";
					if (pepItem.equals("Ratios"))
						pepItemWRatio = ratio;
					else
						pepItemWRatio = pepItem + " " + ratio;
					
					String hName0 = new File(f.split("::")[0]).getName();
					hName0 = hName0.replaceAll("PeptideSummary", "").replaceAll("\\.txt", "").trim().replaceAll("  ", " ").replaceAll("__", "_");
					String hName1 = protItemWRatio + "(" + pepItemWRatio + ")";
					String transName = translateName(hName1, f, aliasList);
					String sortString = translateNameToSortString(hName1, f, aliasList);
					ReportColumnHandle h = new ReportColumnHandle(sortString, new String[] {hName0, hName1, transName}, groupListCache.get(f), protItemWRatio, pepItemWRatio);
					headers.add(h);
				}
			}
		}
		
		this.columnHandles = headers.toArray(new ReportColumnHandle[0]);
		
		this.sortColumns();
	}
	
	// constructor to build the column handles from file lists
	//
	// pathToSummaries is the name of the directory containing all the ProteinSummary and PeptideSummary files
	//
	// reporterLabelFile is a Tab-delimited file of the format
	//    fileKey <tab> reporter <tab> label
	//       fileKey is a text string that when matched means the reporters and labels should be used for a given file
	//       reporter is 113, 114, 115, ... 121
	//       label is a text string indicating what each reporter was
	//       Example:
	//        test1  113   pool
	//        test1  114   donor1
	//        test1  115   donor2
	//       ...
	//    any file containing "test1" in the file name would then use pool, donor1, donor2, etc as the names of the various reporters.
	//    So the ratio 114/113 would be called "donor1/pool" and any data in the report for that ratio would be labeled as such
	// 
	// proteinDataToInclude is an array of names of fields associated with ratios to print in the report
	// peptideDataToInclude is a similar array of names of fields associated with ratios.
	//    Each string in one of these arrays is appended after the ratio string to get a column header from the corresponding summary file.
	//    For example to include the error factor in the report, add the string "EF" to the protein array.  That way, the report would include 
	//    "nnn/ddd EF" for each ratio nnn/ddd.  So if 113 is the denominator, "114/113 EF" would be shown in the report.
	//    You must include "" in these arrays to show the ratios themselves.
	
	
	public ITRAQReport(String pathToSummaries, String reporterLabelFile,
					   String[] proteinDataToInclude,
					   String[] peptideDataToInclude, 
					   String[] proteinQuantDataToInclude,
					   String[] peptideQuantDataToInclude) throws Exception
	{
		ITRAQKeyFile keys = new ITRAQKeyFile(reporterLabelFile);
		
		File[] peptideSummaryFiles = FileUtil.fileSort(FileUtil.getFileList(new File(pathToSummaries), "PeptideSummary.txt", false));
		File[] proteinSummaryFiles = FileUtil.fileSort(FileUtil.getFileList(new File(pathToSummaries), "ProteinSummary.txt", false));

		ProteinGroupList groups[] = new ProteinGroupList[peptideSummaryFiles.length];
		
		
		masterList = new ProteinGroupMasterList();
		
		for (int i = 0; i < peptideSummaryFiles.length; i++)
		{
			if (peptideSummaryFiles[i].getName().startsWith("."))
				continue;
			System.out.println(peptideSummaryFiles[i].getName());
			groups[i] = new ProteinGroupList(proteinSummaryFiles[i].getAbsolutePath(), peptideSummaryFiles[i].getAbsolutePath());
			for (String N : groups[i].groups.keySet())
			{
				ProteinGroup pg = groups[i].groups.get(N);
				MasterListGroup mg = new MasterListGroup(pg);

				masterList.add(mg);
			}
		}
		
		ArrayList<ReportColumnHandle> columnHandles = new ArrayList<ReportColumnHandle>();
		for (int i = 0; i < peptideSummaryFiles.length; i++)
		{
			if (peptideSummaryFiles[i].getName().startsWith("."))
				continue;
			// add in columns for Proteins
			for (int j = 0; j < Math.max(proteinDataToInclude.length, peptideDataToInclude.length); j++)
			{
				String headers[] = new String[4];
				headers[0] = peptideSummaryFiles[i].getName().replaceAll("PeptideSummary\\.txt", "");
				headers[1] = proteinDataToInclude[j] + "(" + peptideDataToInclude[j]+")";
				if (headers[1].equals("()"))
					headers[1] = "";
				headers[2] = headers[3] = "";
				String protField = proteinDataToInclude[j];
				String pepField = peptideDataToInclude[j];
				ReportColumnHandle ch = new ReportColumnHandle("", headers, groups[i], protField, pepField);
				columnHandles.add(ch);
			}
			String fileRatios[] = getFileRatios(peptideSummaryFiles[i]);
			for (int j = 0; j < fileRatios.length; j++)
			{
				for (int k = 0; k < Math.max(proteinQuantDataToInclude.length, peptideQuantDataToInclude.length); k++)
				{
					String headers[] = new String[4];
					headers[0] = peptideSummaryFiles[i].getName().replaceAll("PeptideSummary\\.txt", "");
					headers[1] = fileRatios[j];
					String labelRatio = keys.getLabelRatioFromReporterRatio(peptideSummaryFiles[i].getName(), fileRatios[j]);
					headers[2] = labelRatio;
					headers[3] = proteinQuantDataToInclude[k] + "(" + peptideQuantDataToInclude[k]+")";
					if (headers[3].equals("()"))
						headers[3] = "";

					String protField = (proteinQuantDataToInclude[k] + " " + fileRatios[j]).trim();
					if (proteinQuantDataToInclude[k].isEmpty())
						protField = "";
					if (proteinQuantDataToInclude[k].toLowerCase().equals("ratio"))
						protField = fileRatios[j];
					
					String pepField = (peptideQuantDataToInclude[k] + " " + fileRatios[j]).trim();
					if (peptideQuantDataToInclude[k].isEmpty())
						pepField = "";
					if (peptideQuantDataToInclude[k].toLowerCase().equals("ratio"))
						pepField = fileRatios[j];
					
					ReportColumnHandle ch = new ReportColumnHandle("" + i + ('a' + k), headers, groups[i], protField, pepField);
					columnHandles.add(ch);
				}
			}
		}
		this.columnHandles = columnHandles.toArray(new ReportColumnHandle[0]);
		leadInColumnsProteins = new String[] {"Accession", "Name", "%Cov(95)", "Peptides(95%)", " "};
		leadInColumnsPeptides = new String[] {"", "Sequence", "Modifications", " ", " "};
	}
	
	
	public void printProteinLine(MasterListGroup mg, TreeSet<String> peptidesForQuant, PrintStream out) throws Exception
	{
		// print the lead-in data
		for (int i = 0; i < leadInColumnsProteins.length; i++)
		{
			if (i > 0)
				out.print("\t");
			String field = leadInColumnsProteins[i];
			String fieldStr = mg.getString(field);
			String pseudoField = field;
			
			if (pseudoField.equals("Max Unused"))
			{
				double maxUnused = 0;
			
				for (ReportColumnHandle h : columnHandles)
				{
					ProteinGroup pg = h.pgl.find(mg, collapseWinners);
					if (pg != null)
						maxUnused = Math.max(maxUnused, pg.proteins.get(0).getDouble("Unused"));
				}
				fieldStr = "" + maxUnused;
			}
			out.print(fieldStr);
		}
		// print the column data
		ProteinGroup pg = null;
		ProteinGroupList prevList = null;
		
		for (int i = 0; i < columnHandles.length; i++)
		{
			out.print("\t");
			ReportColumnHandle h = columnHandles[i];
			if (prevList != h.pgl)
			{
				pg = h.pgl.find(mg, collapseWinners);
				prevList = h.pgl;
			}
			if (pg != null)
			{
				if (h.proteinFieldName.equals("Quant Peptides"))
				{
					int count = 0;
					for (Peptide pep : pg.peptides)
					{
						if (pep.getString("Used").equals("1") || (peptidesForQuant!= null && peptidesForQuant.contains(pep.getString("Sequence") + ":" + pep.getString("Mods"))))
						{
							count++;
						}
					}
					out.print(count);
					continue;
				}
				if (peptidesForQuant == null)
				{
					Protein prot = pg.proteins.get(0);//pg.findProteinWithQuants();
					if (prot != null)
					{
						if (ProteinGroup.isRatio(h.proteinFieldName))
						{
							try {
								double ratio = prot.getDouble(h.proteinFieldName);
								if (ratio == 0 || ratio == 100)
								{
									out.print("");
									continue;
								}
							} catch (Exception e) {
								setStatus(e.getLocalizedMessage());
							}
						}
						if (h.proteinColumnNo >= 0)
							out.print(prot.getStringRound(h.proteinColumnNo));
						else
							out.print(prot.getStringRound(h.proteinFieldName));
					}
				}
				else
				{
					out.print(pg.compute(h.proteinFieldName, peptidesForQuant));
				}
			}
		}
		out.println();
	}
	public void printPeptideLines(MasterListGroup mg, String pepStr, PrintStream out, boolean printOnlyPeptidesWithQuant)
	{
		double maxNumQuants = -1;
		HashMap<ProteinGroupList, ArrayList<Peptide>> allInstances = new HashMap<ProteinGroupList, ArrayList<Peptide>>();
		
		// go thru in first pass to collect all instances of this peptide from all the columns
		Peptide examplePeptide = null;
		for (int col = 0; col < columnHandles.length; col++)
		{
			Display.getDefault().readAndDispatch();
			if (abortReportPrinting)
				return;
			ProteinGroupList pgl = columnHandles[col].pgl;
			
			if (allInstances.keySet().contains(pgl))
				continue;
			
			ArrayList<Peptide> allInstancesThisGroup = new ArrayList<Peptide>();
			allInstances.put(pgl, allInstancesThisGroup);
			int countQuants = 0;
			ProteinGroup pg = pgl.find(mg, collapseWinners);
			if (pg != null)
				for (Peptide pep : pg.peptides)
				{
					if (abortReportPrinting)
						return;
					if ((pep.getString("Sequence") + ":" + pep.getString("Modifications")).equals(pepStr))
					{ 
						if (printOnlyPeptidesWithQuant && !pep.hasQuants())
							continue;
						countQuants++;
						allInstancesThisGroup.add(pep);
						examplePeptide = pep;
					}
				}
			maxNumQuants = Math.max(maxNumQuants, countQuants);
		}
		if (summarizePeptides)
			maxNumQuants = 1;
		for (int nq = 0; nq < maxNumQuants; nq++)
		{
			Display.getCurrent().readAndDispatch();
			if (abortReportPrinting)
				return;
			// print the lead in data
			for (int col = 0; col < leadInColumnsPeptides.length; col++)
			{
				if (abortReportPrinting)
					return;
				if (nq == 0)
				{
					if (col > 0)
						out.print("\t");
					if (examplePeptide != null)
						out.print(examplePeptide.getString(leadInColumnsPeptides[col]));
//					out.print(":" + countTimesUsed(mg, examplePeptide.getString("Sequence") + ":" + examplePeptide.getString("Modifications")));
				}
				else if (col > 0)
					out.print("\t");
			}
			
			for (int col = 0; col < columnHandles.length; col++)
			{
				if (abortReportPrinting)
					return;
				ArrayList<Peptide> allInstancesThisGroup = allInstances.get(columnHandles[col].pgl);
				out.print("\t");
				if (allInstancesThisGroup.size() > nq)
				{
					Peptide pep = allInstancesThisGroup.get(nq);
					out.print(pep.getStringRound(columnHandles[col].peptideFieldName));
				}
			}
			out.println();
		}
	}
	public int getNumHeaderLines()
	{
		int numHeaderLines = 0;
		for (ReportColumnHandle h : columnHandles)
			numHeaderLines = Math.max(numHeaderLines, h.headers.length);
		return numHeaderLines;
	}
	public void printColumnHeaders(PrintStream out, boolean includePeptideHeaders)
	{
		int numHeaderLines = 0;
		for (ReportColumnHandle h : columnHandles)
			numHeaderLines = Math.max(numHeaderLines, h.headers.length);
		for (int row = 0; row < numHeaderLines; row++)
		{
			for (int col = 0; col < leadInColumnsProteins.length; col++)
			{
				if (abortReportPrinting)
					return;
				if (col > 0)
					out.print("\t");
				if (row == 0)
					out.print(leadInColumnsProteins[col]);
				if (row == 1 && includePeptideHeaders)
					out.print(leadInColumnsPeptides[col]);
			}
			for (int col = 0; col < columnHandles.length; col++)
			{
				//if (col > 0)
				out.print("\t");
				ReportColumnHandle h = columnHandles[col];
				if (h.headers.length > row)
					out.print(includePeptideHeaders ? h.headers[row] : h.headers[row].replaceAll("\\(.*\\)", ""));
			}
			out.println();
		}

	}
	public boolean isQuantDataIncluded()
	{
		for (ReportColumnHandle h : this.columnHandles)
			if (isMultiValueRatios(h.proteinFieldName) || isMultiValueRatios(h.peptideFieldName))
				return true;
		return false;
	}
	public void printProteinReport(TreeSet<String> peptidesForQuant, PrintStream out, boolean showPeptides) throws Exception
	{
		isReportPrinting = true;
		
		printColumnHeaders(out, showPeptides);
		boolean showOnlyPeptidesWithQuants = isQuantDataIncluded();
		for (MasterListGroup g : masterList.groups)
		{
			Display.getCurrent().readAndDispatch();
			
			if (abortReportPrinting)
				break;
			
			if (peptidesForQuant != null)
			{
				TreeSet<String> gPeptides = (TreeSet<String>) g.peptides.clone();
				if (gPeptides.retainAll(peptidesForQuant) && gPeptides.isEmpty())
					continue;
			}

			printProteinLine(g, peptidesForQuant, out);
			
			if (showPeptides)
				for (String peptide : g.peptides)
					if (peptidesForQuant == null || peptidesForQuant.contains(peptide))
						printPeptideLines(g, peptide, out, showOnlyPeptidesWithQuants);
		}
		printSummary(out);
		isReportPrinting = false;
	}

	public static class SummaryStats
	{
		static String names[] = {"#Proteins", "#Peptides", "#Glycopeptides"};
		
		public int stats[] = new int[names.length];
		
		public void setStat(String name, int val)
		{
			for (int i = 0; i < names.length; i++)
				if (name.equals(names[i]))
				{
					stats[i] = val;
					return;
				}
		}
		public int getStat(String name)
		{
			for (int i = 0; i < names.length; i++)
				if (name.equals(names[i]))
					return stats[i];
			return -1;
		}
	}
	SummaryStats getSummaryStats(ProteinGroupList pgl)
	{
		SummaryStats ret = new SummaryStats();
		
		int numProts = pgl.groups.size();
		int numPeps = 0;
		int numGlycoPeps = 0;
		
		for (String N : pgl.groups.keySet())
		{
			ProteinGroup pg = pgl.groups.get(N);
			TreeSet<String> distinctPeps = new TreeSet<String>();
			TreeSet<String> distinctGlycoPeps = new TreeSet<String>();
			for (Peptide pep : pg.peptides)
			{
				String pepMods = pep.getString("Sequence") + ":" + pep.getString("Modifications");
				distinctPeps.add(pepMods);
				if (!pep.getGlycosites().isEmpty())
					distinctGlycoPeps.add(pepMods);
			}
			numPeps += distinctPeps.size();
			numGlycoPeps += distinctGlycoPeps.size();
		}
		ret.setStat("#Proteins", numProts);
		ret.setStat("#Peptides", numPeps);
		ret.setStat("#Glycopeptides", numGlycoPeps);
		return ret;
	}
	void printSummary(PrintStream out)
	{
		SummaryStats stats[] = new SummaryStats[columnHandles.length];
		for (int col = 0; col < columnHandles.length; col++)
		{
			ProteinGroupList pgl = columnHandles[col].pgl;
			stats[col] = getSummaryStats(pgl);
		}
		for (int iStat = 0; iStat < SummaryStats.names.length; iStat++)
		{
			out.print(SummaryStats.names[iStat]);
			for (int col = 0; col < leadInColumnsPeptides.length; col++)
				out.print("\t");
			for (int col = 0; col < columnHandles.length; col++)
				out.print(stats[col].stats[iStat] + "\t");
			out.println();
		}
	}
	static class NumericStringComparator implements Comparator<String>
	{
		static int numPart(String x)
		{
			String numPt = "";
			for (int i = 0; i < x.length(); i++)
			{
				char c = x.charAt(i);
				if (c >= '0' && c <= '9')
					numPt += c;
				else
					break;
			}
			if (numPt.isEmpty())
				return Integer.MAX_VALUE;
			return Integer.parseInt(numPt);
		}
		public int compare(String a, String b)
		{
			int diff = numPart(a) - numPart(b);
			if (diff != 0)
				return diff;
			
			return a.compareTo(b);
		}
	}

	public void sortColumns()
	{
		Arrays.sort(columnHandles, new Comparator<ReportColumnHandle>() {
			public int compare(ReportColumnHandle a, ReportColumnHandle b)
			{
				return (new NumericStringComparator()).compare(a.sortString, b.sortString);
			}
		});
	}
	
	public void sortColumnsOnHeaders(int[] hNs)
	{
		for (int i = 0; i < this.columnHandles.length; i++)
		{
			ReportColumnHandle ch = columnHandles[i];
			String sortString = "";
			for (int j = 0; j < hNs.length; j++)
				sortString += ch.headers[hNs[j]].replaceAll("Ratio", " Ratio") + " ";
			ch.sortString = sortString;
		}
		sortColumns();
	}
	
	private int countTimesUsed(MasterListGroup mg, String pepStr)
	{
		HashSet<ProteinGroupList> pglsSeen = new HashSet<ProteinGroupList>();
		int countTimesUsed = 0;
		
		for (int col = 0; col < columnHandles.length; col++)
		{
			ProteinGroupList pgl = columnHandles[col].pgl;
			if (pglsSeen.contains(pgl))
				continue;
			pglsSeen.add(pgl);
			
			int countQuants = 0;
			ProteinGroup pg = pgl.find(mg, collapseWinners);
			if (pg != null)
				for (Peptide pep : pg.peptides)
					if ((pep.getString("Sequence") + ":" + pep.getString("Modifications")).equals(pepStr) && pep.hasQuants())
						countQuants++;
			
			if (countQuants > 0)
				countTimesUsed++;
		}
		return countTimesUsed;
	}
	public TreeSet<String> getPeptidesUsedAtLeast(int nTimes)
	{
		TreeSet<String> ret = new TreeSet<String>();

		for (MasterListGroup g : masterList.groups)
		{
			for (String pepStr : g.peptides)
				if (countTimesUsed(g, pepStr) >= nTimes)
					ret.add(pepStr);
		}
		return ret;
	}
	public static void mainX(String args[]) throws Exception
	{
		int nTimes = 3; // min times used requirement for peptides
		String path = "/Volumes/work/lab/Ewa/PVDF paper/Timecourse_ewa to rich/Time course overnight/";
		//path = "/Volumes/work/lab/Ewa/PVDF paper/Nov Acc/Protein_peptide summaries Pilot/";
		//path = "/Volumes/work/lab/Ewa/PVDF paper/Nov Acc/Protein_peptide summaries Pilot/";
		//path = "/Volumes/work/lab/Ewa/PVDF paper/Nov Rec/Nov Rec_Protein_peptide summaries Pilot/";
		//path = "/Volumes/work/lab/Katy/20110708 Orbi iTRAQ CE opt/Summary Files/";
		path = "/Volumes/work/lab/Ewa/PVDF paper/Nov Rec/Pilot files/EditPilot/";
		String keyFile = path + "KeyFile for iTRAQ.txt";
		ITRAQReport r = new ITRAQReport(path, keyFile, new String[] {  }, new String[] {}, new String[] {"Ratio"}, new String[] {"Ratio"});
		TreeSet<String> peptidesForQuant = r.getPeptidesUsedAtLeast(nTimes);
		r.sortColumnsOnHeaders(new int[] {2, 0,3 });
		
		String outFile = path + "CollatedReport using peptides in " + nTimes + " octets.xls";
		//outFile = path + "CollatedReport using all peptides.xls";peptidesForQuant = null;
		
		//PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(outFile)));
		
		WritableWorkbook bk = Workbook.createWorkbook(new File(outFile));
		WritableSheet sP = bk.createSheet("Proteins", 0);
		WritableSheet sPP = bk.createSheet("Proteins and Peptides", 1);
		PrintStream outP = new PrintStream(new BufferedOutputStream(new PrintStream(new XLOutputStream(sP))));
		PrintStream outPP = new PrintStream(new BufferedOutputStream(new PrintStream(new XLOutputStream(sPP))));

		r.printProteinReport(peptidesForQuant, outP, false);
		r.printProteinReport(peptidesForQuant, outPP, true);
		
		outP.close();
		outPP.close();
		bk.write();
		bk.close();

	}
	
	private TreeSet<String> getAllAccessions()
	{
		TreeSet<String> accSet = new TreeSet<String>();
		for (MasterListGroup pg : this.masterList.groups)
		{
			for (Protein p : pg.proteins)
			{
				String acc = p.getString("Accession");
				accSet.add(acc);
			}
		}
		return accSet;
	}
	
	private void refreshSequenceCache() throws Exception
	{
		if (fastaFile.length() == 0)
			return;
		
		// see if we already have all sequences
		
		boolean haveAllSequences = true;
		
		if (proteinSequenceCache == null)
			proteinSequenceCache = new HashMap<String, String>();
		
		TreeSet<String> neededAccessions = getAllAccessions();
		neededAccessions.removeAll(proteinSequenceCache.keySet());
		
		if (neededAccessions.isEmpty())
			return;
		
		
		System.out.println("reading fasta file for sequences.");
		ITRAQReportUI.setStatus("reading fasta file for sequences.");
		
		// create a local HashMap to fill
		
		HashMap<String, String> localMap = new HashMap<String, String>();
		
		// open and read the fasta file and get all required sequences
		
		BufferedReader in = new BufferedReader(new FileReader(fastaFile));
		String line;
		boolean needThisSequence = false;
		String sequence = "";
		String accession = "";
		int countRead = 0;
		int countAddedSinceCO4A = 0;
		boolean addedCO4A = false;
		while (true)
		{
			line = in.readLine();
			if (line == null || line.startsWith(">"))
			{
				countRead++;
				if (countRead % 10000 == 0)
				{
					ITRAQReportUI.updateProgressIndicator(0);
					Display.getDefault().readAndDispatch();
				}
				if (countRead % 100 == 0)
				{
					Display.getDefault().readAndDispatch();
				}
				
				if (needThisSequence)
				{
					if (accession.equals("H13_MOUSE"))
					{
						countAddedSinceCO4A = 0;
						addedCO4A = true;
					}
					localMap.put(accession, sequence);
					neededAccessions.remove(accession);
				}
				countAddedSinceCO4A++;
				
				if (line == null)
					break;
				
				if (neededAccessions.isEmpty())
					break;
				
				needThisSequence = false;
				sequence = "";
				accession = "";
				for (String acc : neededAccessions)
				{
					if (line.substring(0,30).contains(acc) && 
							line.substring(0,30).matches(".*[-|( :;>]" + acc + ".*"))  // using substring to speed this up a bit
					{
						needThisSequence = true;
						accession = acc;
						break;
					}
				}
			}
			else if (needThisSequence)
				sequence += line.trim();	
		}
		// now add the local map to the static map
		proteinSequenceCache.putAll(localMap);
		

		// see if we got them all
		neededAccessions = getAllAccessions();
		neededAccessions.removeAll(proteinSequenceCache.keySet());
		if (!neededAccessions.isEmpty())
		{
			System.out.println("didn't find " + neededAccessions.toString());
			ITRAQReportUI.setStatus("didn't find " + neededAccessions.toString());
		}

		System.out.println("done reading fasta");
		ITRAQReportUI.setStatus("done reading fasta");
	}
}
