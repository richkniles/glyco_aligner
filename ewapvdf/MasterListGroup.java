package edu.ucsf.library.sprot.ewapvdf;

import java.util.*;;

public class MasterListGroup 
{
	ArrayList<Protein> proteins;
	TreeSet<String> peptides;
	
	public static Comparator<Protein> comp = new Comparator<Protein>()
	{
		public int compare(Protein a, Protein b)
		{
			return a.getString("Accession").compareTo(b.getString("Accession"));
		}
	};
	
	public MasterListGroup(ProteinGroup pg)
	{
		proteins = new ArrayList<Protein>();
		proteins.addAll(pg.proteins);
		
		peptides = new TreeSet<String>();
		for (Peptide p : pg.peptides)
			peptides.add(p.getString("Sequence") + ":" + p.getString("Modifications"));
	}
	
	public String accessionString()
	{
		return accessionString("Accession");
	}
	public String accessionString(String field)
	{
		String ret = "";
		TreeSet<String> accession_set = accessionSet();
		for (String acc : accession_set)
			ret += acc;
		
//		for (Protein p : proteins)
//		{
//			if (!ret.isEmpty())
//				ret += ";";
//			ret += p.getString(field);
//		}
		
		return ret;
	}
	public TreeSet<String> accessionSet()
	{
		TreeSet<String> ret = new TreeSet<String>();
		for (Protein p : proteins)
		{
			ret.add(p.getString("Accession"));
		}
		return ret;
	}

	public int getDataType(String field) {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getString(String field) 
	{
		if (field.equals("SP Accession") || field.equals("Entry Name") || field.equals("Accession"))
			return accessionString(field);
		else 
			return proteins.get(0).getString(field);
		
	}
	public String getStringConcatenateDistinct(String field)
	{
		String ret = "";
		for (Protein p : proteins)
		{
			String thisVal = p.getString(field);
			if (ret.contains(thisVal))
				continue;
			if (!ret.isEmpty())
				ret += ";";
			ret += p.getString(field);
		}
		return ret;
	}
	public double getDouble(String field, String combiningFunction)
	{
		double[] vals = new double[proteins.size()];
		int i = 0;
		for (Protein p : proteins)
			vals[i++] = p.getDouble(field);
		Arrays.sort(vals);
		
		String f = combiningFunction.toLowerCase();
		if (f.equals("max"))
			return vals[vals.length-1];
		if (f.equals("min"))
			return vals[0];
		if (f.equals("average") || f.equals("avg") || f.equals("sum"))
		{
			double s = 0;
			for (double d : vals)
				s += d;
			if (f.equals("sum"))
				return s;
			return s/vals.length;
		}
		if (f.equals("median"))
		{
			if (vals.length % 2 == 1)
				return vals[vals.length/2];
			if (vals.length > 0)
				return (vals[vals.length/2-1] + vals[vals.length/2])/2;
		}
		return Double.NaN;
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
