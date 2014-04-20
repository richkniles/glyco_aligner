package edu.ucsf.library.sprot.ewapvdf;

import java.io.*;
public class ReportPrinter implements Runnable
{
	ITRAQReport report;
	PrintStream out;
	boolean showPeptides;
	
	public static void setStatus(String text)
	{
		ITRAQReportUI.statusMsg = text;
	}
	public ReportPrinter(ITRAQReport theReport, PrintStream out, boolean showPeptides) throws Exception
	{
		this.report = theReport;
		this.out = out;
		this.showPeptides = showPeptides;
	}
	public void run()
	{
		try {
			setStatus("Printing report");
			report.printProteinReport(null, out, showPeptides);
			setStatus("Report printed");
		} catch (Exception e) {
			report.isReportPrinting = false;
			e.printStackTrace();
			setStatus(e.getLocalizedMessage());

		}	
	}
	public static void printReport(ITRAQReport report, PrintStream out, boolean showPeptides) throws Exception
	{
		ITRAQReport.abortReportPrinting = false;
		new Thread(new ReportPrinter(report, out, showPeptides)).start();
	}
}
