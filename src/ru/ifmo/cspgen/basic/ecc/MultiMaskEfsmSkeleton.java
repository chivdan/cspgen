package ru.ifmo.cspgen.basic.ecc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import ru.ifmo.cspgen.basic.reduction.VarsActionsScenario;
import ru.ifmo.util.StringUtils;


public class MultiMaskEfsmSkeleton {
    public static int STATE_COUNT;
    public static int PREDICATE_COUNT;
    public static int INPUT_EVENT_COUNT;
    public static int MEANINGFUL_PREDICATES_COUNT;
    public static int TRANSITION_GROUPS_COUNT;
    public static int MAX_OUTPUT_ACTION_COUNT;
    public static String[] OUTPUT_VARIABLES;
    public static int OUTPUT_VARIABLES_COUNT;
    public static Map<String, Integer> INPUT_EVENTS;
    public static Map<String, Integer> OUTPUT_EVENTS;
    public static List<String> PREDICATE_NAMES;
    public static List<Integer> OCCURRING_INPUTS;
    protected final List<VarsActionsScenario> counterExamples = new ArrayList<>();
    private int initialState;
    protected State[] states;
    protected double fitness;
    private static Random random = new Random();
    
    public MultiMaskEfsmSkeleton() {
        states = new State[STATE_COUNT];
    }
    
    public int getNumberOfStates() {
    	return states.length;
    }

    public MultiMaskEfsmSkeleton(State[] states) {
        this.states = states;
    }

    public MultiMaskEfsmSkeleton(MultiMaskEfsmSkeleton other) {
        states = new State[other.states.length];
        this.initialState = other.initialState;
        fitness = other.fitness;
        for (int i = 0; i < states.length; i++) {
            states[i] = new State(other.states[i]);
        }
    }

    public static String tranIdToLabel(int tranId, List<Integer> meaningfulPredicates) {
        String f = Integer.toBinaryString(tranId);
        while (f.length() < meaningfulPredicates.size()) {
            f = "0" + f;
        }
        StringBuilder formula = new StringBuilder();
        for (int i = 0; i < f.length(); i++) {
            if (f.charAt(i) == '0') {
                formula.append("!");
            }
            formula.append(MultiMaskEfsmSkeleton.PREDICATE_NAMES.get(meaningfulPredicates.get(i)));
            if (i < f.length() - 1) {
                formula.append(" & ");
            }
        }
        return formula.toString();
    }
    
