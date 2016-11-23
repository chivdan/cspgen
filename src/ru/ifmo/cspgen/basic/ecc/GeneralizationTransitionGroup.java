package ru.ifmo.cspgen.basic.ecc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GeneralizationTransitionGroup extends TransitionGroup {
	
	public GeneralizationTransitionGroup(int meaningfulPredicatesCount) {
		super(meaningfulPredicatesCount);
		mask = new boolean[MultiMaskEfsmSkeleton.PREDICATE_COUNT];
        Arrays.fill(mask, false);
        newState = new int[(int) Math.pow(2, meaningfulPredicatesCount)];
        Arrays.fill(newState, -1);
        transitionUsed = new boolean[newState.length];
        Arrays.fill(transitionUsed, false);
        transitionUseNumber = new int[newState.length];
        Arrays.fill(transitionUseNumber, 0);
	}
	
	public GeneralizationTransitionGroup(TransitionGroup other) {
		super(other);
        if (other != null) {
            this.mask = Arrays.copyOf(other.mask, other.mask.length);
            this.newState = Arrays.copyOf(other.newState, other.newState.length);            
        } else {
            mask = new boolean[MultiMaskEfsmSkeleton.PREDICATE_COUNT];
            Arrays.fill(mask, false);
            newState = new int[(int) Math.pow(2, MultiMaskEfsmSkeleton.MEANINGFUL_PREDICATES_COUNT)];
            Arrays.fill(newState, -1);
        }
        transitionUsed = new boolean[newState.length];
        Arrays.fill(transitionUsed, false);
        transitionUseNumber = new int[newState.length];
        Arrays.fill(transitionUseNumber, 0);
    }
	
	@Override
	public Set<Integer> getUsedTransitionsIds() {
		Set<Integer> result = new HashSet<Integer>();
		for (int id = 0; id < transitionUsed.length; id++) {
			if (transitionUsed[id]) {
				result.add(id);
			}
		}
		return result;
	}
	
	@Override
	public void setTransitionUsed(int transitionIndex, boolean used) {
		super.setTransitionUsed(transitionIndex, used);
		if (used) {
			transitionUseNumber[transitionIndex]++;
		}
	}
	
	public boolean equals(GeneralizationTransitionGroup other) {
		for (int i = 0; i < newState.length; i++) {
			if (newState[i] != other.newState[i]) {
				return false;
			}
		}
		return true;
	}
}
