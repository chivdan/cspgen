package ru.ifmo.cspgen.basic.ecc;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import ru.ifmo.cspgen.basic.reduction.ScenarioElement;
import ru.ifmo.cspgen.basic.reduction.VarsActionsScenario;
import ru.ifmo.util.Pair;
import ru.ifmo.util.StringUtils;

public class EccUtils {
	public static VarsActionsScenario[] readScenarios(String scenariosFile) {
		Scanner in = null;
		try {
			in = new Scanner(new File(scenariosFile));
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Cannot open scenarios file \"" + scenariosFile + "\"");
		}
		
		VarsActionsScenario[] scenarios = new VarsActionsScenario[in.nextInt()];
		in.nextLine();
		int scenarioIndex = 0;	
		
		MultiMaskEfsmSkeleton.INPUT_EVENTS = new HashMap<String, Integer>();
		MultiMaskEfsmSkeleton.OUTPUT_EVENTS = new HashMap<String, Integer>();
		int maxOutputActionCount = 0;
		
		while (in.hasNext()) {
			String[] io = in.nextLine().split(";"); 

			scenarios[scenarioIndex] = new VarsActionsScenario();
			
			int i = 0;
			
			//TODO replace
			String input = null;
			String inputEvent = "";
			String output = "";
			List<OutputAction> outputActions = new ArrayList<OutputAction>();
			
			while (i < io.length) {
				if (io[i].contains("in=")) {
					if (input != null) {
						if (outputActions.isEmpty()) {
							outputActions.add(new OutputAction(new TernaryOutputAlgorithm(output), ""));
						} 
						if (outputActions.size() > maxOutputActionCount) {
							maxOutputActionCount = outputActions.size();
						}
						scenarios[scenarioIndex].add(new ScenarioElement(inputEvent, input, outputActions));
					}
						
					outputActions.clear();
					input = io[i].replace("in=", "").trim();
					inputEvent = input.substring(0, input.indexOf("["));
					input = input.substring(input.indexOf("[") + 1 , input.indexOf("]"));
					if (MultiMaskEfsmSkeleton.PREDICATE_COUNT == -1) {
						MultiMaskEfsmSkeleton.PREDICATE_COUNT = input.length();
					}
					
					if (!MultiMaskEfsmSkeleton.INPUT_EVENTS.containsKey(inputEvent)) {
						MultiMaskEfsmSkeleton.INPUT_EVENTS.put(inputEvent, MultiMaskEfsmSkeleton.INPUT_EVENTS.size());
					}
					i++;
				} else if (io[i].contains("out=")) {
					int j = i;
					while (io[j].contains("out=")) {
						output = io[j].replace("out=", "").trim();
						String outputEvent = output.substring(0, output.indexOf("["));
						output = output.substring(output.indexOf("[") + 1, output.indexOf("]"));
						outputActions.add(new OutputAction(new TernaryOutputAlgorithm(output), outputEvent));
						if (outputEvent.isEmpty()) {
							System.out.println();
						}
						
						if (!MultiMaskEfsmSkeleton.OUTPUT_EVENTS.containsKey(outputEvent)) {
							MultiMaskEfsmSkeleton.OUTPUT_EVENTS.put(outputEvent, MultiMaskEfsmSkeleton.OUTPUT_EVENTS.size());
						}	
						j++;
						if (j == io.length) {
							break;
						}
					}
					i += j - i;
				} else {
					i++;
				}				
			}
			if (!outputActions.isEmpty()) {
				scenarios[scenarioIndex].add(new ScenarioElement(inputEvent, input, outputActions));	
			}
			scenarioIndex++;
		}
		in.close();
		
		MultiMaskEfsmSkeleton.MAX_OUTPUT_ACTION_COUNT = maxOutputActionCount;
		
		MultiMaskEfsmSkeleton.INPUT_EVENT_COUNT = MultiMaskEfsmSkeleton.INPUT_EVENTS.size();
		
		int outputVariablesCount = scenarios[0].get(scenarios[0].size() - 1).getActions().get(0).getAlgorithm().toString().length();
		for (int i = 0; i < scenarios.length; i++) {
			for (int j = 0; j < scenarios[i].size(); j++) {
				if (scenarios[i].get(j).getActions().isEmpty()) {
					scenarios[i].get(j).addActions(getActions('0', outputVariablesCount), "");
					if (!MultiMaskEfsmSkeleton.OUTPUT_EVENTS.containsKey("")) {
						MultiMaskEfsmSkeleton.OUTPUT_EVENTS.put("", MultiMaskEfsmSkeleton.OUTPUT_EVENTS.size());
					}
				} else if (scenarios[i].get(j).getActions().get(0).getAlgorithm().toString().isEmpty()) {
					scenarios[i].get(j).getActions().clear();
					scenarios[i].get(j).addActions(getActions('0', outputVariablesCount), "");
					if (!MultiMaskEfsmSkeleton.OUTPUT_EVENTS.containsKey("")) {
						MultiMaskEfsmSkeleton.OUTPUT_EVENTS.put("", MultiMaskEfsmSkeleton.OUTPUT_EVENTS.size());
					}
				}
			}
		}
		
