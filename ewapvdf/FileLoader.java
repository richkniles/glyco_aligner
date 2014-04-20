package edu.ucsf.library.sprot.ewapvdf;

import java.util.HashMap;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

public class FileLoader implements Runnable {

	public static HashMap<String, Thread> loadedFiles = new HashMap<String, Thread>();
	
	public String fileToLoad;
	boolean usedFlag;
	double minProtScore;
	double minPepScore;
	boolean collapseWinners;
	
	static void setStatus(String text)
	{
		ITRAQReportUI.statusMsg = text;
	}
	static void updateProgressIndicator(int n)
	{
		ITRAQReportUI.statusMsg += ".";
	}
	
	public FileLoader(String fileName, boolean usedFlag, double minProtScore, double minPepScore, boolean collapseWinners)
	{
		fileToLoad = fileName;
		this.usedFlag = usedFlag;
		this.minProtScore = minProtScore;
		this.minPepScore = minPepScore;
		this.collapseWinners = collapseWinners;
	}
	public static void loadFiles(String fileList[], boolean usedFlag, double minProtScore, double minPepScore, boolean collapseWinners)
	{
		for (String fn : fileList)
		{
			if ((fn.contains("PeptideSummary") || fn.endsWith(".dat")) && !loadedFiles.keySet().contains(fn))
			{
				Thread t = new Thread(new FileLoader(fn, usedFlag, minProtScore, minPepScore, collapseWinners));
				loadedFiles.put(fn, t);
				t.start();
			}
		}
	}
	public static boolean allFilesLoaded()
	{
		for (String f : loadedFiles.keySet())
		{
			if (loadedFiles.get(f).isAlive())
				return false;
		}
		return true;
	}
	public static void abort()
	{
		ITRAQReport.abortFileLoad = true;
		ProteinGroupList.abort = true;
		while(!allFilesLoaded())
		{
			ITRAQReportUI.updateProgressIndicator(0);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				ITRAQReportUI.setStatus(e.getLocalizedMessage());
			}
		}
		loadedFiles = new HashMap<String, Thread>();
		ITRAQReport.groupListCache = new HashMap<String, ProteinGroupList>();
		ITRAQReport.abortFileLoad = false;
		ProteinGroupList.abort = false;
	}
	public void run() 
	{
		try {
			ITRAQReport.readNeededFiles(new String[] {fileToLoad}, usedFlag, minProtScore, minPepScore, collapseWinners);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			setStatus(e.getLocalizedMessage());
		}
		updateProgressIndicator(loadedFiles.keySet().size());
		ITRAQReportUI.needReportUpdate = true;
		Display.getDefault().wake();
	}

}
