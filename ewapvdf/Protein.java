package edu.ucsf.library.sprot.ewapvdf;

import java.util.HashMap;

public class Protein extends Thing
{
	String N;

/*	
	static final String headers[] = {"N","Unused","Total","%Cov","%Cov(50)","%Cov(95)","Accession","Name","Species","Peptides(95%)","","",
		"114:113","PVal 114:113","EF 114:113","LowerCI 114:113","UpperCI 114:113",
		"115:113","PVal 115:113","EF 115:113","LowerCI 115:113","UpperCI 115:113",
		"116:113","PVal 116:113","EF 116:113","LowerCI 116:113","UpperCI 116:113",
		"117:113","PVal 117:113","EF 117:113","LowerCI 117:113","UpperCI 117:113",
		"118:113","PVal 118:113","EF 118:113","LowerCI 118:113","UpperCI 118:113",
		"119:113","PVal 119:113","EF 119:113","LowerCI 119:113","UpperCI 119:113",
		"121:113","PVal 121:113","EF 121:113","LowerCI 121:113","UpperCI 121:113",
		"Biological Processes","Molecular Function","PANTHER ID"};
	
*/
	// header columns needing special handling
	
	int protein_name_column = 5000;
	int accession_column =    5001;
	int entry_name_column =   5002;
	int sp_accession_column = 5003;
	
	public Protein(String fileName, String line)
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
					{
						setStatus(e.getLocalizedMessage());
					}
				}
			}
		return false;
	}
	public String getString(String header)
	{
		if (header.equals("Protein Name"))
		{
			String ret = super.getString("Name");
//			if (ret.contains(" OS="))
//				return ret.substring(0, ret.indexOf(" OS=")).trim();
//			else
				return ret;
		}
		if (header.equals("Accession"))
		{
			// see if accession is composite.  If so, pick the entry name part
			String rawField = super.getString("Accession");
			if (rawField.contains("|"))
			{
				String spl[] = rawField.split("\\|");
				for (String part : spl)
					if (part.contains("_"))
						return part;
			}
			return rawField;
		}
		if (header.equals("Entry Name"))
		{
			// see if accession is composite.  If so, pick the entry name part
			String rawField = super.getString("Accession");
			if (rawField.contains("|"))
			{
				String spl[] = rawField.split("\\|");
				for (String part : spl)
					if (part.contains("_"))
						return part;
			}
			return rawField;
		}
		if (header.equals("SP Accession"))
		{
			// see if accession is composite.  If so, pick the SwissProt accession part
			String rawField = super.getString("Accession");
			if (rawField.contains("|"))
			{
				String spl[] = rawField.split("\\|");
				for (int i = 0; i < spl.length; i++)
					if (spl[i].equals("sp") && spl.length > i+1)
					{
						return spl[i+1];
					}
			}
			return rawField;
		}
		
		return super.getString(header);
	}
	
	public String getString(int column)
	{
		if (column == protein_name_column)
			return getString("Protein Name");
		if (column == accession_column)
			return getString("Accession");
		if (column == entry_name_column)
			return getString("Entry Name");
		if (column == sp_accession_column)
			return getString("SP Accession");
		return super.getString(column);
	}
	
	public int getColumnNumber(String columnName)
	{
		if (columnName.equals("Protein Name"))
			return protein_name_column;
		if (columnName.equals("Accession"))
			return accession_column;
		if (columnName.equals("Entry Name"))
			return entry_name_column;
		if (columnName.equals("SP Accession"))
			return sp_accession_column;
		Integer colNum = hdrLookup.get(columnName);
		if (colNum != null)
			return colNum.intValue();
		return -1;
	}
}


