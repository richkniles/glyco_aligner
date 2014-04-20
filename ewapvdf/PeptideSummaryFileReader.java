package edu.ucsf.library.sprot.ewapvdf;

public class PeptideSummaryFileReader extends SummaryFileReader
{
	public PeptideSummaryFileReader(String fileName) throws Exception
	{
		super(fileName);
	}
	
	public String readLine() throws Exception
	{
		if (in != null)
			return in.readLine();
		else if (inMascot != null)
			return inMascot.readLinePeptide();
		
		return null;
	}

}
