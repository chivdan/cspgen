package ru.ifmo.cspgen.basic.ecc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.ifmo.util.StringUtils;

public class MultiMaskEfsm {
	public static Pattern transitionPattern = Pattern.compile("^([0-9]+)\\s->\\s([0-9]+)\\s\\[label\\s=\\s\"([A-Z]+)\\s\\[(.+)\\] \\(\\)\"\\];$");
	public static Pattern statePattern = Pattern.compile("([0-9]+)\\s\\[label=\"s_([01x]+(_[0-9]+)?)\\(([A-Z]*)\\);(_[1-9])?\"\\];");

	protected MultiMaskEfsmSkeleton skeleton;
	protected List<OutputAction>[] actions;
	private static Random random = new Random();

	public MultiMaskEfsm(MultiMaskEfsmSkeleton skeleton) {
		this.skeleton = skeleton;
		actions = new ArrayList[MultiMaskEfsmSkeleton.STATE_COUNT];
		for (int i = 0; i < MultiMaskEfsmSkeleton.STATE_COUNT; i++) {
			actions[i] = new ArrayList<OutputAction>();
		}
	}

	public MultiMaskEfsm(MultiMaskEfsmSkeleton skeleton, List<OutputAction>[] actions) {
		this.skeleton = skeleton;
		this.actions = new ArrayList[MultiMaskEfsmSkeleton.STATE_COUNT];
		for (int i = 0; i < MultiMaskEfsmSkeleton.STATE_COUNT; i++) {
			this.actions[i] = new ArrayList<OutputAction>();
			for (int j = 0; j < actions[i].size(); j++) {
				this.actions[i].add(actions[i].get(j));
			}
		}
	}

