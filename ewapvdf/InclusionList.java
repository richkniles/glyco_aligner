package edu.ucsf.library.sprot.ewapvdf;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

import edu.ucsf.library.sprot.ewapvdf.EwaPVDF.ITRAQKey;
import edu.ucsf.library.util.FileUtil;

public class InclusionList 
{
	public static void main(String args[]) throws Exception
	{
		String path = "/Volumes/work/lab/niles/Sjogrens grant/for inclusion list/";
		boolean showPeptides = true;
		boolean useStdsForSharedPeptides = false;
		int minSamplesPerPeptide = 0;
		
		
//--------------------------------------------------------------------------------------		
		
		String outFileName = "PeptideTable.txt";
		
		
		
		
		
		File[] peptideSummaryFiles = FileUtil.fileSort(FileUtil.getFileList(new File(path), "PeptideSummary.txt", false));
		File[] proteinSummaryFiles = FileUtil.fileSort(FileUtil.getFileList(new File(path), "ProteinSummary.txt", false));
		
		
		ProteinGroupList groups[] = new ProteinGroupList[peptideSummaryFiles.length];
		
		
		ProteinGroupMasterList masterList = new ProteinGroupMasterList();
		
		for (int i = 0; i < peptideSummaryFiles.length; i++)
		{
			if (peptideSummaryFiles[i].getName().startsWith("."))
				continue;
			groups[i] = new ProteinGroupList(proteinSummaryFiles[i].getAbsolutePath(), peptideSummaryFiles[i].getAbsolutePath());
			for (String N : groups[i].groups.keySet())
			{
				ProteinGroup pg = groups[i].groups.get(N);
				MasterListGroup mg = new MasterListGroup(pg);

				masterList.add(mg);
			}
		}
		
		// printem
		PrintStream out = System.out;//new PrintStream(new BufferedOutputStream(new FileOutputStream(path + outFileName)));
		
		out.print("\t\t\t");
		for (int i = 0; i < peptideSummaryFiles.length; i++)
			out.print("\t" + peptideSummaryFiles[i].getName());
		out.println();
		
		
		for (MasterListGroup mg : masterList.groups)
		{
			TreeSet<String> universalPeptides = null;

			String accs = mg.accessionString();
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
				out.print("\t");
				ProteinGroupList thisFileList = groups[i];
				ProteinGroup pg = thisFileList.find(mg, false);

				if (pg != null)
					out.print("x");
			}
			out.println();
			
			
			
			// if showing peptides ........
			if (!showPeptides)
				continue;
			for (String pepStr : mg.peptides)
			{
				String spl[] = pepStr.split(":");
				out.print("\t" + spl[0] + ":" + spl[1] + "\t ");
				double maxNumQuants = -1;
				ArrayList<Peptide>[] allPeptides = new ArrayList[groups.length];
				
				double mz = 0;
				int chg = 0;
				for (int nf = 0; nf < groups.length; nf++)
				{
					ProteinGroup pg = groups[nf].find(mg, false);
					if (pg != null)
						for (Peptide pep : pg.peptides)
							if ((pep.getString("Sequence") + ":" + pep.getString("Modifications")).equals(pepStr) && pep.hasQuants())
							{
								mz = pep.getDouble("Theor m/z");
								chg = pep.getInt("Theor z");
							}
				}
				out.print(mz + "\t" + chg + ""+"");
				
				for (int nf = 0; nf < groups.length; nf++)
				{
					out.print("\t");
					ProteinGroup pg = groups[nf].find(mg, false);
					if (pg != null)
						for (Peptide pep : pg.peptides)
							if ((pep.getString("Sequence") + ":" + pep.getString("Modifications")).equals(pepStr) && pep.hasQuants())
							{
								out.print(pep.getString("Time"));
								break;
							}
				}
				out.println();

			}

		}
		out.close();
	}

	
}
