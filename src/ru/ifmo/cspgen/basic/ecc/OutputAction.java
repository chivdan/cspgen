package ru.ifmo.cspgen.basic.ecc;

public class OutputAction {
    private OutputAlgorithm algorithm;
    private String outputEvent;

    public OutputAction(OutputAlgorithm algorithm, String outputEvent) {
        this.algorithm = algorithm;
        this.outputEvent = outputEvent;
    }

    public OutputAction(OutputAction other) {
        this.algorithm = other.algorithm.copyOf();
        this.outputEvent = other.outputEvent;
    }

    public OutputAlgorithm getAlgorithm() {
        return algorithm;
    }

    public String getOutputEvent() {
        return outputEvent;
    }
    
    public boolean isDoNothing() {
    	if (!getOutputEvent().isEmpty()) {
    		return false;
    	}
    	
    	return algorithm.isDoNothing();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OutputAction)) {
            return false;
        }

        OutputAction other = (OutputAction) obj;
        return algorithm.equals(other.algorithm) && outputEvent.equals(other.outputEvent);
    }

    @Override
    public int hashCode() {
        return (algorithm + outputEvent).hashCode();
    }

    @Override
    public String toString() {
        return outputEvent + "[" + algorithm + "]";
    }
}