		return scenarios;
	}
	
	public static VarsActionsScenario parseScenario(String s) {
		String[] io = s.split(";"); 

		VarsActionsScenario result = new VarsActionsScenario();
		
		int i = 0;
		
		String input = null;
		String inputEvent = "";
		String output = StringUtils.getString('0', MultiMaskEfsmSkeleton.OUTPUT_VARIABLES_COUNT);
		List<OutputAction> outputActions = new ArrayList<OutputAction>();
		
		while (i < io.length) {
			if (io[i].contains("in=")) {
				if (input != null) {
					if (outputActions.isEmpty() && !output.isEmpty()) {
						outputActions.add(new OutputAction(new TernaryOutputAlgorithm(output), ""));
					} 
					result.add(new ScenarioElement(inputEvent, input, outputActions));
				}
					
				outputActions.clear();
				input = io[i].replace("in=", "").trim();
				inputEvent = input.substring(0, input.indexOf("["));
				input = input.substring(input.indexOf("[") + 1 , input.indexOf("]"));
				if (MultiMaskEfsmSkeleton.PREDICATE_COUNT == -1) {
					MultiMaskEfsmSkeleton.PREDICATE_COUNT = input.length();
				}
				
				if (!MultiMaskEfsmSkeleton.INPUT_EVENTS.containsKey(inputEvent)) {
					MultiMaskEfsmSkeleton.INPUT_EVENTS.put(inputEvent, MultiMaskEfsmSkeleton.INPUT_EVENTS.size());
				}
				i++;
			} else if (io[i].contains("out=")) {
				int j = i;
				while (io[j].contains("out=")) {
					output = io[j].replace("out=", "").trim();
					String outputEvent = output.substring(0, output.indexOf("["));
					output = output.substring(output.indexOf("[") + 1, output.indexOf("]"));
					outputActions.add(new OutputAction(new TernaryOutputAlgorithm(output), outputEvent));
					
					if (!MultiMaskEfsmSkeleton.OUTPUT_EVENTS.containsKey(outputEvent)) {
						MultiMaskEfsmSkeleton.OUTPUT_EVENTS.put(outputEvent, MultiMaskEfsmSkeleton.OUTPUT_EVENTS.size());
					}	
					j++;
					if (j == io.length) {
						break;
					}
				}
				i += j - i;
			} else {
				i++;
			}				
		}
		if (!outputActions.isEmpty()) {
			result.add(new ScenarioElement(inputEvent, input, outputActions));	
		}
		
		if (result.size() == 0 && outputActions.isEmpty()) {
			outputActions.add(new OutputAction(new TernaryOutputAlgorithm(output), ""));
			result.add(new ScenarioElement(inputEvent, input, outputActions));
		}
		return result;
	}
	
	public static String getActions(char c, int outputVariablesCount) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < outputVariablesCount; i++) {
			sb.append(c);
		}
		return sb.toString();
	}
	
	public static void readPredicateNames(String filename) {
		Scanner in = null;
		try {
			in = new Scanner(new File(filename));
		} catch (FileNotFoundException e) {
			System.err.println("Cannon open predicate names file \"" + filename + "\"");
			System.exit(1);
		}
		
		try {
			MultiMaskEfsmSkeleton.PREDICATE_NAMES = new ArrayList<String>();
			while (in.hasNext()) {
				MultiMaskEfsmSkeleton.PREDICATE_NAMES.add(in.next());
			}
		} catch (Exception e) {
			System.err.println("Error while reading predicate names file \"" + filename + "\"");
			System.exit(1);
		}
		
		in.close();
		
		MultiMaskEfsmSkeleton.PREDICATE_COUNT = MultiMaskEfsmSkeleton.PREDICATE_NAMES.size();
	}
	
	public static VarsActionsScenario[] preprocessScenarios(int scale, VarsActionsScenario[] scenarios) {
		VarsActionsScenario[] result = new VarsActionsScenario[scenarios.length];

		for (int i = 0; i < scenarios.length; i++) {
			List<ScenarioElement> processed = new ArrayList<ScenarioElement>();
			int j = 0;
			if (scenarios[i].size() == 1) {
				processed.add(scenarios[i].get(0));
				result[i] = new VarsActionsScenario(processed);
				continue;
			}
			ScenarioElement currentElement = scenarios[i].get(j++);
			int numberOfRepeats = 1;
			while (j < scenarios[i].size()) {
				if (scenarios[i].get(j).equals(currentElement)) {
					j++;
					numberOfRepeats++;
				} else {
					for (int k = 0; k < Math.min(numberOfRepeats, scale); k++) {
						processed.add(currentElement);
					}
					currentElement = scenarios[i].get(j);
					numberOfRepeats = 1;
					j++;
				}

				if (j == scenarios[i].size()) {
					for (int k = 0; k < Math.min(numberOfRepeats, scale); k++) {
						processed.add(currentElement);
					}
				}
			}

			result[i] = new VarsActionsScenario(processed);
		}
		return result;
	}
	
	public static void readOutputVariableNames(String filename) {
		Scanner in = null;
		try {
			in = new Scanner(new File(filename));
		} catch (FileNotFoundException e) {
			System.err.println("Cannon open output variable names file \"" + filename + "\"");
			System.exit(1);
		}
		
		List<String> names = new ArrayList<String>();
		try {
			while (in.hasNext()) {
				names.add(in.nextLine());
			}
		} catch (Exception e) {
			System.err.println("Error while reading output variable names file \"" + filename + "\"");
		}
		
		in.close();
		
		MultiMaskEfsmSkeleton.OUTPUT_VARIABLES = names.toArray(new String[0]);
		MultiMaskEfsmSkeleton.OUTPUT_VARIABLES_COUNT = names.size();
	}
	
	public static VarsActionsScenario[] removePassiveElements(VarsActionsScenario[] scenarios) {
		VarsActionsScenario[] result = new VarsActionsScenario[scenarios.length];

		for (int i = 0; i < scenarios.length; i++) {
			List<ScenarioElement> processed = new ArrayList<ScenarioElement>();
			int j = 0;
			for (ScenarioElement e : scenarios[i].getElements()) {
				if (!e.getActions().get(0).getOutputEvent().isEmpty() || j == 0) {
					processed.add(e);
				}
				j++;
			}
			result[i] = new VarsActionsScenario(processed);
		}
		return result;
	}
	
	public static List<Integer> getOccurringInputs(VarsActionsScenario[] scenarios) {
		List<Integer> result = new ArrayList<Integer>();
		for (VarsActionsScenario s : scenarios) {
			for (ScenarioElement e : s.getElements()) {
				int id = Integer.valueOf(e.getInputVariableValues(), 2);
				if (!result.contains(id)) {
					result.add(id);
				}
			}
		}
		return result;
	}
	
	public static VarsActionsScenario[] preprocessScenariosNew(int scale, VarsActionsScenario[] scenarios) {
		VarsActionsScenario[] result = new VarsActionsScenario[scenarios.length];

		for (int i = 0; i < scenarios.length; i++) {
			List<ScenarioElement> processed = new ArrayList<ScenarioElement>();
			int j = 0;

			while (j < scenarios[i].size()) {
				ScenarioElement currentElement = scenarios[i].get(j);
				Pair<List<ScenarioElement>, Integer> sequence = EccUtils.getEqualElements(scenarios[i], j);
				if (sequence == null) {
					processed.add(currentElement);
					j++;
					continue;
				}
				for (int k = 0; k < Math.min(scale, sequence.second); k++) {
					processed.addAll(sequence.first);
				}
				j += sequence.first.size() * sequence.second;
			}

			result[i] = new VarsActionsScenario(processed);
		}

//		for (int i = 0; i < scenarios.length; i++) {
//			System.out.println(scenarios[i].size() + " " + result[i].size());
//		}
//		System.out.println();

		return result;
	}

	public static Pair<List<ScenarioElement>, Integer> getEqualElements(VarsActionsScenario scenario, int position) {
		List<ScenarioElement> elements = new ArrayList<ScenarioElement>();
		List<Integer> positions = new ArrayList<Integer>();
		int distance = -1;

		ScenarioElement element = scenario.get(position);
		elements.add(element);
		positions.add(position);

		int last = position;
		for (int i = position + 1; i < scenario.size(); i++) {
			if (scenario.get(i).equals(element)) {
				if (distance == -1) {
					distance = i - last;    				
					positions.add(i);
				} else if (i - last != distance) {
					break;
				} else {
					positions.add(i);
				}
				last = i;
			}
		}

		return EccUtils.getEqualElements(scenario, positions, distance);
	}

	public static Pair<List<ScenarioElement>, Integer> getEqualElements(VarsActionsScenario scenario, List<Integer> positions, int distance) {
		if (positions.size() < 2) {
			return null;
		}

		for (int i = 1; i < distance; i++) {
			ScenarioElement e = scenario.get(positions.get(0) + i);
			for (int j = 1; j < positions.size(); j++) {
				if (positions.get(j) + i >= scenario.size()) {
					List<Integer> newPositions = new ArrayList<Integer>();
					newPositions.addAll(positions);
					newPositions.remove(newPositions.size() - 1);
					return getEqualElements(scenario, newPositions, distance);
				}
				if (!scenario.get(positions.get(j) + i).equals(e)) {
					List<Integer> newPositions = new ArrayList<Integer>();
					newPositions.addAll(positions);
					newPositions.remove(newPositions.size() - 1);
					return getEqualElements(scenario, newPositions, distance);
				}
			}
		}

		List<ScenarioElement> elements = new ArrayList<ScenarioElement>();
		for (int i = 0; i < distance; i++) {
			elements.add(scenario.get(positions.get(0) + i));
		}

		return new Pair<List<ScenarioElement>, Integer>(elements, positions.size());
	}
}


