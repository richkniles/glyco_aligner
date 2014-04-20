package edu.ucsf.library.sprot.ewapvdf;

import java.io.*;
import java.util.*;

import edu.ucsf.library.util.FileUtil;

public class EwaPVDF 
{
	static final String stds[] = {"THYG_BOVIN", "CATA_BOVIN", "ALDOA_RABIT", "OVAL_CHICK", "FRIL_HORSE", "ALBU_BOVIN", "cont|000035"};

	static boolean isStd(String x)
	{
		for (String std : stds)
			if (x.contains(std))
				return true;
		return false;
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
	static class ITRAQKey
	{
		String fileKey;
		String reporter;
		String label;
		
		public ITRAQKey(String fileKey, String reporter, String label) 
		{
			this.fileKey = fileKey;
			this.reporter = reporter;
			this.label = label;
		}
		
		public static ArrayList<ITRAQKey> keys = new ArrayList<ITRAQKey>();
		
		public static void readKeys(String fileName) throws Exception
		{
			BufferedReader in = null;
			try {
				in = new BufferedReader(new FileReader(fileName));
			} catch (Exception e) {
				FileUtil.diagnoseProblemWithFileName(fileName);
			}
			String line;
			while((line = in.readLine()) != null)
			{
				if (line.isEmpty())
					continue;
				String spl[] = line.split("\t");
				if (spl.length < 3)
					continue;
				keys.add(new ITRAQKey(spl[0], spl[1], spl[2]));
			}
		}
		
		public static String getLabel(String fileName, String reporter)
		{
			for (ITRAQKey k : keys)
			{
				if (fileName.contains(k.fileKey) && reporter.equals(k.reporter))
					return k.label;
			}
			return null;
		}
		
		public static String getReporter(String fileName, String label)
		{
			for (ITRAQKey k : keys)
			{
				if (fileName.contains(k.fileKey) && label.equals(k.label))
					return k.reporter;
			}
			return null;
		}
		
		public static String getReporterRatioFromLabelRatio(String fileName, String labelRatio)
		{
			String spl[] = labelRatio.split("/");
			String num = getReporter(fileName, spl[0]);
			String denom = getReporter(fileName, spl[1]);
			if (num != null && denom != null)
				return num + ":" + denom;
			return null;
		}
		
		public static String[] getAllLabels()
		{
			ArrayList<String> list = new ArrayList<String>();
			for (ITRAQKey k : keys)
			{
				boolean found = false;
				for (String label : list)
				{
					if (label.equals(k.label))
					{
						found = true;
						break;
					}
				}
				if (!found)
					list.add(k.label);
			}
			return list.toArray(new String[0]);
		}
		
		public static String[] getFileRatioLabels(String fileName)
		{
			ArrayList<String> fileLabels = new ArrayList<String>();
			
			String allLabels[] = getAllLabels();
			for (int i = 0; i < allLabels.length; i++)
			{
				for (int j = 0; j < allLabels.length; j++)
				{
					String potentialRatio = allLabels[j] + "/" + allLabels[i];
					String ratio = getReporterRatioFromLabelRatio(fileName, potentialRatio);
					if (ratio != null)
						fileLabels.add(potentialRatio);
				}
			}
			return fileLabels.toArray(new String[0]);
		}
	}

	public static String tabs(int n)
	{
		String ret = "";
		while(n-- > 0)
			ret += "\t";
		return ret;
	}
	
	public static void main(String[] args) throws Exception
	{

		String path = "/Volumes/work/lab/Ewa/PVDF paper/Nov Acc/Pilot files/Acc EditSummary/";
		path = "/Volumes/work/lab/Ewa/PVDF paper/Nov Rec/Pilot files/Rich NoEditSummary/";
		path = "/Volumes/work/lab/Ewa/PVDF paper/Nov Rec/Pilot files/Repr_EditSummary/";
		path = "/Volumes/work/lab/Ewa/PVDF paper/Timecourse_ewa to rich/Time course overnight/";
		//path = "/Volumes/work/lab/Ewa/PVDF paper/Timecourse_ewa to rich/Time course 4h/";
		path = "/Volumes/work/lab/niles/Sjogrens grant/117/";
		path = "/Volumes/work/lab/niles/Sjogrens grant/Peptide Summaries/";
		path = "/Volumes/work/lab/niles/Sjogrens grant/Pilot background subtract/";
		path = "/Volumes/work/lab/niles/Sjogrens grant/Pilot background subtract/Reruns with incl/";
		path = "/Volumes/work/lab/Biospecimens-Contract/Expts/iTRAQ/Plasma iTRAQ data_50 donors/group files/Good ones xml done/Protein and Peptide Summaries/";

		
		boolean showPeptides = true;
		boolean useStdsForSharedPeptides = false;
		int minSamplesPerPeptide = 0;
		
		String iTRAQProteinColumns[] = {""};//, "EF "};//{"", "PVal ", "EF ","LowerCI ","UpperCI " };
		String iTRAQPeptideColumns[] = {""};//, "%Err " };
		
//--------------------------------------------------------------------------------------		
		
		String outFileName = "BigTable " + (useStdsForSharedPeptides?"auto+shared ":"only auto ") + ((minSamplesPerPeptide>0)?("only peptides seen in " + minSamplesPerPeptide + " samples"):"all ") + "peptides1.txt";
		
		if (useStdsForSharedPeptides)
			ProteinGroupList.proteinsToIncludeSharedPeptidesForQuant = stds;
		
		
		
		ITRAQKey.readKeys(path + "KeyFile for iTRAQ.txt");
		
		File[] peptideSummaryFiles = FileUtil.fileSort(FileUtil.getFileList(new File(path), "PeptideSummary.txt", false));
		File[] proteinSummaryFiles = FileUtil.fileSort(FileUtil.getFileList(new File(path), "ProteinSummary.txt", false));
		
		
		ProteinGroupList groups[] = new ProteinGroupList[peptideSummaryFiles.length];
		
		MasterListGroup.comp = new 	Comparator<Protein>()
									{
										boolean isStd(Protein a)
										{
											for (String std : stds)
												if (a.getString("Accession").contains(std))
													return true;
											return false;
										}
										public int compare(Protein a, Protein b)
										{
											if (isStd(a) && !isStd(b))
												return -1;
											if (isStd(b) && !isStd(a))
												return 1;
											if (a.getString("Accession").contains("_") && !b.getString("Accession").contains("_"))
												return -1;
											if (b.getString("Accession").contains("_") && !a.getString("Accession").contains("_"))
												return 1;
											return a.getString("Accession").compareTo(b.getString("Accession"));
										}
									};
		
		ProteinGroupMasterList.comp = new Comparator<MasterListGroup>()
		{
			public int compare(MasterListGroup a, MasterListGroup b)
			{
				if (isStd(a.accessionString()) && !isStd(b.accessionString()))
					return -1;
				if (isStd(b.accessionString()) && !isStd(a.accessionString()))
					return 1;
				return a.accessionString().compareTo(b.accessionString());	
			}
		};
		ProteinGroupMasterList masterList = new ProteinGroupMasterList();
		
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

//				if (isStd(mg.accessionString()))
					masterList.add(mg);
					//masterList.add(groups[i].groups.get(N));
			}
		}
		
