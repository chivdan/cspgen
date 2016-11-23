package ru.ifmo.cspgen.basic.reduction;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import ru.ifmo.cspgen.basic.ecc.DoubleBinaryOutputAlgorithm;
import ru.ifmo.cspgen.basic.ecc.EccUtils;
import ru.ifmo.cspgen.basic.ecc.GeneralizationMultiMaskEfsm;
import ru.ifmo.cspgen.basic.ecc.GeneralizationSkeleton;
import ru.ifmo.cspgen.basic.ecc.GeneralizationTransitionGroup;
import ru.ifmo.cspgen.basic.ecc.MultiMaskEfsmSkeleton;
import ru.ifmo.cspgen.basic.ecc.OutputAction;
import ru.ifmo.cspgen.basic.ecc.State;
import ru.ifmo.cspgen.basic.ecc.TernaryOutputAlgorithm;
import ru.ifmo.cspgen.basic.ecc.TransitionGroup;
import ru.ifmo.cspgen.basic.reduction.ScenarioTree.Edge;
import ru.ifmo.cspgen.basic.reduction.ScenarioTree.Node;

public class NewCspFbGenerator {
	public class Tran {
		public String inputEvent;
		public String label;
		public int destination;

		public Tran(String inputEvent, String label, int destination) {
			this.inputEvent = inputEvent;
			this.label = label;
			this.destination = destination;
		}	

		@Override
		public int hashCode() {
			return (inputEvent + label).hashCode() + destination;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Tran)) {
				return false;
			}
			Tran t = (Tran) obj;
			if (inputEvent.equals(t.inputEvent) && t.label.equals(label) && t.destination == destination) {
				return true;
			}
			return false;
		}

		@Override
		public String toString() {
			return inputEvent + "[" + label + ", " + destination + "]";
		}
	}

	//  protected String modelFileName = "minizinc/model-with-bfs.mzn";
