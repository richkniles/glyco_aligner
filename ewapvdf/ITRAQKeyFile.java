package edu.ucsf.library.sprot.ewapvdf;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import edu.ucsf.library.sprot.ewapvdf.EwaPVDF.ITRAQKey;
import edu.ucsf.library.util.FileUtil;


public class ITRAQKeyFile 
{
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
	}
	
	public ArrayList<ITRAQKey> keys = new ArrayList<ITRAQKey>();

	public ITRAQKeyFile(String fileName) throws Exception
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
	public String getLabel(String fileName, String reporter)
	{
		for (ITRAQKey k : keys)
		{
			if (fileName.contains(k.fileKey) && reporter.equals(k.reporter))
				return k.label;
		}
		return null;
	}
	
	public String getReporter(String fileName, String label)
	{
		for (ITRAQKey k : keys)
		{
			if (fileName.contains(k.fileKey) && label.equals(k.label))
				return k.reporter;
		}
		return null;
	}
	
	public String getReporterRatioFromLabelRatio(String fileName, String labelRatio)
	{
		String spl[] = labelRatio.split("/");
		String num = getReporter(fileName, spl[0]);
		String denom = getReporter(fileName, spl[1]);
		if (num != null && denom != null)
			return num + ":" + denom;
		return null;
	}
	
	public String getLabelRatioFromReporterRatio(String fileName, String labelRatio)
	{
		String spl[] = labelRatio.split(":");
		String num = getLabel(fileName, spl[0]);
		String denom = getLabel(fileName, spl[1]);
		if (num != null && denom != null)
			return num + "/" + denom;
		return null;
	}
	
	public String[] getAllLabels()
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
	
	public String[] getFileRatioLabels(String fileName)
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