		// printem
		PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(path + outFileName)));
		
		out.print("\tAccessions\t\t");
		for (int i = 0; i < peptideSummaryFiles.length; i++)
			out.print("\t" + peptideSummaryFiles[i].getName() + tabs(7*iTRAQProteinColumns.length-1));
		out.println();
		
		// Print the human names of the itraq ratios
		out.print("\t\t\t");
		for (int i = 0; i < peptideSummaryFiles.length; i++)
		{
			String fileName = peptideSummaryFiles[i].getName();
			String fileRatios[] = ITRAQKey.getFileRatioLabels(fileName);
			
			ProteinGroupList thisFileList = groups[i];
			ProteinGroup pg = thisFileList.groups.get("1");
			for (String ratioLabel : fileRatios)
			{
				String val = "";
				for (Protein prot : pg.proteins)
				{
					String xx = ITRAQKey.getReporterRatioFromLabelRatio(fileName, ratioLabel);
					for (int itCol = 0; itCol < iTRAQProteinColumns.length; itCol++)
					{
						val = prot.getString(iTRAQProteinColumns[itCol] + xx);
						if (val != null)
						{
							out.print("\t" + iTRAQProteinColumns[itCol] + ratioLabel);
						}
					}
					break;
				}
			}
		}
		out.println();
		
		// print the reporter mass names of the iTRAQ ratios
		out.print("\t\t\t");
		for (int i = 0; i < peptideSummaryFiles.length; i++)
		{
			String fileName = peptideSummaryFiles[i].getName();
			String fileRatios[] = ITRAQKey.getFileRatioLabels(fileName);
			
			ProteinGroupList thisFileList = groups[i];
			ProteinGroup pg = thisFileList.groups.get("1");
			for (String ratioLabel : fileRatios)
			{
				String val;
				for (Protein prot : pg.proteins)
				{
					String xx = ITRAQKey.getReporterRatioFromLabelRatio(fileName, ratioLabel);
					for (int itCol = 0; itCol < iTRAQProteinColumns.length; itCol++)
					{
						val = prot.getString(iTRAQProteinColumns[itCol] + xx);
						if (val != null)
							out.print("\t" + iTRAQProteinColumns[itCol] + ITRAQKey.getReporterRatioFromLabelRatio(fileName, ratioLabel));
					}
					break;
				}
			}
		}
		out.println();
		for (MasterListGroup mg : masterList.groups)
		{
			TreeSet<String> universalPeptides = null;
			if (minSamplesPerPeptide > 0)
			{
				// find which peptides are in every sample
				universalPeptides = new TreeSet<String>();
				for (String mgPepSeqMods : mg.peptides)
				{
					int numFilesThisPeptide = 0;
					for (ProteinGroupList thisFileList : groups)
					{
						ProteinGroup pg = thisFileList.find(mg, false);
						if (pg != null)
						{
							for (Peptide pep : pg.peptides)
							{
								String thisPepSeqMods = pep.getString("Sequence") + ":" + pep.getString("Modifications");
								if (thisPepSeqMods.equals(mgPepSeqMods))
								{
									numFilesThisPeptide++;
									break;
								}
							}
						}
					}
					if (numFilesThisPeptide >= minSamplesPerPeptide)
						universalPeptides.add(mgPepSeqMods);
				}
			}

			String accs = mg.accessionString();
			if (accs.contains("IGHA2"))
			{
				int x = 1;
				x++;
			}
			String accspl[] = accs.split(";");
			String spAcc = "";
			accs = "";
			for (String acc: accspl)
			{
				String bspl[] = acc.split("\\|");
				spAcc = bspl[1];
				acc = acc.replaceAll("sp\\|.*\\|", "");
				if (!accs.isEmpty())
					accs += ";";
				accs += acc;
			}
			out.print(accs + "\t");
			String name = mg.proteins.get(0).getString("Name");
			if (name.contains(" OS="))
					name = name.substring(0, name.indexOf(" OS=")).trim();
			out.print("(" + spAcc + ") " + name + "\t\t ");
			for (int i = 0; i < groups.length; i++)
			{
				String fileName = peptideSummaryFiles[i].getName();
				String fileRatios[] = ITRAQKey.getFileRatioLabels(fileName);
				
				ProteinGroupList thisFileList = groups[i];
				ProteinGroup pg = thisFileList.find(mg, false);

				if (pg != null)// && pg.countPeptidesWithQuants() >= 5)
				{
					Protein prot = pg.findProteinWithQuants();
					if (prot != null)
					{
						for (String ratioLabel : fileRatios)
						{
							String xx = ITRAQKey.getReporterRatioFromLabelRatio(fileName, ratioLabel);
							for (int itCol = 0; itCol < iTRAQProteinColumns.length; itCol++)
							{
								String val = prot.getString(iTRAQProteinColumns[itCol] + xx);
								if (val != null)
								{
									double myVal = pg.computeRatio(xx, universalPeptides);
									double theirVal = -1;
									if (!val.isEmpty())
										theirVal = Double.parseDouble(val);
									double diffpct = Math.abs(myVal-theirVal)/((myVal+theirVal)/2);
									//if (iTRAQProteinColumns[itCol].isEmpty() && diffpct > .05)
									if (false)
									{
										int nPeptides = pg.peptides.size();
										int nPeptidesUsed = 0;
										int nZero = 0;
										for (Peptide pep : pg.peptides)
										{
											String thisPepSeqMods = pep.getString("Sequence") + ":" + pep.getString("Modifications");
											if (minSamplesPerPeptide>0 && !universalPeptides.contains(thisPepSeqMods))
												continue;
											nPeptidesUsed++;
											if (!pep.getString(xx).isEmpty() && pep.getDouble(xx) == 0)
												nZero++;
										}
										System.out.println(accs + "\t" + fileName + "\t" + nZero + "/" + nPeptidesUsed + "/" + nPeptides + " peptides\t" +  xx + "\t" +  Math.round(myVal*1000)/1000. + "\t" + Math.round(theirVal*1000)/1000.+ "\t" + Math.round(diffpct*100) + "%");
									}
									try
									{
										if (minSamplesPerPeptide == 0)
										{// print the PP value
											out.print("\t" + Math.round(Double.parseDouble(val)*100)/100.);
										}
										else
										{// print my recomputed value
											if (iTRAQProteinColumns[itCol].isEmpty())
												out.print("\t" + Math.round(myVal*100)/100.);
											else if (iTRAQProteinColumns[itCol].equals("EF "))
												out.print("\t" + Math.round(pg.computeErrorFactor(xx, universalPeptides)*100)/100.);
											else
												out.print("\tXXX");
										}
									}
									catch (Exception e)
									{
										out.print("\t" + val);
									}
								}
							}
						}
					}
					else
						out.print(tabs(iTRAQProteinColumns.length*7));
				}
				else
					out.print(tabs(iTRAQProteinColumns.length*7));
			}
			out.println();
			
			
			
			// if showing peptides ........
			if (!showPeptides)
				continue;
			for (String pepStr : mg.peptides)
			{
				String spl[] = pepStr.split(":");
				out.print("\t\t" + spl[0] + ":" + spl[1] + "\t ");
				double maxNumQuants = -1;
				ArrayList<Peptide>[] allPeptides = new ArrayList[groups.length];
				
				for (int nf = 0; nf < groups.length; nf++)
				{
					allPeptides[nf] = new ArrayList();
					ProteinGroupList thisFileList = groups[nf];
					int countQuants = 0;
					ProteinGroup pg = thisFileList.find(mg, false);
					if (pg != null)
						for (Peptide pep : pg.peptides)
							if ((pep.getString("Sequence") + ":" + pep.getString("Modifications")).equals(pepStr) && pep.hasQuants())
							{
								countQuants++;
								allPeptides[nf].add(pep);
							}
					maxNumQuants = Math.max(maxNumQuants, countQuants);
				}
				for (int nq = 0; nq < maxNumQuants; nq++)
				{
					if (nq > 0)
						out.print("\t\t\t");
					for (int nf = 0; nf < groups.length; nf++)
					{
						String fileName = peptideSummaryFiles[nf].getName();
						String fileRatios[] = ITRAQKey.getFileRatioLabels(fileName);
						if (allPeptides[nf].size() > nq)
						{
							Peptide pep = allPeptides[nf].get(nq);
							for (String ratioLabel : fileRatios)
							{
								String xx = ITRAQKey.getReporterRatioFromLabelRatio(fileName, ratioLabel);
								if (pep.getString(xx) != null)
								{
									int countColsPrinted = 0;
									for (int itCol = 0; itCol < iTRAQPeptideColumns.length; itCol++)
									{
										String val = pep.getString(iTRAQPeptideColumns[itCol] + xx);
										if (val != null)
										{
											try
											{
												out.print("\t" + Math.round(Double.parseDouble(val)*10000)/10000.);
											}
											catch (Exception e)
											{
												out.print("\t" + val);
											}
											countColsPrinted++;
										}
									}
									out.print(tabs(iTRAQProteinColumns.length - countColsPrinted));
								}
							}

						}
						else
							out.print(tabs(iTRAQProteinColumns.length*7));
					}
					out.println();
				}

			}

		}
		out.close();
	}

}