//	protected String modelFileName = "minizinc/model-multiple-with-passive.mzn";
	protected String modelFileName = "minizinc/model-with-passive.mzn";
	protected VarsActionsScenario[] fullScenarios;
	protected VarsActionsScenario[] scenarios;
	protected int statesCount;
	protected List<String> uniqueGuards;

	@Option(name = "--nstates", aliases = {"-n"}, usage = "number of FSM states", metaVar = "<number>", required = true)
	private int nStates;

	@Option(name = "--predicate-names", aliases = {"-p"}, usage = "file with predicate names (default = predicate-names)", metaVar = "<file>", required = false)
	private String predicateNamesFile = "predicate-names";

	@Option(name = "--output-variables", aliases = {"-o"}, usage = "file with output variable names (default = output-variables)", metaVar = "<file>", required = false)
	private String outputVariableNamesFile = "output-variables";

	@Option(name = "--csp-solver", aliases = {"-s"}, usage = "path to CSP solver (default = mzn-cpx)", metaVar = "<csp>", required = false)
	private String cspSolver = "mzn-cpx";

	@Argument(usage = "scenarios file", metaVar = "<file>", required = true)
	private String file;

	public static void main(String args[]) {
		    new NewCspFbGenerator().run(args);
//		new NewCspFbGenerator().run(new String[]{"-n", "15", "tests"});
//		new CspFbGenerator().run(new String[]{"-n", "15", "-p", "short-predicate-names", "-o", "short-output-variables", "tests"});
	}

	public NewCspFbGenerator() {
	}

	public NewCspFbGenerator(int statesCount, String scenariosFile) {
		fullScenarios = EccUtils.readScenarios(scenariosFile);
		scenarios = EccUtils.preprocessScenarios(1, fullScenarios);
//		scenarios = EccUtils.removePassiveElements(scenarios);
		this.statesCount = statesCount;

		EccUtils.readPredicateNames(predicateNamesFile);
		EccUtils.readOutputVariableNames(outputVariableNamesFile);
		MultiMaskEfsmSkeleton.STATE_COUNT = statesCount;
		MultiMaskEfsmSkeleton.PREDICATE_COUNT = MultiMaskEfsmSkeleton.PREDICATE_NAMES.size();
		MultiMaskEfsmSkeleton.OCCURRING_INPUTS = EccUtils.getOccurringInputs(scenarios);
		System.out.println("Number of occurring inputs: " + MultiMaskEfsmSkeleton.OCCURRING_INPUTS.size());
	}

	public String getDataString(ScenarioTree tree) {
		uniqueGuards = new ArrayList<String>();

		for (Node node : tree.getNodes()) {
			for (Edge edge : node.getChildren()) {
				if (edge.getVariableValues().equals("1")) {
					continue;
				}
				if (!uniqueGuards.contains(edge.getVariableValues())) {
					uniqueGuards.add(edge.getVariableValues());
				}
			}
		}

//		uniqueGuards.add("1");

		MultiMaskEfsmSkeleton.INPUT_EVENTS.put("", MultiMaskEfsmSkeleton.INPUT_EVENT_COUNT++);
		MultiMaskEfsmSkeleton.OUTPUT_EVENTS.remove("");
		MultiMaskEfsmSkeleton.OUTPUT_EVENTS.put("INITO", MultiMaskEfsmSkeleton.OUTPUT_EVENTS.size());

		int[] input_events = new int[tree.getNodesCount()];
		int[] guards = new int[tree.getNodesCount()];
		int[] output_events = new int[tree.getNodesCount()];
		int[] parents = new int[tree.getNodesCount()];
		int[][] z = new int[tree.getNodesCount()][MultiMaskEfsmSkeleton.OUTPUT_VARIABLES_COUNT];
		int[] previousActiveVertex = new int[tree.getNodesCount()];

		input_events[0] = 1;//MultiMaskEfsmSkeleton.INPUT_EVENTS.size() + 1;
		guards[0] = uniqueGuards.size() - 1;
		output_events[0] = MultiMaskEfsmSkeleton.OUTPUT_EVENTS.get(tree.getNodes().get(0).getOutputEvent()) + 1;
		parents[0] = tree.getNodesCount();
		for (int l = 0; l < MultiMaskEfsmSkeleton.OUTPUT_VARIABLES_COUNT; l++) {
			z[0][l] = 0;
		}

		for (int nodeNum = 1; nodeNum < tree.getNodesCount(); nodeNum++) {
			Node node = tree.getNodes().get(nodeNum);
			Edge e = node.getIncomingEdge();

			input_events[nodeNum] = MultiMaskEfsmSkeleton.INPUT_EVENTS.get(e.getInputEvent()) + 1;
			guards[nodeNum] = uniqueGuards.indexOf(e.getVariableValues()) + 1;
			output_events[nodeNum] = node.getOutputEvent().isEmpty()
					? MultiMaskEfsmSkeleton.OUTPUT_EVENTS.size() + 1 
							: MultiMaskEfsmSkeleton.OUTPUT_EVENTS.get(node.getOutputEvent()) + 1;
			parents[nodeNum] = node.getParent().getId() + 1;
			for (int l = 0; l < MultiMaskEfsmSkeleton.OUTPUT_VARIABLES_COUNT; l++) {
				z[nodeNum][l] = Character.getNumericValue(node.getAlgorithm().charAt(l));
			}
		}
		
		previousActiveVertex[0] = tree.getNodesCount();
		for (int nodeNum = 1; nodeNum < tree.getNodesCount(); nodeNum++) {
			int node = parents[nodeNum] - 1;
			while (output_events[node] == MultiMaskEfsmSkeleton.OUTPUT_EVENTS.size() + 1) {
				node = parents[node] - 1;
				if (node == 1) {
					break;
				}
			}
			previousActiveVertex[nodeNum] = node + 1;
		}


		StringBuilder sb = new StringBuilder();

		sb.append("include \"");
		sb.append(modelFileName);
		sb.append("\";\n\n");

		sb.append(String.format("C = %d;\n" +
				"V = %d;\n" + 
				"E = %d;\n" + 
				"X = %d;\n" + 
				"O = %d;\n" +
				"L = %d;\n", 
				statesCount, tree.getNodesCount(), MultiMaskEfsmSkeleton.INPUT_EVENT_COUNT - 1,
				uniqueGuards.size(), MultiMaskEfsmSkeleton.OUTPUT_EVENTS.size(), MultiMaskEfsmSkeleton.OUTPUT_VARIABLES_COUNT));

		//		int: C; % machine size
		//		int: V; % scenarios tree size
		//		int: E; % input events count
		//		int: X; % unique input variables sets count
		//		int: O; % output events count
		//		int: L; % output variables count

		sb.append("tree_input_event = ").append(Arrays.toString(input_events)).append(";\n");
		sb.append("tree_guards = ").append(Arrays.toString(guards)).append(";\n");
		sb.append("tree_output_event = ").append(Arrays.toString(output_events)).append(";\n");
		sb.append("tree_previous_active = ").append(Arrays.toString(previousActiveVertex)).append(";\n");
		sb.append("tree_z = ").append("[");
		for (int i = 0; i < z.length; i++) {
			sb.append("| ").append(Arrays.toString(z[i]).substring(1, Arrays.toString(z[i]).length() - 1));
			if (i < z.length - 1) {
				sb.append(",\n");
			}
		}
		sb.append(" |];\n");
		return sb.toString();
	}

	protected void writeMznFile(ScenarioTree tree, String filename) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		out.print(getDataString(tree));

		out.close();
	}

	protected GeneralizationMultiMaskEfsm buildSolution(ScenarioTree tree, int[] x, int[] d_0, int[] d_1, int[] o) {
		int nstates = 0;
		for (int i = 0; i < x.length; i++) {
			nstates = Math.max(nstates, x[i]);
		}
		nstates++;
		List<Tran>[] transitions = new ArrayList[nstates];
		for (int i = 0; i < transitions.length; i++) {
			transitions[i] = new ArrayList<Tran>();
		}

		for (Node node : tree.getNodes()) {
			if (node.getParent() == null) {
				continue;
			}
			int color = x[node.getId()];
			int parentColor = x[node.getParent().getId()];
			String inputEvent = node.getIncomingEdge().getInputEvent();
			String inputVariables = node.getIncomingEdge().getVariableValues();
			Tran newTransition = new Tran(inputEvent, inputVariables, color);
			if (!transitions[parentColor].contains(newTransition)) {
				transitions[parentColor].add(newTransition);
			}
		}

		MultiMaskEfsmSkeleton.TRANSITION_GROUPS_COUNT = 50;
		State[] states = new State[transitions.length];
		for (int i = 0; i < states.length; i++) {
			states[i] = new State();
			for (Tran t : transitions[i]) {
				TransitionGroup tg = new GeneralizationTransitionGroup(MultiMaskEfsmSkeleton.PREDICATE_COUNT);
				for (int j = 0; j < MultiMaskEfsmSkeleton.PREDICATE_COUNT; j++) {
					tg.setMaskElement(j, true);
				}
				tg.setNewState(Integer.valueOf(t.label, 2), t.destination);
				states[i].addTransitionGroup(t.inputEvent, tg);
			}
		}

		MultiMaskEfsmSkeleton.STATE_COUNT = states.length;

		MultiMaskEfsmSkeleton skeleton = new GeneralizationSkeleton(states);

		String[] zeroLabels = new String[states.length];
		String[] oneLabels = new String[states.length];
		Arrays.fill(zeroLabels, "");
		Arrays.fill(oneLabels, "");
		for (int state = 0; state < states.length; state++) {
			for (int j = 0; j < MultiMaskEfsmSkeleton.OUTPUT_VARIABLES_COUNT; j++) {
				zeroLabels[state] += "" + d_0[state * MultiMaskEfsmSkeleton.OUTPUT_VARIABLES_COUNT + j];
				oneLabels[state] += "" + d_1[state * MultiMaskEfsmSkeleton.OUTPUT_VARIABLES_COUNT + j];
			}
		}

		GeneralizationMultiMaskEfsm result = new GeneralizationMultiMaskEfsm(skeleton);

		for (int state = 0; state < states.length; state++) {
			String outputEvent = null;
			for (Entry<String, Integer> e : MultiMaskEfsmSkeleton.OUTPUT_EVENTS.entrySet()) {
				if (e.getValue() == o[state]) {
					outputEvent = e.getKey();
					break;
				}
			}
			if (outputEvent == null) {
				outputEvent = "";
			}
			if (outputEvent.equals("INITO")) {
				outputEvent = "";
			}
			result.addActions(state, new OutputAction(new DoubleBinaryOutputAlgorithm(zeroLabels[state], oneLabels[state]), outputEvent));
		}
		return result;
	}

	protected GeneralizationMultiMaskEfsm buildSolution(ScenarioTree tree, int[] x, int[] raw_y, int[] d_0, int[] d_1, int[] o) {
		int nstates = statesCount + 1;
		List<Tran>[] transitions = new ArrayList[nstates];
		for (int i = 0; i < transitions.length; i++) {
			transitions[i] = new ArrayList<Tran>();
		}

		int[][][] y = new int[nstates][MultiMaskEfsmSkeleton.INPUT_EVENT_COUNT][uniqueGuards.size()];
//		for (int from = 1; from < nstates + 1; from++) {
//			for (int event = 1; event < MultiMaskEfsmSkeleton.INPUT_EVENT_COUNT + 1; event++) {
//				for (int guard = 1; guard < uniqueGuards.size() + 1; guard++) {
//					y[from - 1][event - 1][guard - 1] = raw_y[from * event * guard];
//				}
//			}
//		}
		int from_t = 0; 
		int event_t = 0; 
		int guard_t = 0;
		for (int c = 0; c < raw_y.length; c++) {
			if (from_t == nstates) {
				break;
			}
			
			y[from_t][event_t][guard_t] = (raw_y[c] != nstates - 1 ? raw_y[c] : -1);
			
			if (event_t == MultiMaskEfsmSkeleton.INPUT_EVENT_COUNT - 1 && guard_t == uniqueGuards.size() - 1) {
				from_t++;
				event_t = 0;
				guard_t = 0;
			} else if (event_t < MultiMaskEfsmSkeleton.INPUT_EVENT_COUNT - 1 && guard_t == uniqueGuards.size() - 1) {
				event_t++;
				guard_t = 0;
			} else {
				guard_t++;
			}
		}
		
		String[] stringEvents = new String[]{"REQ", ""};

		for (int from = 0; from < nstates - 1; from++) {
			for (int event = 0; event < MultiMaskEfsmSkeleton.INPUT_EVENT_COUNT; event++) {
				for (int guard = 0; guard < uniqueGuards.size(); guard++) {
//					if (event == 1) {
//						continue;
//					}
					
//					if (y[from][event][guard] == nstates - 1) {
//						continue;
//					}
					
					String inputEvent = stringEvents[event];
					String inputVariables = uniqueGuards.get(guard);
					if (from == y[from][event][guard]) {
						continue;
					}
					
					Tran newTransition = new Tran(inputEvent, inputVariables, y[from][event][guard]);
					if (!transitions[from].contains(newTransition)) {
						transitions[from].add(newTransition);
					}
				}
			}
		}

		MultiMaskEfsmSkeleton.TRANSITION_GROUPS_COUNT = 50;
		State[] states = new State[transitions.length];
		for (int i = 0; i < states.length; i++) {
			states[i] = new State();
			for (Tran t : transitions[i]) {
				TransitionGroup tg = new GeneralizationTransitionGroup(MultiMaskEfsmSkeleton.PREDICATE_COUNT);
				for (int j = 0; j < MultiMaskEfsmSkeleton.PREDICATE_COUNT; j++) {
					tg.setMaskElement(j, true);
				}
				tg.setNewState(Integer.valueOf(t.label, 2), t.destination);
				states[i].addTransitionGroup(t.inputEvent, tg);
			}
		}

		MultiMaskEfsmSkeleton.STATE_COUNT = states.length;

		GeneralizationSkeleton skeleton = new GeneralizationSkeleton(states);

		String[] zeroLabels = new String[states.length];
		String[] oneLabels = new String[states.length];
		Arrays.fill(zeroLabels, "");
		Arrays.fill(oneLabels, "");
		for (int state = 0; state < states.length; state++) {
			for (int j = 0; j < MultiMaskEfsmSkeleton.OUTPUT_VARIABLES_COUNT; j++) {
				zeroLabels[state] += "" + d_0[state * MultiMaskEfsmSkeleton.OUTPUT_VARIABLES_COUNT + j];
				oneLabels[state] += "" + d_1[state * MultiMaskEfsmSkeleton.OUTPUT_VARIABLES_COUNT + j];
			}
		}

		GeneralizationMultiMaskEfsm result = new GeneralizationMultiMaskEfsm(skeleton);

		for (int state = 0; state < states.length; state++) {
			String outputEvent = null;
			for (Entry<String, Integer> e : MultiMaskEfsmSkeleton.OUTPUT_EVENTS.entrySet()) {
				if (e.getValue() == o[state]) {
					outputEvent = e.getKey();
					break;
				}
			}
			if (outputEvent == null) {
				outputEvent = "";
			}
			if (outputEvent.equals("INITO")) {
				outputEvent = "";
			}
			result.addActions(state, new OutputAction(new DoubleBinaryOutputAlgorithm(zeroLabels[state], oneLabels[state]), outputEvent));
		}
		return result;
	}


	private void run(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e.getLocalizedMessage());
			System.err.println("CSP-based inference of IEC 61499 basic function block execution control charts from execution scenarios");
			System.err.println("Author: Daniil Chivilikhin (chivdan@rain.ifmo.ru)\n");
			System.err.print("Usage: ");
			parser.printSingleLineUsage(System.err);
			System.err.println();
			parser.printUsage(System.err);
			return;
		}

		fullScenarios = EccUtils.readScenarios(file);
		scenarios = EccUtils.preprocessScenarios(1, fullScenarios);
