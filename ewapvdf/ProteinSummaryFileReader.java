package edu.ucsf.library.sprot.ewapvdf;

import java.util.ArrayList;

public class ProteinSummaryFileReader extends SummaryFileReader
{
	
	public ProteinSummaryFileReader(String fileName) throws Exception
	{
		super(fileName);
	}
	
	public String readLine() throws Exception
	{
		if (in != null)
			return in.readLine();
		else if (inMascot != null)
			return inMascot.readLineProtein();
		
		return null;
	}
	

	
	
}
