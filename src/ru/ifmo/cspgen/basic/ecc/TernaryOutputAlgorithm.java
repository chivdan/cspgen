package ru.ifmo.cspgen.basic.ecc;

public class TernaryOutputAlgorithm extends OutputAlgorithm {

	private String algorithm;
	
	public TernaryOutputAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	public TernaryOutputAlgorithm(TernaryOutputAlgorithm other) {
		algorithm = other.algorithm;
	}
	
	@Override
	public boolean isDoNothing() {
		for (char c : algorithm.toCharArray()) {
    		if (c != 'x') {
    			return false;
    		}
    	}
    	return true;
	}
	
	@Override
	public String apply(String currentActions) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < currentActions.length(); i++) {
			result.append(algorithm.charAt(i) == 'x' ? currentActions.charAt(i) : algorithm.charAt(i));
		}
		return result.toString();
	}
	
	public char get(int i) {
		return algorithm.charAt(i);
	}
 	
	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof TernaryOutputAlgorithm)) {
			return false;
		}
		
		TernaryOutputAlgorithm other = (TernaryOutputAlgorithm)obj;
		return algorithm.equals(other.algorithm);
	}

	@Override
    public int hashCode() {
        return algorithm.hashCode();
    }
	
	@Override
	public String toString() {
		return algorithm;
	}
	
	@Override
	public String toFbtString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<ST Text=\" ");
		for (int i = 0; i < algorithm.length(); i++) {
			if (algorithm.charAt(i) == 'x') {
				continue;
			}
			String value = algorithm.charAt(i) == '0' ? "FALSE" : "TRUE";
			sb.append(MultiMaskEfsmSkeleton.OUTPUT_VARIABLES[i] + " := " + value +";&#10; ");
			
		}
		sb.append("\"/>");
		return sb.toString();
	}
	
	@Override
	public void generalizeElement(int i) {
		char[] c = algorithm.toCharArray();
		c[i] = 'x';
		algorithm = String.valueOf(c);
	}
	@Override
	public boolean isGeneral(int i) {
		return algorithm.charAt(i) == 'x';
	}
	
	@Override
	public OutputAlgorithm copyOf() {
		return new TernaryOutputAlgorithm(this);
	}
}
