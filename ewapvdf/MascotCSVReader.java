package edu.ucsf.library.sprot.ewapvdf;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import edu.ucsf.library.sprot.sequence.SequenceTools;

public class MascotCSVReader 
{
	BufferedReader in;
	String curLine;

	String spectrumDataFile;
	String dbName;
	
	public String modsTable[]; 

	
	class MascotProtein
	{
		ArrayList<MascotPeptide> peptides;
		int nextPeptide;
		
		String N;
		String accession;
		String name;
		String sequence;
		String score;
		
		public MascotProtein(String csvLine)
		{
			String spl[] = splitCSVLine(csvLine);
			peptides = new ArrayList<MascotPeptide>();
			nextPeptide = 0;
			N = spl[0];
			accession = spl[1];
			name = spl[2];
			sequence = spl[6];
			score = spl[3];
		}
		double pctCoverage(double confidenceThreshold)
		{
			int coverageMap[] = new int[sequence.length()];
			for (MascotPeptide p : peptides)
			{
				if (p.confidence >= confidenceThreshold)
				{
					int loc1 = sequence.indexOf(p.sequence);
					int loc = SequenceTools.indexOf(sequence, p.sequence);
					//int loc = sequence.indexOf(p.sequence);
					if (loc != loc1)
					{
						int x = 1;
						x++;
						
					}
					for (int i = loc; i < loc + p.sequence.length(); i++)
						coverageMap[i] = 1;
				}
			}
			int covered = 0;
			int total = 0;
			for (int i : coverageMap)
			{
				covered += i;
				total++;
			}
			return 100. * covered / total;
		}
		String ProteinSummaryLine()
		{
			TreeSet<String> pep95 = new TreeSet<String>();
			for (MascotPeptide p : peptides)
				if (p.confidence >= .95)
					pep95.add(p.sequence + ":" + p.mods);
			String retVal = "";
			String species = "";
			if (accession.contains("_"))
				species = accession.split("_")[1];
				
			retVal = N + "\t" + score + "\t" + score + "\t" + pctCoverage(0) + "\t" + pctCoverage(.50) + "\t" + pctCoverage(.95) + "\t" + accession + "\t" + name + "\t" + species + "\t" + pep95.size();
			return retVal;
		}
	}
	class MascotPeptide
	{
		MascotProtein parent;
		
		String N;
		String sequence;
		String mods;
		double confidence;
		String mz;
		String theorMz;
		String theorMW;
		int z;
		String mw;
		String spectrum;
		
		public MascotPeptide(MascotProtein parent, String csvLine)
		{
			this.parent = parent;
			String spl[] = splitCSVLine(csvLine);
			N = spl[0];
			sequence = spl[19];
			mods = translateMods(spl[22], sequence);
			confidence = 100*(1-Math.pow(10, Double.parseDouble(spl[16])/-10));
			mz = spl[10];
			mw = spl[11];
			z = Integer.parseInt(spl[12]);
			theorMW = spl[13];
			double mr = Double.parseDouble(spl[13]);
			theorMz = "" + (mr + z*1.007276)/z;
			spectrum = spl[23];
		}
		String PeptideSummaryLine()
		{
			String proteinStuff = parent.ProteinSummaryLine();
			String spl[] = proteinStuff.split("\t");
			String retVal = "";
			for (int i = 0; i < 8; i++)
				retVal += (i>0?"\t":"") + spl[i];
			retVal += "\t\t\t\t";
			retVal += confidence + "\t" + sequence + "\t" + mods + "\t\t";
			retVal += (Double.parseDouble(mw) - Double.parseDouble(theorMW)) + "\t";
			retVal += mw + "\t" + mz + "\t" + theorMW + "\t" + theorMz + "\t" + z + "\t\t" + spectrum;
			return retVal;
		}
	}
	
