package edu.ucsf.library.sprot.ewapvdf;

import java.util.*;

public class ProteinGroupMasterList 
{
	public boolean collapseWinners = false;
	
	public TreeSet<MasterListGroup> groups = new TreeSet<MasterListGroup>(comp);
	
	public static Comparator<MasterListGroup> comp = new Comparator<MasterListGroup>()
	{
		public int compare(MasterListGroup a, MasterListGroup b)
		{
			//if (a.accessionSet().equals(b.accessionSet()))
			//	return 0;
			return a.accessionString().compareTo(b.accessionString());
		}
	};
	public void add(ProteinGroup pg)
	{
		MasterListGroup mg = new MasterListGroup(pg);
		if (!collapseWinners)
		{
			groups.add(mg);
			MasterListGroup xmg = groups.tailSet(mg).first();
			xmg.peptides.addAll(mg.peptides);
		}
		else
		{
			boolean hadItAlready = false;
			for (MasterListGroup xg : groups)
			{
				TreeSet<Protein> intersect = mg.winnerSet();
				intersect.retainAll(xg.winnerSet());
				if (!intersect.isEmpty()) // common winner, collapse
				{
					hadItAlready = true;
					TreeSet<String> xgProteins = xg.accessionSet();
					TreeSet<String> mgProteins = mg.accessionSet();
					xgProteins.retainAll(mgProteins);
					int i = 0;
					while (i < xg.proteins.size())
					{
						Protein xp = xg.proteins.get(i);
						if (!xgProteins.contains(xp.getString("Accession")))
							xg.proteins.remove(i);
						else
							i++;
					}
				}
			}
			if (!hadItAlready)
				groups.add(mg);
		}
	}
	public void add(MasterListGroup mg)
	{
		for (MasterListGroup xmg : groups)
		{
			if (xmg.proteins.containsAll(mg.proteins))
			{
				groups.remove(xmg);
				groups.add(mg);
				mg.peptides.addAll(xmg.peptides);
				return;
			}
			if (mg.proteins.containsAll(xmg.proteins))
			{
				xmg.peptides.addAll(mg.peptides);
				return;
			}
		}
		groups.add(mg);
	}
	
}
