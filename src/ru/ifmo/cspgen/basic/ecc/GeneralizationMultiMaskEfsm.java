package ru.ifmo.cspgen.basic.ecc;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;

public class GeneralizationMultiMaskEfsm extends MultiMaskEfsm {

	public GeneralizationMultiMaskEfsm(MultiMaskEfsmSkeleton skeleton) {
		super(skeleton);
		this.skeleton = new GeneralizationSkeleton(skeleton);
	}

	public GeneralizationMultiMaskEfsm(MultiMaskEfsmSkeleton skeleton, List<OutputAction>[] actions) {
		super(skeleton, actions);
		this.skeleton = new GeneralizationSkeleton(skeleton);
		this.actions = new ArrayList[MultiMaskEfsmSkeleton.STATE_COUNT];
		for (int i = 0; i < MultiMaskEfsmSkeleton.STATE_COUNT; i++) {
			this.actions[i].addAll(actions[i]);
		}
	}

	public GeneralizationMultiMaskEfsm(String filename) {
		super(filename);
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

		skeleton = new GeneralizationSkeleton(states);
		skeleton.removeNullTransitionGroups();
	}
}
