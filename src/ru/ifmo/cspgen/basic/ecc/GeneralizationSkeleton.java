package ru.ifmo.cspgen.basic.ecc;

import java.util.List;

import ru.ifmo.util.StringUtils;

public class GeneralizationSkeleton extends MultiMaskEfsmSkeleton {
	
	 public GeneralizationSkeleton() {
		 states = new GeneralizationState[STATE_COUNT];
	 }	
	 
	 public GeneralizationSkeleton(MultiMaskEfsmSkeleton other) {
		 super(other);
		 states = new GeneralizationState[other.states.length];
		 for (int i = 0; i < states.length; i++) {
			 states[i] = new GeneralizationState(other.states[i]);
		 }
		 fitness = other.fitness;
//		 counterExamples.addAll(other.counterExamples);
	 }
	 
	 public GeneralizationSkeleton(State[] states) {
		 this.states = states;
	 }
	 
	 @Override
	 public int getNewState(int state, String inputEvent, String variableValues) {
		 return states[state].getNewState(inputEvent, variableValues);
	 }
	 
	 public String tranIdToFbtLabel(int tranId, List<Integer> meaningfulPredicates) {
		 String f = StringUtils.toAugmentedBinaryString(tranId, meaningfulPredicates.size());
		 StringBuilder formula = new StringBuilder();	
		 for (int i = 0; i < f.length(); i++) {
			 if (f.charAt(i) == '0') {
				 formula.append("NOT ");
			 }
			 formula.append(MultiMaskEfsmSkeleton.PREDICATE_NAMES.get(meaningfulPredicates.get(i)));
			 if (i < f.length() - 1) {
				 formula.append(" AND ");
			 }
		 }
		 return formula.toString();
	 }
	 
	 public double getMeanTransitionCoverageFrequency() {
		 double result = 0;
		 for (State state : states) {
			 result += ((GeneralizationState)state).getTransitionCoverageFrequency();
		 }
		 return result / getDefinedTransitionsCount();
	 }
	 
	 public boolean[][][] getUsedTransitions() {
		 boolean[][][] result = new boolean[states.length][MultiMaskEfsmSkeleton.INPUT_EVENT_COUNT][MultiMaskEfsmSkeleton.OCCURRING_INPUTS.size()];

		 //not needed

		 return result;
	 }
}
