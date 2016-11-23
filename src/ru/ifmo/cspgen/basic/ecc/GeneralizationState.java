package ru.ifmo.cspgen.basic.ecc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GeneralizationState extends State {

	public GeneralizationState() {
		super();
	}
	
	public GeneralizationState(State other) {
		super(other);
	}
	
	public int getNewState(String inputEvent, String variableValues) {
        int eventIndex = MultiMaskEfsmSkeleton.INPUT_EVENTS.get(inputEvent);
        if (transitionGroups[eventIndex] == null) {
            return -1;
        }
        for (TransitionGroup g : transitionGroups[eventIndex]) {
            if (g == null) {
                continue;
            }
            if (!g.hasTransitions()) {
                continue;
            }
            List<Integer> meaningfulPredicateIds = g.getMeaningfulPredicateIds();
            StringBuilder meaningfulPredicateValues = new StringBuilder();
            for (Integer i : meaningfulPredicateIds) {
                meaningfulPredicateValues.append(variableValues.charAt(i));
            }
            int transitionIndex = Integer.parseInt(meaningfulPredicateValues.toString(), 2);
            int newState = g.getNewState(transitionIndex);
            if (newState == -1) {
                continue;
            }

//            g.setTransitionUsed(transitionIndex, true);
            return newState;
        }
        return -1;
    }
	
	public void markTransitionUsed(String inputEvent, String variableValues) {
        int eventIndex = MultiMaskEfsmSkeleton.INPUT_EVENTS.get(inputEvent);
        for (TransitionGroup g : transitionGroups[eventIndex]) {
            if (g == null) {
                continue;
            }
            if (!g.hasTransitions()) {
                continue;
            }
            List<Integer> meaningfulPredicateIds = g.getMeaningfulPredicateIds();
            StringBuilder meaningfulPredicateValues = new StringBuilder();
            for (Integer i : meaningfulPredicateIds) {
                meaningfulPredicateValues.append(variableValues.charAt(i));
            }
            int transitionIndex = Integer.parseInt(meaningfulPredicateValues.toString(), 2);
            int newState = g.getNewState(transitionIndex);
            if (newState == -1) {
                continue;
            }

            g.setTransitionUsed(transitionIndex, true);
        }
    }
	
	public void shuffleTransitionGroups() {
		for (int i = 0; i < transitionGroups.length; i++) {
			List<TransitionGroup> tmp = new ArrayList<TransitionGroup>();
			for (int j = 0; j < transitionGroups[i].length; j++) {
				tmp.add(transitionGroups[i][j]);
			}
			Collections.shuffle(tmp, ThreadLocalRandom.current());
			for (int j = 0; j < transitionGroups[i].length; j++) {
				transitionGroups[i][j] = tmp.get(j);
			}
		} 
	}
	
	public int getTransitionCoverageFrequency() {
		int result = 0;
		for (int eventIndex = 0; eventIndex < MultiMaskEfsmSkeleton.INPUT_EVENT_COUNT; eventIndex++) {
			for (TransitionGroup g : transitionGroups[eventIndex]) {
				result += g.getTotalTransitionCoverageFrequency();
			}
		}
		return result;
	}
 }