	public MultiMaskEfsm(String filename) {
		State[] states = new State[MultiMaskEfsmSkeleton.STATE_COUNT];
		actions = new ArrayList[MultiMaskEfsmSkeleton.STATE_COUNT];
		for (int i = 0; i < MultiMaskEfsmSkeleton.STATE_COUNT; i++) {
			actions[i] = new ArrayList<OutputAction>();
		}
		Map<String, Integer>[] transitionGroupMap = new HashMap[MultiMaskEfsmSkeleton.STATE_COUNT];

		for (int stateId = 0; stateId < transitionGroupMap.length; stateId++) {
			states[stateId] = new State();
			transitionGroupMap[stateId] = new HashMap<String, Integer>();
		}

		Scanner in = null;
		try {
			in = new Scanner(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		while (in.hasNext()) {
			String line = in.nextLine();
			if (!line.contains("->")) {
				Matcher m = statePattern.matcher(line);
				m.find();
				if (!m.matches()) {
					continue;
				}
				int state = Integer.parseInt(m.group(1));
				String algorithm = m.group(2);
				String outputEvent = m.group(4);
				actions[state].add(new OutputAction(new TernaryOutputAlgorithm(algorithm), outputEvent));
				continue;
			}
			Matcher m = transitionPattern.matcher(line);
			m.find();
			if (!m.matches()) {
				continue;
			}
			int startState = Integer.parseInt(m.group(1));
			int endState = Integer.parseInt(m.group(2));
			String event = m.group(3);
			String formula = m.group(4);

			List<Integer> meaningfulPredicatesList = new ArrayList<Integer>();
			StringBuilder inputVariables = new StringBuilder();
			StringBuilder meaningfulPredicates = new StringBuilder();

			//convert formula to bit string
			for (String v : formula.split(" & ")) {
				int value = v.contains("!") ? 0 : 1;
				inputVariables.append(value);
				String predicateName = v.contains("!") ? v.replace("!", "") : v;
				int predicateId = MultiMaskEfsmSkeleton.PREDICATE_NAMES.indexOf(predicateName);
				meaningfulPredicates.append(predicateId + ",");
				meaningfulPredicatesList.add(predicateId);
			}

			int eventIndex = MultiMaskEfsmSkeleton.INPUT_EVENTS.get(event);

			//determine transition group
			int transitionGroupId = -1;
			if (transitionGroupMap[startState].containsKey(meaningfulPredicates.toString())) {
				transitionGroupId = transitionGroupMap[startState].get(meaningfulPredicates.toString());
			} else {
				transitionGroupId = transitionGroupMap[startState].size();
				if (transitionGroupId >= MultiMaskEfsmSkeleton.MEANINGFUL_PREDICATES_COUNT) {
					throw new RuntimeException("Wrong number of transitionGroups in state " + startState);
				}
				transitionGroupMap[startState].put(meaningfulPredicates.toString(), transitionGroupId);
			}

			//if transition group is empty, initialize its mask
			if (states[startState].getTransitionGroup(eventIndex, transitionGroupId) == null) {
				states[startState].addTransitionGroup(event, new TransitionGroup(meaningfulPredicates.length()));
				for (Integer predicateId : meaningfulPredicatesList) {
					states[startState].getTransitionGroup(eventIndex, transitionGroupId).setMaskElement(predicateId, true);
				}
			}

			int transitionIndex = Integer.valueOf(inputVariables.toString(), 2);
			states[startState].getTransitionGroup(eventIndex, transitionGroupId).setNewState(transitionIndex, endState);
			states[startState].getTransitionGroup(eventIndex, transitionGroupId).setTransitionUsed(transitionIndex, true);
		}

		in.close();

		skeleton = new MultiMaskEfsmSkeleton(states);
		skeleton.removeNullTransitionGroups();
	}

	public MultiMaskEfsm(String filename, boolean generalization) {
		State[] states = new GeneralizationState[MultiMaskEfsmSkeleton.STATE_COUNT];
		actions = new ArrayList[MultiMaskEfsmSkeleton.STATE_COUNT];
		for (int i = 0; i < MultiMaskEfsmSkeleton.STATE_COUNT; i++) {
			actions[i] = new ArrayList<OutputAction>();
		}
		Map<String, Integer>[] transitionGroupMap = new HashMap[MultiMaskEfsmSkeleton.STATE_COUNT];

		for (int stateId = 0; stateId < transitionGroupMap.length; stateId++) {
			states[stateId] = new GeneralizationState();
			transitionGroupMap[stateId] = new HashMap<String, Integer>();
		}

		Scanner in = null;
		try {
			in = new Scanner(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		while (in.hasNext()) {
			String line = in.nextLine();
			if (!line.contains("->")) {
				Matcher m = statePattern.matcher(line);
				m.find();
				if (!m.matches()) {
					continue;
				}
				int state = Integer.parseInt(m.group(1));
				String algorithm = m.group(2);
				String outputEvent = m.group(4);
				actions[state].add(new OutputAction(new TernaryOutputAlgorithm(algorithm), outputEvent));
				continue;
			}
			Matcher m = transitionPattern.matcher(line);
			m.find();
			if (!m.matches()) {
				continue;
			}
			int startState = Integer.parseInt(m.group(1));
			int endState = Integer.parseInt(m.group(2));
			String event = m.group(3);
			String formula = m.group(4);

			List<Integer> meaningfulPredicatesList = new ArrayList<Integer>();
			StringBuilder inputVariables = new StringBuilder();
			StringBuilder meaningfulPredicates = new StringBuilder();

			//convert formula to bit string
			for (String v : formula.split(" & ")) {
				int value = v.contains("!") ? 0 : 1;
				inputVariables.append(value);
				String predicateName = v.contains("!") ? v.replace("!", "") : v;
				int predicateId = MultiMaskEfsmSkeleton.PREDICATE_NAMES.indexOf(predicateName);
				meaningfulPredicates.append(predicateId + ",");
				meaningfulPredicatesList.add(predicateId);
			}

			//      int eventIndex = MultiMaskEfsmSkeleton.INPUT_EVENTS.get(event);

			GeneralizationTransitionGroup newTg = new GeneralizationTransitionGroup(meaningfulPredicates.length());
			states[startState].addTransitionGroup(event, newTg);
			for (Integer predicateId : meaningfulPredicatesList) {
				newTg.setMaskElement(predicateId, true);
			}

			int transitionIndex = Integer.valueOf(inputVariables.toString(), 2);
			newTg.setNewState(transitionIndex, endState);
			newTg.setTransitionUsed(transitionIndex, true);
		}

		in.close();

		skeleton = new GeneralizationSkeleton(states);
		skeleton.removeNullTransitionGroups();
	}

	public int getNumberOfStates() {
		return skeleton.getNumberOfStates();
	}

	public void markTransitionsUnused() {
		skeleton.markTransitionsUnused();
	}

	public void markTransitionsUsed() {
		skeleton.markTransitionsUsed();
	}

	public MultiMaskEfsmSkeleton getSkeleton() {
		return skeleton;
	}

	public void setSkeleton(MultiMaskEfsmSkeleton skeleton) {
		this.skeleton = skeleton;
	}

	public int getInitialState() {
		return skeleton.getInitialState();
	}

	public int getNewState(int state, String inputEvent, String variableValues) {
		int newState = skeleton.getNewState(state, inputEvent, variableValues);
		return newState;
	}

	public void setNumberOfActions(int state, int size) {
		while (actions[state].size() < size) {
			actions[state].add(null);
		}
	}

	public void setActions(int state, OutputAction actions, int i) {
		this.actions[state].set(i, actions);
	}

	public void addActions(int state, OutputAction actions) {
		this.actions[state].add(actions);
	}

	public List<OutputAction> getActions(int state) {
		return actions[state];
	}

	public int getActionsCount(int state) {
		return actions[state].size();
	}

	public State getState(int stateId) {
		return skeleton.getState(stateId);
	}

	public String toGraphvizString(boolean makeNice) {
		StringBuilder sb = new StringBuilder();
		Map<Integer, Integer> stateIdMap = new HashMap<Integer, Integer>();
		Map<String, Integer> labelSet = new HashMap<String, Integer>();
		markTransitionsUsed();

		int stateCounter = 0;
		sb.append("digraph efsm{\n");
		for (int state = 0; state < actions.length; state++) {
			if (skeleton.stateUsedInTransitions(state)) {
				String label = state + ":";
				for (OutputAction a : actions[state]) {
					label += a.getAlgorithm() + "(" + a.getOutputEvent() +");";
				}            		

				if (labelSet.containsKey(label)) {
					labelSet.put(label, labelSet.get(label) + 1);
					label = label + "_" + labelSet.get(label);
				} else {
					labelSet.put(label, 1);
				}
				StringBuilder niceLabel = new StringBuilder();
				if (actions[state].get(0).getAlgorithm() instanceof DoubleBinaryOutputAlgorithm) {
					niceLabel.append("<").append(state + 1).append("<br/>");
					DoubleBinaryOutputAlgorithm a = (DoubleBinaryOutputAlgorithm) actions[state].get(0).getAlgorithm();
					for (int i = 0; i < MultiMaskEfsmSkeleton.OUTPUT_VARIABLES_COUNT; i++) {						
						String outputVar = MultiMaskEfsmSkeleton.OUTPUT_VARIABLES[i];
						niceLabel.append(outputVar).append(": 0→").append(a.getZeroAlgorithm().charAt(i)).append(", ");                		
						niceLabel.append("1→").append(a.getOneAlgorithm().charAt(i));
						if (i < MultiMaskEfsmSkeleton.OUTPUT_VARIABLES_COUNT - 1) {
							niceLabel.append("<br/>");
						}
					}
					if (!actions[state].get(0).getOutputEvent().isEmpty()) {
						niceLabel.append("<br/><i>e</i><sup><i>o</i></sup>=").append(actions[state].get(0).getOutputEvent());
					}
					niceLabel.append(">");
				} else {
					niceLabel.append(label);
				}

				if (makeNice) {
					sb.append(stateCounter + " [label=" + niceLabel + "];\n");
				} else {
					sb.append(stateCounter + " [label=" + label + "];\n");
				}
				stateIdMap.put(state, stateCounter);
				stateCounter++;
			}
		}
		sb.append(skeleton.toGraphvizString(stateIdMap));
		sb.append("}");

		return sb.toString();
	}

	public OutputAction[] getActionsForSMV() {
		List<OutputAction> outputActions = new ArrayList<OutputAction>();
		for (List<OutputAction> a : actions) {
			outputActions.addAll(a);
		}
		return outputActions.toArray(new OutputAction[0]);
	}

	public List<OutputAction>[] getActions() {
		return actions;
	}

	public void printGv(String dirname, String name, boolean makeNice) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new File(dirname +"/" + name + ".gv"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		out.println(toGraphvizString(makeNice));
		out.close();
	}

	public void printFbt(String dirname, String name) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new File(dirname +"/" + name + ".fbt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		out.println(genFbtHeader(name));
		out.println(genFbtInterfaceList(1));
		out.println(genBasicFB(1));
		out.println("</FBType>");
		out.close();
	}

	private String genFbtHeader(String name) {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<FBType Name=\"Controller\" Comment=\"Basic Function Block Type\" Namespace=\"Main\">");
		sb.append("  <Identification Standard=\"61499-2\" />\n");
		sb.append("  <VersionInfo Organization=\"nxtControl GmbH\" Version=\"0.0\" Author=\"cspgen\" Date=\"2011-08-30\" Remarks=\"Template\" />\n");
		return sb.toString();
	}

	private String genFbtInterfaceList(int depth) {
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.genSpace(depth) + "<InterfaceList>\n");
		sb.append(genEventInputs(depth + 1));
		sb.append(genEventOutputs(depth + 1));
		sb.append(genInputVars(depth + 1));
		sb.append(genOutputVars(depth + 1));
		sb.append(StringUtils.genSpace(depth) + "</InterfaceList>\n");
		return sb.toString();
	}

	private String genEventInputs(int depth) {
		List<String> predicates = new ArrayList<String>();
		predicates.addAll(MultiMaskEfsmSkeleton.PREDICATE_NAMES);
		for (String outputVariableName : MultiMaskEfsmSkeleton.OUTPUT_VARIABLES) {
			predicates.remove(outputVariableName);
		}

		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.genSpace(depth) + "<EventInputs>\n");
		sb.append(StringUtils.genSpace(depth + 1) + "<Event Name=\"INIT\" Comment=\"Initialization Request\" ></Event>\n");
		for (String inputEvent : MultiMaskEfsmSkeleton.INPUT_EVENTS.keySet()) {
			if (inputEvent.isEmpty()) {
				continue;
			}
			sb.append(genEventVariableAssociations(inputEvent, depth + 1, predicates.toArray(new String[0])));
		}
		sb.append(StringUtils.genSpace(depth) + "</EventInputs>\n");
		return sb.toString();
	}

	private String genEventOutputs(int depth) {
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.genSpace(depth) + "<EventOutputs>\n");
		for (String outputEvent : MultiMaskEfsmSkeleton.OUTPUT_EVENTS.keySet()) {
			if (outputEvent.isEmpty()) {
				continue;
			}
			sb.append(genEventVariableAssociations(outputEvent, depth + 1, MultiMaskEfsmSkeleton.OUTPUT_VARIABLES));
		}
		sb.append(StringUtils.genSpace(depth) + "</EventOutputs>\n");
		return sb.toString();
	}

