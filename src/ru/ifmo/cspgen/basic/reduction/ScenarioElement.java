package ru.ifmo.cspgen.basic.reduction;

import java.util.ArrayList;
import java.util.List;

import ru.ifmo.cspgen.basic.ecc.OutputAction;
import ru.ifmo.cspgen.basic.ecc.OutputAlgorithm;
import ru.ifmo.cspgen.basic.ecc.TernaryOutputAlgorithm;

public class ScenarioElement {
    private String inputEvent;
    private String variableValues;
    private List<OutputAction> outputActions;

    public ScenarioElement(String inputEvent, String variableValues, List<OutputAction> outputActions) {
        this.inputEvent = inputEvent;
        this.variableValues = variableValues;
        this.outputActions = new ArrayList<OutputAction>();
        this.outputActions.addAll(outputActions);
    }

    public ScenarioElement(String inputEvent, String variableValues, OutputAction outputAction) {
        this.inputEvent = inputEvent;
        this.variableValues = variableValues;
        this.outputActions = new ArrayList<OutputAction>();
        this.outputActions.add(outputAction);
    }
    
    public int getActionsCount() {
    	return outputActions.size();
    }
    
    public String getAlgorithm(int i) {
    	return outputActions.get(i).getAlgorithm().toString();
    }
    
    public String getLastAlgorithm() {
    	return outputActions.get(outputActions.size() - 1).getAlgorithm().toString();
    }
    
    public String getOutputEvent(int i) {
    	return outputActions.get(i).getOutputEvent();
    }
    

    public String getInputEvent() {
        return inputEvent;
    }

    public String getInputVariableValues() {
        return variableValues;
    }
    
    public boolean[] getVariableValuesArray() {
    	boolean[] result = new boolean[variableValues.length()];
    	for (int i = 0; i < variableValues.length(); i++) {
    		result[i] = (variableValues.charAt(i) == '1' ? true : false);
    	}
    	return result;
    }
    
    public OutputAction getAction(int i) {
    	return outputActions.get(i);
    }
    
    public OutputAction getLastAction() {
    	return outputActions.get(outputActions.size() - 1);
    }
    
    public String getMeaningfulVariableValues(boolean mask[]) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mask.length; i++) {
            if (mask[i]) {
                sb.append(variableValues.charAt(i));
            }
        }
        return sb.toString();
    }

    public List<OutputAction> getActions() {
    	return outputActions;
    }
    
    public String getStringOutput() {
    	String result = "";
    	for (OutputAction a : outputActions) {
    		result += a.getAlgorithm() + a.getOutputEvent();
    	}
    	return result;
    }
    
    public List<String> getOutput() {
    	List<String> result = new ArrayList<String>();
    	for (OutputAction a : outputActions) {
    		result.add(a.getAlgorithm() + a.getOutputEvent());
    	}
    	return result;
    }

    public void addActions(String actions, String outputEvent) {
        this.outputActions.add(new OutputAction(new TernaryOutputAlgorithm(actions), outputEvent));
    }
    
    public void addToInputs(OutputAlgorithm algorithm) {
    	variableValues = variableValues + algorithm.toString();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(inputEvent).append("[").append(variableValues).append("]; ");
        sb.append(outputActions).append(">");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ScenarioElement)) {
            return false;
        }
        ScenarioElement other = (ScenarioElement) obj;
        if (!variableValues.equals(other.variableValues) || outputActions.size() != other.outputActions.size() ||
                !(inputEvent.equals(other.inputEvent))) {
            return false;
        }
        
        for (int i = 0; i < outputActions.size(); i++) {
        	if (!outputActions.get(i).equals(other.outputActions.get(i))) {
        		return false;
        	}
        }

        return true;
    }

    @Override
    public int hashCode() {
        return (inputEvent + variableValues).hashCode() + outputActions.hashCode() * 17;
    }  
}
