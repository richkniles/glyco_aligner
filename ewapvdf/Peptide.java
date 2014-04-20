package edu.ucsf.library.sprot.ewapvdf;

import java.util.*;

public class Peptide extends Thing
{
	String N;
	
/*	
	static String headers[] = {"N","Unused","Total","%Cov","%Cov(50)","%Cov(95)","Accessions","Names","Used","Annotation","Contrib","Conf","Sequence",
		"Modifications","Cleavages","dMass","Prec MW","Prec m/z","Theor MW","Theor m/z","Theor z","Sc","Spectrum",
		"Time","114:113","%Err 114:113",
		"115:113","%Err 115:113",
		"116:113","%Err 116:113",
		"117:113","%Err 117:113",
		"118:113","%Err 118:113",
		"119:113","%Err 119:113",
		"121:113","%Err 121:113",
		"Area 113","Err 113","Area 114","Err 114","Area 115","Err 115","Area 116","Err 116",
		"Area 117","Err 117","Area 118","Err 118","Area 119","Err 119","Area 121","Err 121",
		"Background"};

*/	
	int glycosite_column = 5000;
	
	public Peptide(String fileName, String line)
	{
		super(fileName, line);
		N = getString("N");
	}
	public boolean hasQuants()
	{
		for (int num = 113; num < 121; num++)
			for (int denom = 113; denom < 121; denom++)
			{
				String val = getString("" + num + ":" + denom);
				if (val != null)
				{
					try
					{
						double d = Double.parseDouble(val);
						return true;
					}
					catch (Exception e)
					{}
				}
			}
		return false;

	}
	private String proteinSequence()
	{
		String acc = getString("Accessions").split(";")[0];
		for (String a : acc.split("\\|"))
			if (a.contains("_"))
			{
				acc = a;
				break;
			}
		return ITRAQReport.proteinSequenceCache.get(acc);
	}
	private String getSequenceInContext()
	{
		String pepSeq = getString("Sequence");
		String protSeq = proteinSequence();
		String seqInContext = pepSeq;
		if (protSeq != null) {
			int loc = protSeq.indexOf(pepSeq);
			if (loc + pepSeq.length() < protSeq.length())
				seqInContext += protSeq.charAt(loc + pepSeq.length());
		}
		return seqInContext;
		
	}
	private int getLocInProtein()
	{
		if (proteinSequence() == null)
			return 0;
		return proteinSequence().indexOf(getString("Sequence"));
	}
	public String getGlycosites()
	{
		if (ITRAQReport.proteinSequenceCache == null)
			return "";
		
		String sequenceInContext = getSequenceInContext();
		String mods = super.getString("Modifications");
		
		int locInProtein = getLocInProtein();
		
		String siteList = "";
		if (sequenceInContext.matches(".*N[^G][ST].*") && mods.contains("Deamidated"))
		{
			String modsList[] = mods.split("; ");
			for (int i = 0; i < modsList.length; i++)
			{
				if (modsList[i].startsWith("Deamidated(N)@"))
				{
					int loc = Integer.parseInt(modsList[i].split("@")[1])-1;
					int end = loc + 3; 
					if (end >= sequenceInContext.length())
						end = sequenceInContext.length();
					if(sequenceInContext.substring(loc, end).matches(".*N[^G][ST].*"))
					{
						if (siteList.length() > 0)
							siteList += "|";
						siteList += (locInProtein + loc + 1);
					}
				}
			}
		}
		return siteList;
	}
	public String getString(String header)
	{
		if (header.equals("Glycosites (fasta reqd.)"))
			return getGlycosites();
		
		
		return super.getString(header);
	}

	public String getString(int i)
	{
		if (i == glycosite_column)
			return getString("Glycosites (fasta reqd.)");
		return super.getString(i);
	}
	
	public int getColumnNumber(String headerName)
	{
		if (headerName.endsWith("Glycosites (fasta reqd.)"))
			return glycosite_column;
		Integer colNo = hdrLookup.get(headerName);
		if (colNo != null)
			return colNo.intValue();
		return -1;
	}
}