	private String genEventVariableAssociations(String event, int depth, String[] variables) {
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.genSpace(depth) + "<Event Name=\"" + event + "\" Comment=\"" + event + "\" >\n");
		for (String variable : variables) {
			sb.append(StringUtils.genSpace(depth + 1) + "<With Var=\"" + variable + "\" />\n");
		}
		sb.append(StringUtils.genSpace(depth) + "</Event>\n");
		return sb.toString();
	}

	private String genInputVars(int depth) {
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.genSpace(depth) + "<InputVars>\n");
		List<String> predicates = new ArrayList<String>();
		predicates.addAll(MultiMaskEfsmSkeleton.PREDICATE_NAMES);
		for (String outputVariableName : MultiMaskEfsmSkeleton.OUTPUT_VARIABLES) {
			predicates.remove(outputVariableName);
		}
		for (String inputVariable : predicates) {
			sb.append(StringUtils.genSpace(depth + 1) + "<VarDeclaration Name=\"" + inputVariable + "\" Type=\"BOOL\" />\n");
		}
		sb.append(StringUtils.genSpace(depth) + "</InputVars>\n");
		return sb.toString();
	}

	private String genOutputVars(int depth) {
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.genSpace(depth) + "<OutputVars>\n");
		for (String outputVariable : MultiMaskEfsmSkeleton.OUTPUT_VARIABLES) {
			sb.append(StringUtils.genSpace(depth + 1) + "<VarDeclaration Name=\"" + outputVariable + "\" Type=\"BOOL\" />\n");
		}
		sb.append(StringUtils.genSpace(depth) + "</OutputVars>\n");
		return sb.toString();
	}

	private String genBasicFB(int depth) {
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.genSpace(depth) + "<BasicFB>\n");
		sb.append(genEcc(depth + 1));
		sb.append(genAlgorithms(depth + 1));
		sb.append(StringUtils.genSpace(depth) + "</BasicFB>\n");
		return sb.toString();
	}

	private String genEcc(int depth) {
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.genSpace(depth) + "<ECC>\n");
		sb.append(genStates(depth + 1));
		sb.append(genTransitions(depth + 1));
		sb.append(StringUtils.genSpace(depth) + "</ECC>\n");
		return sb.toString();
	}

	private String genStates(int depth) {
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.genSpace(depth + 1) + "<ECState Name=\"START\" Comment=\"Initial State\" x=\"" + random.nextInt(1000) + "\" y=\"" + random.nextInt(1000) + "\" > </ECState>\n");
		sb.append(StringUtils.genSpace(depth + 1) + "<ECState Name=\"INIT\" Comment=\"Initialization\" x=\"" + random.nextInt(1000) + "\" y=\"" + random.nextInt(1000) + "\" >\n");
		sb.append(StringUtils.genSpace(depth + 2) + "<ECAction Algorithm=\"INIT\" Output=\"INITO\" />\n");
		sb.append(StringUtils.genSpace(depth + 1) + "</ECState>\n");

		for (int state = 0; state < actions.length; state++) {
			sb.append(StringUtils.genSpace(depth + 1) + "<ECState Name=\"s_" + state + "\" x=\"" + random.nextInt(1000) + "\" y=\"" + random.nextInt(1000) + "\">\n");

			for (int actionId = 0; actionId < actions[state].size(); actionId++) {
				sb.append(StringUtils.genSpace(depth + 2) + "<ECAction Algorithm=\"a_" + actions[state].get(actionId).getAlgorithm() + 
						(!actions[state].get(actionId).getOutputEvent().isEmpty() ? "_" + actions[state].get(actionId).getOutputEvent() : "") + 
						"\" Output=\"" + actions[state].get(actionId).getOutputEvent() + "\" />\n");
			}

			sb.append(StringUtils.genSpace(depth + 1) + "</ECState>\n");
		}
		return sb.toString();
	}

	private String genTransitions(int depth) {
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.genSpace(depth) + 
				"<ECTransition Source=\"START\" Destination=\"INIT\" Condition=\"INIT\" x=\"" + random.nextInt(1000) + "\" y=\"" + random.nextInt(1000) + "\" />\n");
		sb.append(StringUtils.genSpace(depth) + 
				"<ECTransition Source=\"INIT\" Destination=\"s_0\" Condition=\"1\" x=\"" + random.nextInt(1000) + "\" y=\"" + random.nextInt(1000) + "\" />\n");
		sb.append(skeleton.toFbtString(depth));
		return sb.toString();
	}

	private String genAlgorithms(int depth) {
		StringBuilder sb = new StringBuilder();
		Set<OutputAction> uniqueActions = new HashSet<OutputAction>();
		for (List<OutputAction> a : actions) {
			uniqueActions.addAll(a);
		}
		sb.append(StringUtils.genSpace(depth) + "<Algorithm Name=\"INIT\" Comment=\"Initialization algorithm\" >\n");
		sb.append(StringUtils.genSpace(depth + 1) + "<ST Text=\"");
		sb.append("REQ := FALSE;&#xD;&#xA;");
		for (String outputVariable : MultiMaskEfsmSkeleton.OUTPUT_VARIABLES) {
			sb.append(outputVariable + ":=FALSE;&#xD;&#xA;");
		}
		sb.append("\"/>\n");
		sb.append(StringUtils.genSpace(depth) + "</Algorithm>\n");
		for (OutputAction action : uniqueActions) {
			sb.append(StringUtils.genSpace(depth) + "<Algorithm Name=\"a_" + action.getAlgorithm() + 
					(!action.getOutputEvent().isEmpty() ? ("_" + action.getOutputEvent()) : "") + "\">\n");
			sb.append(StringUtils.genSpace(depth + 1) + action.getAlgorithm().toFbtString());
			sb.append(StringUtils.genSpace(depth) + "</Algorithm>\n");
		}
		return sb.toString();
	}

	public void deleteUnusedTransitions() {
		skeleton.deleteUnusedTransitions();
	}
}
