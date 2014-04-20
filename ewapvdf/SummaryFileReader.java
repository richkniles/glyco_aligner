package edu.ucsf.library.sprot.ewapvdf;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

public class SummaryFileReader 
{
	BufferedReader in = null;
	MascotCSVReader inMascot = null;
	
	public SummaryFileReader(String fileName) throws Exception
	{
		if (fileName.endsWith(".txt") && fileName.contains("Summary"))
			in = new BufferedReader(new FileReader(fileName));
		else if (fileName.endsWith(".dat"))
		{
			inMascot = new MascotCSVReader(fileName);
		}
	}
}