    public String tranIdToFbtLabel(int tranId, List<Integer> meaningfulPredicates) {
        String f = Integer.toBinaryString(MultiMaskEfsmSkeleton.OCCURRING_INPUTS.get(tranId));
        while (f.length() < meaningfulPredicates.size()) {
            f = "0" + f;
        }
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
    
    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public List<VarsActionsScenario> getCounterExamples() {
        return counterExamples;
    }

    public int getCounterExamplesLength() {
        int result = 0;
        for (VarsActionsScenario s : counterExamples) {
            result += s.size();
        }
        return result;
    }

    public void clearCounterExamples() {
        counterExamples.clear();
    }

    public void addCounterExample(VarsActionsScenario scenario) {
        counterExamples.add(scenario);
    }

    public void markTransitionsUnused() {
        for (int i = 0; i < states.length; i++) {
            states[i].markTransitionsUnused();
        }
    }
    
    public void markTransitionsUsed() {
        for (int i = 0; i < states.length; i++) {
            states[i].markTransitionsUsed();
        }
    }
    
    public int getInitialState() {
        return initialState;
    }

    public void setInitialState(int initialState) {
        this.initialState = initialState;
    }

    public int getNewState(int state, String inputEvent, String variableValues) {
        return states[state].getNewState(inputEvent, variableValues);
    }

    public TransitionGroup getNewStateTransitionGroup(int state, String inputEvent, String variableValues) {
        return states[state].getNewStateTransitionGroup(inputEvent, variableValues);
    }

    public State getState(int state) {
        return states[state];
    }

    public boolean hasTransitions(int state) {
        return states[state].getUsedTransitionsCount() > 0;
    }

    public boolean stateUsedInTransitions(int state) {
        if (states[state].getUsedTransitionsCount() > 0) {
            return true;
        }

        for (int otherState = 0; otherState < MultiMaskEfsmSkeleton.STATE_COUNT; otherState++) {
            if (otherState == state) {
                continue;
            }
            for (int eventId = 0; eventId < MultiMaskEfsmSkeleton.INPUT_EVENT_COUNT; eventId++) {
                for (int tgId = 0; tgId < states[otherState].getTransitionGroupCount(eventId); tgId++) {
                    TransitionGroup tg = states[otherState].getTransitionGroup(eventId, tgId);
                    for (int tranId = 0; tranId < tg.getTransitionsCount(); tranId++) {
                        if (tg.isTransitionUsed(tranId) && tg.getNewState(tranId) == state) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    public boolean[][][] getUsedTransitions() {
    	boolean[][][] result = new boolean[states.length][MultiMaskEfsmSkeleton.INPUT_EVENT_COUNT][MultiMaskEfsmSkeleton.OCCURRING_INPUTS.size()];
    	
    	for (int state = 0; state < states.length; state++) {
    		for (int event = 0; event < MultiMaskEfsmSkeleton.INPUT_EVENT_COUNT; event++) {
    			for (int input = 0; input < MultiMaskEfsmSkeleton.OCCURRING_INPUTS.size(); input++) {
    				result[state][event][input] = states[state].getTransitionGroup(event, 0).isTransitionUsed(input);
    			}
    		}
    	}
    	return result;
    }

    public String toGraphvizString(Map<Integer, Integer> stateIdMap) {
        StringBuilder sb = new StringBuilder();

        String[] stringEvents = new String[]{"REQ", ""};
        boolean[] oneTransitions = new boolean[states.length];
        Arrays.fill(oneTransitions, false);
        
        for (int stateId = 0; stateId < states.length; stateId++) {
            State state = states[stateId];
            for (int eventId = 0; eventId < MultiMaskEfsmSkeleton.INPUT_EVENT_COUNT; eventId++) {
                for (int tgId = 0; tgId < states[stateId].getTransitionGroupCount(eventId); tgId++) {
                    TransitionGroup tg = state.getTransitionGroup(eventId, tgId);
                    for (int tranId = 0; tranId < tg.getTransitionsCount(); tranId++) {
                        if (!tg.isTransitionDefined(tranId)) {
                            continue;
                        }
                        if (stateIdMap.get(stateId) == null || stateIdMap.get(tg.getNewState(tranId)) == null) {
                        	continue;
                        }
                        
                        if (!stringEvents[eventId].isEmpty()) {
                        	sb.append(stateIdMap.get(stateId) + " -> " + stateIdMap.get(tg.getNewState(tranId))
                        	+ " [label = \"" + stringEvents[eventId] + " [" + tranIdToLabel(tranId, tg.getMeaningfulPredicateIds()) + "] ()\"];\n");
                        } else {
                        	if (!oneTransitions[stateId]) {
                        		sb.append(stateIdMap.get(stateId) + " -> " + stateIdMap.get(tg.getNewState(tranId))
                        		+ " [label = \"1\"];\n");
                        	}
                        	oneTransitions[stateId] = true;
                        }
                    }
                }
            }
        }

        return sb.toString();
    }
    
    public String toFbtString(int depth) {    	
        StringBuilder sb = new StringBuilder();
        
        Map<Integer, String> idToEvent = new HashMap<Integer, String>();
        for (Entry<String, Integer> e : MultiMaskEfsmSkeleton.INPUT_EVENTS.entrySet()) {
        	idToEvent.put(e.getValue(), e.getKey());
        }
        
        boolean[] oneTransitions = new boolean[states.length];
        Arrays.fill(oneTransitions, false);

        for (int stateId = 0; stateId < states.length; stateId++) {
            State state = states[stateId];
            for (int eventId = 0; eventId < MultiMaskEfsmSkeleton.INPUT_EVENT_COUNT; eventId++) {
                for (int tgId = 0; tgId < states[stateId].getTransitionGroupCount(eventId); tgId++) {
                    TransitionGroup tg = state.getTransitionGroup(eventId, tgId);
                    for (int tranId = 0; tranId < tg.getTransitionsCount(); tranId++) {
                        if (!tg.isTransitionDefined(tranId)) {
                            continue;
                        }
                        if (!tg.isTransitionUsed(tranId)) {
                            continue;
                        }
                        
                        if (!idToEvent.get(eventId).isEmpty()) {
                            sb.append(StringUtils.genSpace(depth) + 
                            		"<ECTransition Source=\"s_" + stateId + "\" Destination=\"s_" + tg.getNewState(tranId) + "\" "
                            				+ "Condition=\"" + idToEvent.get(eventId) + "&amp;"
                            						+ tranIdToFbtLabel(tranId, tg.getMeaningfulPredicateIds())
                            						+ "\" x=\"" + random.nextInt(1000) + 
                            						"\" y=\"" + random.nextInt(1000) + "\"/>\n");
                        } else {
//                            sb.append(StringUtils.genSpace(depth) + 
//                            		"<ECTransition Source=\"s_" + stateId + "\" Destination=\"s_" + tg.getNewState(tranId) + "\" "
//                            				+ "Condition=\"" 
//                            						+ tranIdToFbtLabel(tranId, tg.getMeaningfulPredicateIds())
//                            						+ "\" x=\"" + random.nextInt(1000) + 
//                            						"\" y=\"" + random.nextInt(1000) + "\"/>\n");

                        	if (!oneTransitions[stateId]) {
                        		sb.append(StringUtils.genSpace(depth) + 
                        				"<ECTransition Source=\"s_" + stateId + "\" Destination=\"s_" + tg.getNewState(tranId) + "\" "
                        				+ "Condition=\"1\"" + " x=\"" + random.nextInt(1000) + 
                        				"\" y=\"" + random.nextInt(1000) + "\"/>\n");
                        	}
                        	oneTransitions[stateId] = true;
                        }
                    }
                }
            }
        }

        return sb.toString();
    }

    public int getUsedTransitionsCount() {
        int result = 0;
        for (int i = 0; i < states.length; i++) {
            result += states[i].getUsedTransitionsCount();
        }
        return result;
    }

    public int getDefinedTransitionsCount() {
        int result = 0;
        for (int i = 0; i < states.length; i++) {
            result += states[i].getDefinedTransitionsCount();
        }
        return result;
    }

    public int getTransitionsCount() {
        int result = 0;
        for (int i = 0; i < states.length; i++) {
            result += states[i].getTransitionsCount();
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Map<Integer, Integer> stateIdMap = new HashMap<Integer, Integer>();

        int stateCounter = 0;
        sb.append("digraph efsm{\n");
        for (int state = 0; state < MultiMaskEfsmSkeleton.STATE_COUNT; state++) {
            if (stateUsedInTransitions(state)) {
                String label = "" + state;
                sb.append(stateCounter + " [label=\"s_" + label + "\"];\n");
                stateIdMap.put(state, stateCounter);
                stateCounter++;
            }
        }
        sb.append(toGraphvizString(stateIdMap));
        sb.append("}");

        return sb.toString();
    }

    public void clearUsedTransitions() {
        for (State state : states) {
            state.clearUsedTransitions();
        }
    }

    public void removeNullTransitionGroups() {
        for (State state : states) {
            state.removeNullTransitionGroups();
        }
    }
    
    public void removeEmptyTransitionGroups() {
    	for (State state : states) {
    		state.removeEmptyTransitionGroups();
    	}
    }
    
    public void deleteUnusedTransitions() {
    	for (State state : states) {
    		state.deleteUnusedTransitions();
    	}
    }
    
    public int getMaxNumberOfTransitionsFromState() {
    	int result = Integer.MIN_VALUE;
    	for (State state : states) {
    		result = Math.max(result, state.getDefinedTransitionsCount());
    	}
    	
    	return result;
    }
    
    public int getMaxNumberOfUsedTransitionsFromState() {
    	int result = Integer.MIN_VALUE;
    	for (State state : states) {
    		result = Math.max(result, state.getUsedTransitionsCount());
    	}
    	
    	return result;
    }
    
    public int getUsedPredicatesCount() {
    	int result = 0;
    	for (State state : states) {
    		result += state.getUsedPredicatesCount();
    	}
    	return result;
    }
}