//		scenarios = EccUtils.removePassiveElements(scenarios);

		EccUtils.readPredicateNames(predicateNamesFile);
		EccUtils.readOutputVariableNames(outputVariableNamesFile);
		statesCount = nStates;

		//check correspondence between scenarios and predicate/output variable names
		if (MultiMaskEfsmSkeleton.PREDICATE_COUNT != scenarios[0].get(0).getInputVariableValues().length()) {
			System.err.println("Read " + MultiMaskEfsmSkeleton.PREDICATE_COUNT + " predicate names, however scenario #1 has "
					+ scenarios[0].get(0).getInputVariableValues().length() + " input predicates");
			System.exit(1);
		}

		if (MultiMaskEfsmSkeleton.OUTPUT_VARIABLES_COUNT != ((TernaryOutputAlgorithm)scenarios[0].get(0).getAction(0).getAlgorithm()).toString().length()) {
			System.err.println("Read " + MultiMaskEfsmSkeleton.OUTPUT_VARIABLES_COUNT + " predicate names, however scenario #1 has "
					+ ((TernaryOutputAlgorithm)scenarios[0].get(0).getAction(0).getAlgorithm()).toString().length() + " output variables");
			System.exit(1);
		}


		MultiMaskEfsmSkeleton.STATE_COUNT = nStates;
		MultiMaskEfsmSkeleton.PREDICATE_COUNT = MultiMaskEfsmSkeleton.PREDICATE_NAMES.size();
		MultiMaskEfsmSkeleton.OUTPUT_VARIABLES_COUNT = scenarios[0].get(0).getAction(0).getAlgorithm().toString().length();
		MultiMaskEfsmSkeleton.OCCURRING_INPUTS = EccUtils.getOccurringInputs(scenarios);

		ScenarioTree tree = new ScenarioTree();
		for (VarsActionsScenario s : scenarios) {
			tree.addScenario(s);
		}

		writeMznFile(tree, "fb.mzn");

		int[] x = new int[tree.getNodesCount()];
		int[] d_0 = null;
		int[] d_1 = null;
		int[] o = null;
		int[] raw_y = null;

		long start = System.currentTimeMillis();

		boolean solutionFound = false;
		List<GeneralizationMultiMaskEfsm> solutions = new ArrayList<GeneralizationMultiMaskEfsm>();
		try {
			Process p = Runtime.getRuntime().exec(cspSolver + " fb.mzn");
			Scanner s = new Scanner (new BufferedInputStream(p.getInputStream()));
			while (s.hasNext()) {
				String line = s.nextLine();

				if (line.contains("------")) {
					GeneralizationMultiMaskEfsm solution = buildSolution(tree, x, raw_y, d_0, d_1, o);
					solution.markTransitionsUsed();
					System.out.println("Solution found in " + (System.currentTimeMillis() - start) / 1000.0 + " sec.");
					solutions.add(solution);
					solution.markTransitionsUsed();
					solution.printGv(".", "solution", false);
					solution.printGv(".", "solution-nice", true);
					solution.printFbt(".", "solution");
					solutionFound = true;
					break;
				}

				if (line.startsWith("# x")) {
					line = line.replace("# x = [", "").replace("]", "");
					String[] states = line.split(", ");
					for (int i = 0; i < states.length; i++) {
						x[i] = Integer.valueOf(states[i]) - 1;
					}
				}

				if (line.startsWith("# y")) {
					line = line.replace("# y = [", "").replace("]", "");
					String[] ys = line.split(", ");
					raw_y = new int[ys.length];
					for (int i = 0; i < ys.length; i++) {
						raw_y[i] = Integer.valueOf(ys[i]) - 1;
					}
				}


				if (line.startsWith("# d_0")) {
					line = line.replace("# d_0 = [", "").replace("]", "");
					String[] data = line.split(", ");
					d_0 = new int[data.length];
					for (int i = 0; i < data.length; i++) {
						d_0[i] = Integer.valueOf(data[i]);
					}
				}

				if (line.startsWith("# d_1")) {
					line = line.replace("# d_1 = [", "").replace("]", "");
					String[] data = line.split(", ");
					d_1 = new int[data.length];
					for (int i = 0; i < data.length; i++) {
						d_1[i] = Integer.valueOf(data[i]);
					}
				}

				if (line.startsWith("# o")) {
					line = line.replace("# o = [", "").replace("]", "");
					String[] data = line.split(", ");
					o = new int[data.length];
					for (int i = 0; i < data.length; i++) {
						o[i] = Integer.valueOf(data[i]) - 1;
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Error while reading solver output");
			System.exit(1);
		}

		    tree.printToGraphViz("tree.gv", x);

		if (!solutionFound) {
			System.err.println("No solution found with N=" + nStates + " states, time elapsed = " 
					+ (System.currentTimeMillis() - start) / 1000.0 + " sec.");
		}
	}
}
