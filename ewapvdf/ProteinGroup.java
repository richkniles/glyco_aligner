package edu.ucsf.library.sprot.ewapvdf;

import java.util.*;
import JSci.maths.statistics.*;

public class ProteinGroup 
{
	ArrayList<Protein> proteins = new ArrayList<Protein>();
	ArrayList<Peptide> peptides = new ArrayList<Peptide>();
	
	public void add(Protein p)
	{
		proteins.add(p);
	}
	public void add(Peptide p)
	{
		if (p.getDouble("Conf") < 99)
		{
			int x = 1;
			x++;
		}
		peptides.add(p);
	}
	public Protein findProteinWithQuants() throws Exception
	{
		Protein ret = null;
		for (Protein p : proteins)
		{
			if (p.hasQuants())
			{
				if (ret != null)
					throw new Exception(p.getString("Accession") + " has more than one protein with quants");
				ret = p;
				return ret;
			}
		}
		if (ret == null)
			ret = proteins.get(0);
		return ret;
	}
	
	public int countPeptidesWithQuants()
	{
		int count = 0;
		for (Peptide pep : peptides)
			if (pep.hasQuants())
				count++;
		return count;
	}
	class RatioStats
	{
		double ratio;
		double S;
		double b;
		int n;
	}
	HashMap<String, RatioStats> ratioStats = new HashMap<String, RatioStats>();
	
	static String ratioList = null;
	public static boolean isRatio(String s)
	{
		if (ratioList == null)
		for (int i = 113; i <= 121; i++)
			for (int j = 113; j <= 121; j++)
				if (j != i && j != 120 && i != 120)
					ratioList += "" + i + ":" + j + ",";
		return ratioList.contains(s);
	}
	public String compute(String field, TreeSet<String> peptidesForQuant)
	{
		if (isRatio(field))
		{
			double ratio = computeRatio(field, peptidesForQuant);
			if (Double.isNaN(ratio))
				return "";
			return "" + Math.round(ratio * 100)/100.;
		}
		else if (field.startsWith("EF ") && isRatio(field.substring(3)))
			return "" + computeErrorFactor(field.substring(3), peptidesForQuant);
		else if (field.startsWith("pVal ") && isRatio(field.substring(5)))
			return "" + computePValue(field.substring(5), peptidesForQuant);
		else
		{
			System.out.println("Unimplemented computation: " + field);
			return "";
		}
	}
	public double computeRatio(String numColonDenom)
	{
		return computeRatio(numColonDenom, null);
	}
	public double computeRatio(String numColonDenom, TreeSet<String> useOnlyPeptides)
	{
		// check the cache to see if we already computed this
		RatioStats stats = ratioStats.get(numColonDenom);
		if (stats != null)
			return stats.ratio;
		
		// get peptide ratios and pctErrors
		TreeSet<String> peptideSeqs = new TreeSet<String>();
		double ratios[] = new double[peptides.size()];
		double pctErrs[] = new double[peptides.size()];
		for (int i = 0; i < peptides.size(); i++)
		{
			Peptide pep = peptides.get(i);
			ratios[i] = -1e15;
			String pepSeqMods = pep.getString("Sequence") + ":" + pep.getString("Modifications");
			if (!pep.getString(numColonDenom).isEmpty() && (useOnlyPeptides == null || useOnlyPeptides.contains(pepSeqMods)))
			{
				double rawVal = pep.getDouble(numColonDenom);
				if (rawVal <= 0.01 || rawVal >= 99)
					continue;
//					rawVal = .00005;
				ratios[i] = Math.log10(rawVal);
				peptideSeqs.add(pepSeqMods);
			}
			pctErrs[i] = 100;
			if (!pep.getString("%Err " + numColonDenom).isEmpty())
				pctErrs[i] = pep.getDouble("%Err " + numColonDenom);
		}
		double wSumRatios = 0,
			   sumWeights = 0,
			   sumWeightsSq = 0,
			   sumRatioSq = 0,
			   sumRatios = 0,
			   Xavg = 0;
		int N = 0,
			n = peptideSeqs.size();
		
		for (int i = 0; i < ratios.length; i++)
		{
			if (pctErrs[i] <= 0 || ratios[i] <=-1e14)
				continue;
			double weight = 1/pctErrs[i];
			wSumRatios += weight * ratios[i];
			sumWeights += weight;
			sumWeightsSq += weight*weight;
			sumRatios += ratios[i];
			sumRatioSq += ratios[i]*ratios[i];
			N++;
		}
		stats = new RatioStats();
 		stats.ratio = Math.pow(10, wSumRatios/sumWeights);
		Xavg = sumRatios/N;
		stats.S = Math.sqrt((sumRatioSq - N*Xavg*Xavg)/(n-1));
		stats.b = sumWeights*sumWeights/sumWeightsSq;
		stats.n = n;// try using all peptides for n
		stats.n = N;
		ratioStats.put(numColonDenom, stats);
		return stats.ratio;
	}
	public double computePValue(String numColonDenom)
	{
		return computePValue(numColonDenom, null);
	}
	public double computePValue(String numColonDenom, TreeSet<String> useOnlyPeptides)
	{
		computeRatio(numColonDenom, useOnlyPeptides);
		RatioStats stats = ratioStats.get(numColonDenom);
		double Smw = stats.S/Math.sqrt(stats.b);
		double t = Math.log10(stats.ratio) / Smw;
		if (t > 0)
			t = -t;
		TDistribution tdist = new TDistribution(stats.n - 1);
		double p = tdist.cumulative(t)*2;
		return p;
	}
	public double computeErrorFactor(String numColonDenom)
	{
		return computeErrorFactor(numColonDenom, null);
	}
	public double computeErrorFactor(String numColonDenom, TreeSet<String> useOnlyPeptides)
	{
		computeRatio(numColonDenom, useOnlyPeptides);
		RatioStats stats = ratioStats.get(numColonDenom);
		if (stats.n <= 1)
			return -1;
		TDistribution tdist = new TDistribution(stats.n - 1);
		double Smw = stats.S/Math.sqrt(stats.b);
		double conf95 = Smw * tdist.inverse(0.975);
		return Math.pow(10,	conf95);
	}
	public double computeLowerCI(String numColonDenom)
	{
		return computeLowerCI(numColonDenom, null);
	}
	public double computeLowerCI(String numColonDenom, TreeSet<String> useOnlyPeptides)
	{
		double ratio = computeRatio(numColonDenom, useOnlyPeptides);
		return ratio/computeErrorFactor(numColonDenom, useOnlyPeptides);
	}
	public double computeUpperCI(String numColonDenom)
	{
		return computeUpperCI(numColonDenom, null);
	}
	public double computeUpperCI(String numColonDenom, TreeSet<String> useOnlyPeptides)
	{
		double ratio = computeRatio(numColonDenom, useOnlyPeptides);
		return ratio*computeErrorFactor(numColonDenom, useOnlyPeptides);
	}
	public TreeSet<Protein> winnerSet()
	{
		TreeSet<Protein> ret = new TreeSet<Protein>(MasterListGroup.comp);
		double maxTotalScore = 0;
		for (Protein p : proteins)
			maxTotalScore = Math.max(maxTotalScore, p.getDouble("Total"));
		for (Protein p : proteins)
			if (p.getDouble("Total") == maxTotalScore)
				ret.add(p);
		return ret;
	}
}
