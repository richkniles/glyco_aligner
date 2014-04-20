package edu.ucsf.library.sprot.ewapvdf;

import java.util.HashMap;

public class TryHash {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		HashMap<String,String> x = new HashMap<String,String>();
		
		x.put("LIRB1_HUMAN", "1");
		x.put("LIRA1_HUMAN","2");
		System.out.println(x.get("LIRB1_HUMAN"));
		System.out.println(x.get("LIRA1_HUMAN"));
	}

}
