package ru.ifmo.cspgen.basic.ecc;

public class DoubleBinaryOutputAlgorithm extends OutputAlgorithm {

	private String zeroAlgorithm;
	private String oneAlgorithm;
	
	public DoubleBinaryOutputAlgorithm(String zeroAlgorithm, String oneAlgorithm) {
		this.zeroAlgorithm = zeroAlgorithm;
		this.oneAlgorithm = oneAlgorithm;
	}

	public DoubleBinaryOutputAlgorithm(DoubleBinaryOutputAlgorithm other) {
		this.zeroAlgorithm = other.zeroAlgorithm;
		this.oneAlgorithm = other.oneAlgorithm;
	}
	
	public String getZeroAlgorithm() {
		return zeroAlgorithm;
	}
	
	public String getOneAlgorithm() {
		return oneAlgorithm;
	}
	
	@Override
	public boolean isDoNothing() {
		return !zeroAlgorithm.contains("1") && !oneAlgorithm.contains("0");
	}

	@Override
	public String apply(String currentActions) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < currentActions.length(); i++) {
			sb.append(currentActions.charAt(i) == '0' ? zeroAlgorithm.charAt(i) : oneAlgorithm.charAt(i));
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return zeroAlgorithm + "_" + oneAlgorithm;
	}
	
	@Override
	public int hashCode() {
		return (zeroAlgorithm + oneAlgorithm).hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof DoubleBinaryOutputAlgorithm)) {
			return false;
		}
		DoubleBinaryOutputAlgorithm other = (DoubleBinaryOutputAlgorithm)obj;
		
		return zeroAlgorithm.equals(other.zeroAlgorithm) && oneAlgorithm.equals(other.oneAlgorithm);
	}
	
	
	@Override
	public String toFbtString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<ST Text=\" ");
		sb.append("REQ := FALSE;&#xD;&#xA;");
		for (int i = 0; i < zeroAlgorithm.length(); i++) {
			String var = MultiMaskEfsmSkeleton.OUTPUT_VARIABLES[i];
			String ifNotVarAction = zeroAlgorithm.charAt(i) == '1' ? "TRUE" : "FALSE";
			String ifVarAction = oneAlgorithm.charAt(i) == '1' ? "TRUE" : "FALSE";
			
			if (ifVarAction.equals("FALSE")) {
				if (ifNotVarAction.equals("FALSE")) {
					sb.append("IF " + var + " THEN&#xD;&#xA;" + var + " := " + ifVarAction + ";&#10;END_IF;&#xD;&#xA;");
				} else {
					sb.append("IF " + var + " THEN&#xD;&#xA;" + var + " := " + ifVarAction + ";&#10;ELSE&#10;" + var + " := " + ifNotVarAction + ";&#xD;&#xA;END_IF;&#xD;&#xA;");
				}
			} else {
				if (ifNotVarAction.equals("TRUE")) {
					sb.append("IF NOT " + var + " THEN&#xD;&#xA;" + var + " := " + ifNotVarAction + ";&#xD;&#xA;END_IF;&#xD;&#xA;");
				} 
			}
		}
		sb.append("\"/>");
		return sb.toString();
	}
	
@Override
	public void generalizeElement(int i) {
		char[] c0 = zeroAlgorithm.toCharArray();
		c0[i] = '0';
		zeroAlgorithm = String.valueOf(c0);
		
		char[] c1 = oneAlgorithm.toCharArray();
		c1[i] = '1';
		oneAlgorithm = String.valueOf(c1);
	}

	@Override
	public boolean isGeneral(int i) {
		return zeroAlgorithm.charAt(i) == '0' & oneAlgorithm.charAt(i) == '1';
	}
	
	@Override
	public OutputAlgorithm copyOf() {
		return new DoubleBinaryOutputAlgorithm(this);
	}
}	
