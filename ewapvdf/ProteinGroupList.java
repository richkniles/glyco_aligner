package edu.ucsf.library.sprot.ewapvdf;

import java.util.*;
import java.io.*;

import org.eclipse.swt.widgets.Display;

public class ProteinGroupList
{
	public String fileName;
	
	
	public static boolean abort = false;
	
	HashMap<String,ProteinGroup> groups = new HashMap<String,ProteinGroup>();
	HashMap<String,ArrayList<String>> index = new HashMap<String,ArrayList<String>>();
	
	public static String proteinsToIncludeSharedPeptidesForQuant[] = new String[0];
	
	public ProteinGroupList(String proteinSummaryFileName, String peptideSummaryFileName) throws Exception
	{
		this(proteinSummaryFileName, peptideSummaryFileName, false, 1, 80);
	}
	public ProteinGroupList(String proteinSummaryFileName, String peptideSummaryFileName, boolean usedFlag, double minProtScore, double minPepScore) throws Exception
	{
		fileName = proteinSummaryFileName;
		ProteinSummaryFileReader inProt = new ProteinSummaryFileReader(proteinSummaryFileName);
		if (fileName.endsWith(".dat"))
			fileName = inProt.inMascot.spectrumDataFile;
		String line = inProt.readLine();
		Protein.setHeaderInfo(proteinSummaryFileName, line);
		TreeSet<String> groupNsToSkip = new TreeSet<String>();
		while((line = inProt.readLine()) != null)
		{
			Protein prot = new Protein(proteinSummaryFileName, line);
			ProteinGroup gp = groups.get(prot.N);
			if (gp == null)
			{
				if (groupNsToSkip.contains(prot.N))
					continue;
				if (minProtScore > 0 && prot.getDouble("Unused") < minProtScore)
				{
					groupNsToSkip.add(prot.N);
					continue;
				}
				groups.put(prot.N, gp = new ProteinGroup());
			}
			gp.add(prot);
			ArrayList<String> bin = index.get(prot.getString("Accession"));
			if (bin == null)
			{
				bin = new ArrayList<String>();
				index.put(prot.getString("Accession"), bin);
			}
			bin.add(prot.N);

		}
		PeptideSummaryFileReader inPep = new PeptideSummaryFileReader(peptideSummaryFileName);
		line = inPep.readLine();
		Peptide.setHeaderInfo(peptideSummaryFileName, line);
		while((line = inPep.readLine()) != null)
		{
			if (line.contains("TRFL_HUMAN"))
			{
				int x = 1;
				x++;
			}
			if (abort)
				return;
			Peptide pep = new Peptide(peptideSummaryFileName, line);
			if (pep.getDouble("Conf") < minPepScore || (usedFlag && !pep.getString("Used").equals("1")))
				continue;
			ProteinGroup gp = groups.get(pep.N);
			if (gp == null)
			{
				continue;
//				groups.put(pep.N, gp  = new ProteinGroup());
//				Protein prot = new Protein(peptideSummaryFileName, line);
//				gp.add(prot);
			}
			
/*			boolean includeSharedPep = false;
			if (pep.getString("Annotation").equals("auto - shared MS/MS"))
			{
				for (int i = 0; i < proteinsToIncludeSharedPeptidesForQuant.length && !includeSharedPep; i++)
				{
					for(Protein prot : gp.proteins)
						if (prot.getString("Accession").contains(proteinsToIncludeSharedPeptidesForQuant[i]))
							includeSharedPep = true;
				}
			}
			String used = pep.getString("Used");
			if (pep.getString("Used").equals("1") || includeSharedPep)
			{
				gp.add(pep);
			}
*/
			if (pep.getString("Sequence").equals("AERNR"))
			{
				int x = 1;
				x++;
			}
			gp.add(pep);
		}
/*		// remove groups with no peptides
		TreeSet<String> nsToRemove = new TreeSet<String>();
		for (String N : groups.keySet())
		{
			ProteinGroup gp = groups.get(N);
			if (gp.peptides.isEmpty())
				nsToRemove.add(N);
		}
		for (String N : nsToRemove)
			groups.remove(N);*/
	}

	
	
/*	
	public ProteinGroupList(String filePath, boolean usedFlag, double minProtScore, double minPepScore) throws Exception
	{
		String fileName = (new File(filePath)).getName();
		if (fileName.endsWith(".txt") && fileName.contains("PeptideSummary"))
		{
			// see if there's a protein summary too
			String peptideSummaryFileName = fileName;
			String proteinSummaryFileName = filePath.replaceAll("PeptideSummary", "ProteinSummary");
			if ((new File(proteinSummaryFileName)).exists())
			{
				BufferedReader in = new BufferedReader(new FileReader(proteinSummaryFileName));
				String line = in.readLine();
				Protein.setHeaderInfo(proteinSummaryFileName, line);
				TreeSet<String> groupNsToSkip = new TreeSet<String>();
				while((line = in.readLine()) != null)
				{
					Protein prot = new Protein(proteinSummaryFileName, line);
					if (prot.getString("Accession").contains("F1SFI7"))
					{
						int x = 1;
						x++;
					}
					ProteinGroup gp = groups.get(prot.N);
					if (gp == null)
					{
						if (groupNsToSkip.contains(prot.N))
							continue;
						if (minProtScore > 0 && prot.getDouble("Unused") < minProtScore)
						{
							groupNsToSkip.add(prot.N);
							continue;
						}
						groups.put(prot.N, gp = new ProteinGroup());
					}
					gp.add(prot);
				}
				in = new BufferedReader(new FileReader(peptideSummaryFileName));
				line = in.readLine();
				Peptide.setHeaderInfo(peptideSummaryFileName, line);
				while((line = in.readLine()) != null)
				{
					if (abort)
						return;
					Peptide pep = new Peptide(peptideSummaryFileName, line);
					if (pep.getDouble("Conf") < minPepScore || (usedFlag && !pep.getString("Used").equals("1")))
						continue;
					ProteinGroup gp = groups.get(pep.N);
					if (gp == null)
						continue;
					
					gp.add(pep);
				}
		
			}
			return;
		}
		PeptideSummaryFileReader in = new PeptideSummaryFileReader(filePath);
		String header = in.readLine();
		Peptide.setHeaderInfo(filePath, header);
		String line;
		int accLoc = 0;
		String spl[] = header.split("\t");
		for (String h : spl)
		{
			if (h.equals("Accessions"))
				break;
			accLoc++;
		}
		while((line = in.readLine()) != null)
		{
			String splt[] = line.split("\t");
			
			// find the accessions, and make a protein group for them.
			String accList[] = splt[accLoc].split(";");
			if (abort)
				return;
			Peptide pep = new Peptide(filePath, line);
			if (pep.getDouble("Conf") < minPepScore || (usedFlag && !pep.getString("Used").equals("1")))
				continue;
			ProteinGroup gp = groups.get(pep.N);
			if (gp == null)
				continue;
			
			gp.add(pep);
		}


	}
*/	
	