	public MascotCSVReader(String fileName) throws Exception
	{		
		String urlString = "http://mascot.ckm.ucsf.edu/cgi/export_dat_2.pl?file=" + fileName + 
						"&do_export=1" + 
						"&prot_hit_num=1" + 
						"&prot_acc=1" + 
						"&pep_query=1" + 
						"&pep_rank=1" + 
						"&pep_isbold=1" + 
						"&pep_exp_mz=1" + 
						"&_showallfromerrortolerant=0" + 
						"&_onlyerrortolerant=0" + 
						"&_noerrortolerant=0" + 
						"&_show_decoy_report=1" + 
						"&export_format=CSV" + 
						"&_sigthreshold=0.05" + 
						"&REPORT=AUTO" + 
						"&_server_mudpit_switch=0.000000001" + 
						"&_ignoreionsscorebelow=0" + 
						"&show_same_sets=1" +
						"&_showsubsets=0" + 
						"&_requireboldred=1" + 
						"&search_master=1" + 
						"&show_header=1" + 
						"&show_mods=1" + 
						"&show_params=1" + 
						"&show_format=1" + 
						"&protein_master=1" + 
						"&prot_score=1" + 
						"&prot_desc=1" + 
						"&prot_mass=1" + 
						"&prot_matches=1" +
						"&prot_seq=1" + 
						"&peptide_master=1" + 
						"&pep_exp_mr=1" + 
						"&pep_exp_z=1" + 
						"&pep_calc_mr=1" + 
						"&pep_delta=1" + 
						"&pep_miss=1" + 
						"&pep_score=1" + 
						"&pep_expect=1" + 
						"&pep_seq=1" + 
						"&pep_var_mod=1" + 
						"&pep_scan_title=1";
				
		URL url = new URL(urlString);
		URLConnection urlConn = url.openConnection();
		urlConn.setDoInput (true);
		urlConn.setDoOutput (false);
		urlConn.setUseCaches (false);		                 
		urlConn.setRequestProperty("Content-Type", "multipart/mixed; boundary=-----------------------------16838575810113");
		urlConn.setConnectTimeout(10000000);
		in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
		while((curLine = in.readLine()) != null && !curLine.startsWith("prot_hit_num,prot_acc"))
		{
			ArrayList<String> aModTable = new ArrayList<String>();

			String spl[] = splitCSVLine(curLine);
			if (spl.length < 2)
				continue;
				
			if (spl[0].equals("Peak list data path"))
				spectrumDataFile = spl[1];
			
			if (spl[0].equals("Database"))
				dbName = spl[1];
				
				if (curLine.startsWith("\"Identifier\",\"Name\",\"Delta\",\"Neutral loss(es)\""))
				{
					curLine = in.readLine();
					spl = splitCSVLine(curLine);
					while (spl[0].matches("[0-9]"))
					{
						aModTable.add(spl[1]);
						curLine = in.readLine();
						spl = splitCSVLine(curLine);
					}
					modsTable = aModTable.toArray(new String[0]);
				}

		}
		curLine = in.readLine();
	}
	
	public static  String[] splitCSVLine(String line)
	{
		ArrayList<String> vals = new ArrayList<String>();
		
		int state = 0; // 0 = outside "...", 1 = inside "..."
		String value = "";
		for (int i = 0; i < line.length(); i++)
		{
			char c = line.charAt(i);
			
			if (state == 0 && c == ',')
			{
				if (line.charAt(i+1) == '"')
				{
					i++;
					state = 1;
				}
				vals.add(value);
				value = "";
			}
			else if (state == 1 && c == '"' && (i == line.length()-1 || line.charAt(i+1) == ','))
			{
				//i++;
				state = 0;
				//vals.add(value);
				//value = "";
			}
			else if (state == 1 || c != ',')
				value += c;
			
		}
		vals.add(value);
		return (String[]) vals.toArray(new String[0]);
	}

	MascotProtein nextProtein() throws Exception
	{
		if (curLine == null)
			return null;
		
		MascotProtein p = new MascotProtein(curLine);
		while(curLine != null)
		{
			p.peptides.add(new MascotPeptide(p, curLine));
			curLine = in.readLine();
			if (curLine != null && !curLine.startsWith(p.N + ",,"))
				break;
		}
		return p;
	}
	static String header = "N\tUnused\tTotal\t%Cov\t%Cov(50)\t%Cov(95)\tAccession\tName\tSpecies\tPeptides(95%)";
	boolean sentHeader = false;
	

