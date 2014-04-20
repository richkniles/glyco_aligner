package edu.ucsf.library.sprot.ewapvdf;

import java.util.HashMap;

public abstract class Thing 
{
	static HashMap<String, HashMap<String,Integer>> headerLookupCache = new HashMap<String, HashMap<String,Integer>>();
	HashMap<String,Integer> hdrLookup = null;
	
	String strVals[];
	
	public static void setStatus(String text)
	{
		ITRAQReportUI.statusMsg = text;
	}
	
	static void fixiTRAQRatios(String headers[], String headerLine)
	{
		String spl[] = headerLine.split("\t");
		int newDenom = 0;
		for (int i = 0; i < spl.length && newDenom == 0; i++)
			if (spl[i].contains(":"))
				newDenom = Integer.parseInt(spl[i].split(":")[1]);
		for (int i = 0; i < headers.length; i++)
		{
			if (headers[i].contains(":"))
			{
				String thisH = headers[i];
				String start = "";
				if (thisH.contains(" "))
				{
					String sp[] = thisH.split(" ");
					start = sp[0] + " ";
					thisH = sp[1];
				}
				int numerator = Integer.parseInt(thisH.split(":")[0]);
				int denominator = Integer.parseInt(thisH.split(":")[1]);
				if (denominator == newDenom)
					return;
				int newNum = numerator;
				if (numerator <= newDenom)
						newNum = numerator-1;
				if (newNum == 120)
					newNum = 119;
				
				headers[i] = start + newNum + ":" + newDenom;
			}
		}
	}
	
	public static void setHeaderInfo(String fileName, String headerLine)
	{
		HashMap<String,Integer> hdrLookup = new  HashMap<String,Integer>();

		String spl[] = headerLine.split("\t");
		for (int i = 0; i < spl.length; i++)
		{
			hdrLookup.put(spl[i], i);
		}
		headerLookupCache.put(fileName, hdrLookup);
	}

	int headerPosition(String headerName)
	{
		Integer i = hdrLookup.get(headerName);
		if (i == null)
			return -1;
		return i.intValue();
	}

	public Thing(String fileName, String line)
	{
		try {
			hdrLookup = headerLookupCache.get(fileName);
			strVals = line.split("\t");
		} catch (Exception e) {
			setStatus(e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
	public String getString(String hdrName)
	{
		if (hdrName.trim().isEmpty())
			return "";
		int i = headerPosition(hdrName);
		if (i != -1 && i < strVals.length)
			return strVals[i];
		return "";
	}
	public String getStringRound(String hdrName)
	{
		try
		{
			double val = Double.parseDouble(getString(hdrName));
			val = Math.round(val*100)/100.;
			return "" + val;
		}
		catch(Exception e)
		{
			setStatus(e.getLocalizedMessage());
		}
		return getString(hdrName);
	}
	public double getDouble(String hdrName)
	{
		return Double.parseDouble(getString(hdrName));
	}
	public int getInt(String hdrName)
	{
		return Integer.parseInt(getString(hdrName));
	}

	public String getString(int i)
	{
		if (i != -1 && i < strVals.length)
			return strVals[i];
		return "";
	}
	public String getStringRound(int i)
	{
		try
		{
			double val = Double.parseDouble(getString(i));
			val = Math.round(val*100)/100.;
			return "" + val;
		}
		catch(Exception e)
		{
			setStatus(e.getLocalizedMessage());
		}
		return getString(i);
	}
	public double getDouble(int i)
	{
		return Double.parseDouble(getString(i));
	}
	public int getInt(int i)
	{
		return Integer.parseInt(getString(i));
	}


}
