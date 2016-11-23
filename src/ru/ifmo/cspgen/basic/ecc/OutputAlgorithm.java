package ru.ifmo.cspgen.basic.ecc;

public abstract class OutputAlgorithm {
	public abstract boolean isDoNothing();
	public abstract String apply(String currentActions);
	public abstract String toFbtString();
	public abstract void generalizeElement(int i);
	public abstract boolean isGeneral(int i);
	public abstract OutputAlgorithm copyOf();
}