	public String readLineProtein() throws Exception
	{
		if (!sentHeader)
		{
			sentHeader = true;
			return header;
		}
		MascotProtein p = nextProtein();
		if (p == null)
			return null;
		return p.ProteinSummaryLine();
	}
	
	String peptideHeader = "N\tUnused\tTotal\t%Cov\t%Cov(50)\t%Cov(95)\tAccessions\tNames\tUsed\tAnnotation\tContrib\tConf\tSequence\tModifications\tCleavages\tdMass\tPrec MW\tPrec m/z\tTheor MW\tTheor m/z\tTheor z\tSc\tSpectrum\tTime\tPrecursorSignal\tPrecursorElution";
	boolean sentPeptideHeader = false;
	
	MascotProtein thisProtein = null;
	
	public String readLinePeptide() throws Exception
	{
		if (!sentPeptideHeader)
		{
			sentPeptideHeader = true;
			thisProtein = nextProtein();
			return peptideHeader;
		}
		for(;;)
		{
			if (thisProtein == null)
				return null;
			if (thisProtein.nextPeptide >= thisProtein.peptides.size())
				thisProtein = nextProtein();
			else
				return thisProtein.peptides.get(thisProtein.nextPeptide++).PeptideSummaryLine();
		}
	}
	public String translateMods(String mascotModInfo, String sequence)
	{
		String ppModInfo = "";
		if (mascotModInfo.length() == 0)
			return ppModInfo;
		char nTermMod = '0';
		if (mascotModInfo.charAt(1) == '.')
		{
			nTermMod = mascotModInfo.charAt(0);
			mascotModInfo = mascotModInfo.substring(2);
		}
		char cTermMod = '0';
		if (mascotModInfo.charAt(mascotModInfo.length()-2) == '.')
		{
			cTermMod = mascotModInfo.charAt(mascotModInfo.length()-1);
			mascotModInfo = mascotModInfo.substring(0, mascotModInfo.length()-2);
		}
		if (nTermMod != '0')
		{
			int iMod = Integer.parseInt("" + nTermMod);
			String thisMod = modsTable[iMod-1];
			if (thisMod.equals("Glu->pyro-Glu (N-term E)"))
			{
				if (ppModInfo.length() > 0)
					ppModInfo += "; ";
				ppModInfo += "Glu->pyro-Glu@N-term";
			}
			else if (thisMod.equals("Acetyl (Protein N-term)"))
			{
				if (ppModInfo.length() > 0)
					ppModInfo += "; ";
				ppModInfo += "Protein Terminal Acetyl";
			}
			else 
				if (ppModInfo.length() > 0)
					ppModInfo += "; ";
				ppModInfo += thisMod;
		}
		for (int i = 0; i < mascotModInfo.length(); i++)
		{
			if (i >= sequence.length())
			{
				int x = 1;
				x++;
			}
			char residue = sequence.charAt(i);
			if (residue == 'C')
			{
				if (ppModInfo.length() > 0)
					ppModInfo += "; ";
				ppModInfo += "Carbamidomethyl(C)@" + (i+1);
			}	
			int iMod = Integer.parseInt(mascotModInfo.substring(i, i+1));
			if (iMod == 0)
				continue;
			
			String thisMod = modsTable[iMod-1];
			if (thisMod.equals("Deamidated (NQ)") || thisMod.equals("Deamidation (NQ)"))
			{
				if (ppModInfo.length() > 0)
					ppModInfo += "; ";
				ppModInfo += "Deamidated(" + residue + ")@" + (i+1);
			}
			if (thisMod.matches("Oxidation \\([KMPW]\\)"))
			{
				if (ppModInfo.length() > 0)
					ppModInfo += "; ";
				ppModInfo += "Oxidation(" + residue + ")@" + (i+1);
			}
			if (thisMod.equals("Glu->pyro-Glu (N-term E)"))
			{
				if (ppModInfo.length() > 0)
					ppModInfo += "; ";
				ppModInfo += "Glu->pyro-Glu@N-term";
			}
		}
		return ppModInfo;
	}

	public static void main(String args[]) throws Exception
	{
		String fileName = "20111106/F3861828.dat";
		MascotCSVReader r = new MascotCSVReader(fileName);
		String line;
		while((line = r.readLinePeptide()) != null)
			System.out.println(line);
	}
}