	HashMap<MasterListGroup,String> lookupCache = new HashMap<MasterListGroup,String>();
	
	public ProteinGroup find(MasterListGroup mg, boolean collapseWinners)
	{
		{ // I want to use N here, so have to "hide" it in a block
			
			String N = lookupCache.get(mg);
			if (N != null)
				return this.groups.get(N);
		}
		
		for (Protein p : mg.proteins)
		{
			ArrayList<String> bin = index.get(p.getString("Accession"));
			if (bin == null)
			{
				lookupCache.put(mg, "");
				return null;
			}
			for (String N : bin)
			{
				ProteinGroup pg = this.groups.get(N);
				if (!collapseWinners)
				{
					TreeSet<Protein> pgts = new TreeSet<Protein>(MasterListGroup.comp);
					pgts.addAll(pg.proteins);
					TreeSet<Protein> mgts = new TreeSet<Protein>(MasterListGroup.comp);
					mgts.addAll(mg.proteins);
					if (pgts.equals(mgts))
					{
						lookupCache.put(mg, N);
						return pg;
					}
				}
				else
				{
					TreeSet<Protein> intersect = mg.winnerSet();
					intersect.retainAll(pg.winnerSet());
					if (!intersect.isEmpty()) // common winner, collapse
					{
						lookupCache.put(mg, N);
						return pg;
					}
				}
			}
			
			lookupCache.put(mg, "");
			return null;
				
			
		}
		for (String N : groups.keySet())
		{
			Display.getDefault().readAndDispatch();
			ProteinGroup pg = groups.get(N);
			if (!collapseWinners)
			{
				TreeSet<Protein> pgts = new TreeSet<Protein>(MasterListGroup.comp);
				pgts.addAll(pg.proteins);
				TreeSet<Protein> mgts = new TreeSet<Protein>(MasterListGroup.comp);
				mgts.addAll(mg.proteins);
				if (pgts.equals(mgts))
				{
					lookupCache.put(mg, N);
					return pg;
				}
			}
			else
			{
				TreeSet<Protein> intersect = mg.winnerSet();
				intersect.retainAll(pg.winnerSet());
				if (!intersect.isEmpty()) // common winner, collapse
				{
					lookupCache.put(mg, N);
					return pg;
				}
			}
		}
		
		lookupCache.put(mg, "");
		return null;
	}
}
