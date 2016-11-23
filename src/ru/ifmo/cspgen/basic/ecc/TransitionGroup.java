package ru.ifmo.cspgen.basic.ecc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TransitionGroup {
  protected boolean mask[];
  protected int[] newState;
  protected boolean[] transitionUsed;
  protected int transitionUseNumber[];

  public TransitionGroup(int meaningfulPredicatesCount) {
    mask = new boolean[MultiMaskEfsmSkeleton.PREDICATE_COUNT];
    Arrays.fill(mask, false);
    newState = new int[MultiMaskEfsmSkeleton.OCCURRING_INPUTS.size()];
    Arrays.fill(newState, -1);
    transitionUsed = new boolean[newState.length];
    Arrays.fill(transitionUsed, false);
  }

  public TransitionGroup(TransitionGroup other) {
    if (other != null) {
      this.mask = Arrays.copyOf(other.mask, other.mask.length);
      this.newState = Arrays.copyOf(other.newState, other.newState.length);            
    } else {
      mask = new boolean[MultiMaskEfsmSkeleton.PREDICATE_COUNT];
      Arrays.fill(mask, false);
      newState = new int[MultiMaskEfsmSkeleton.OCCURRING_INPUTS.size()];
      Arrays.fill(newState, -1);
    }
    transitionUsed = new boolean[newState.length];
    Arrays.fill(transitionUsed, false);
  }

  public void markTransitionsUnused() {
    Arrays.fill(transitionUsed, false);
  }

  public void markTransitionsUsed() {
    Arrays.fill(transitionUsed, true);
  }

  public void setPredicateMeaningful(int i, boolean isMeaningful) {
    mask[i] = isMeaningful;
  }

  public void setTransitionUsed(int transitionIndex, boolean used) {
    transitionUsed[transitionIndex] = used;
  }

  public boolean isTransitionUsed(int transitionIndex) {
    return transitionUsed[transitionIndex];
  }

  public List<Integer> getMeaningfulPredicateIds() {
    List<Integer> result = new ArrayList<Integer>();
    for (int i = 0; i < mask.length; i++) {
      if (mask[i]) {
        result.add(i);
      }
    }
    return result;
  }

  public List<Integer> getUnmeaningfulPredicateIds() {
    List<Integer> result = new ArrayList<Integer>();
    for (int i = 0; i < mask.length; i++) {
      if (!mask[i]) {
        result.add(i);
      }
    }
    return result;
  }

  public int getNewState(int transitionIndex) {
    return newState[transitionIndex];
  }

  public void setMaskElement(int i, boolean value) {
    mask[i] = value;
  }

  public void setMaskTrue() {
    Arrays.fill(mask, true);
  }

  public void setNewState(int transitionIndex, int newState) {
    this.newState[transitionIndex] = newState;
  }

  public int getTransitionsCount() {
    return newState.length;
  }

  public boolean isTransitionDefined(int transitionIndex) {
    return newState[transitionIndex] != -1;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (boolean v : mask) {
      sb.append(v ? 1 : 0);
    }
    for (int v : newState) {
      sb.append(v);
    }
    return sb.toString();
  }

  public boolean hasUndefinedTransitions() {
    for (int v : newState) {
      if (v == -1) {
        return true;
      }
    }
    return false;
  }

  public boolean hasTransitions() {
    for (int v : newState) {
      if (v != -1) {
        return true;
      }
    }
    return false;
  }

  public List<Integer> getUndefinedTransitionIds() {
    List<Integer> result = new ArrayList<Integer>();
    for (int i = 0; i < newState.length; i++) {
      if (newState[i] == -1) {
        result.add(i);
      }
    }
    return result;
  }

  public List<Integer> getDefinedTransitionIds() {
    List<Integer> result = new ArrayList<Integer>();
    for (int i = 0; i < newState.length; i++) {
      if (newState[i] != -1) {
        result.add(i);
      }
    }
    return result;
  }

  public int getUsedTransitionsCount() {
    int result = 0;
    for (boolean b : transitionUsed) {
      if (b) {
        result++;
      }
    }
    return result;
  }

  public Set<Integer> getUsedTransitionsIds() {
    Set<Integer> result = new HashSet<Integer>();
    for (int id = 0; id < transitionUsed.length; id++) {
      if (transitionUsed[id]) {
        //            	 result.add(id);
        result.add(MultiMaskEfsmSkeleton.OCCURRING_INPUTS.get(id));
      }
    }
    return result;
  }

  public boolean[] getUsedTransitions() {
    return transitionUsed;
  }

  public void setUsedTransitions(boolean[] usedTransitions) {
    transitionUsed = Arrays.copyOf(usedTransitions, usedTransitions.length);
  }

  public int getDefinedTransitionsCount() {
    return getDefinedTransitionIds().size();
  }

  public int getMeaningfulPredicatesCount() {
    return getMeaningfulPredicateIds().size();
  }

  public int getUnmeaningfulPredicatesCount() {
    return getUnmeaningfulPredicateIds().size();
  }

  public void removePredicate(int predicateId) {
    int relativePredicateId = getMeaningfulPredicateIds().indexOf(predicateId);

    int[] changedNewState = new int[(int) (newState.length / 2)];
    int newStateCounter = 0;
    boolean[] visited = new boolean[newState.length];
    Arrays.fill(visited, false);

    for (int i = 0; i < newState.length; i++) {
      if (visited[i]) {
        continue;
      }
      visited[i] = true;
      int pairedTransitionId = i + (int) Math.pow(2, getMeaningfulPredicatesCount() - relativePredicateId - 1);
      if (pairedTransitionId >= newState.length) {
        continue;
      }
      if (newStateCounter % 2 == 0) {
        changedNewState[newStateCounter++] = newState[i];
      } else {
        changedNewState[newStateCounter++] = newState[pairedTransitionId];
      }
      visited[pairedTransitionId] = true;
    }

    mask[predicateId] = false;
    newState = Arrays.copyOf(changedNewState, changedNewState.length);
    transitionUsed = new boolean[changedNewState.length];
    Arrays.fill(transitionUsed, false);
  }


  public void removePredicateForSimplifying(int predicateId, boolean doLeaveFirst) {
    int relativePredicateId = getMeaningfulPredicateIds().indexOf(predicateId);

    int[] changedNewState = new int[(int) (newState.length / 2)];
    int newStateCounter = 0;
    boolean[] visited = new boolean[newState.length];
    Arrays.fill(visited, false);

    for (int i = 0; i < newState.length; i++) {
      if (visited[i]) {
        continue;
      }
      visited[i] = true;
      int pairedTransitionId = i + (int) Math.pow(2, getMeaningfulPredicatesCount() - relativePredicateId - 1);
      if (pairedTransitionId >= newState.length) {
        continue;
      }

      if (doLeaveFirst) {
        changedNewState[newStateCounter++] = newState[i];
      } else {
        changedNewState[newStateCounter++] = newState[pairedTransitionId];
      }
      visited[pairedTransitionId] = true;
    }

    mask[predicateId] = false;
    newState = Arrays.copyOf(changedNewState, changedNewState.length);
    transitionUsed = new boolean[changedNewState.length];
    Arrays.fill(transitionUsed, false);
  }

  public void addPredicate(int predicate) {
    int[] changedNewState = new int[2 * newState.length];
    int newStateCounter = 0;
    for (int i = 0; i < newState.length; i++) {
      changedNewState[newStateCounter++] = newState[i];
      changedNewState[newStateCounter++] = newState[i];
    }

    mask[predicate] = true;
    newState = Arrays.copyOf(changedNewState, changedNewState.length);
    transitionUsed = new boolean[changedNewState.length];
    Arrays.fill(transitionUsed, false);
  }

  public void clearUsedTransitions() {
    Arrays.fill(transitionUsed, false);
  }

  public void deleteUnusedTransitions() {
    for (int i = 0; i < newState.length; i++) {
      if (!transitionUsed[i]) {
        newState[i] = -1;
      }
    }
  }

  public int getTotalTransitionCoverageFrequency() {
    int result = 0;
    for (int i : transitionUseNumber) {
      result += i;
    }
    return result;
  }

}