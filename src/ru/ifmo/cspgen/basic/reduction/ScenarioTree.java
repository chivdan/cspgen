package ru.ifmo.cspgen.basic.reduction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import ru.ifmo.cspgen.basic.ecc.MultiMaskEfsmSkeleton;
import ru.ifmo.cspgen.basic.ecc.OutputAction;
import ru.ifmo.cspgen.basic.ecc.TernaryOutputAlgorithm;
import ru.ifmo.util.StringUtils;

public class ScenarioTree {
	public class Node {
		private int id;
		private Node parent;
		private Edge incomingEdge;
		private OutputAction actions;
		private List<Edge> children = new ArrayList<Edge>();
		private int loopTo = -1;
		private boolean terminal = false;

		public Node(Node parent, Edge incomingEdge, OutputAction actions) {
			this.id = nodesCount++;
			this.parent = parent;
			this.incomingEdge = incomingEdge;
			this.actions = actions;
			nodes.add(this);
		}

		public Node addChild(ScenarioElement element) {
			for (Edge e : children) {
				if (e.inputEvent.equals(element.getInputEvent()) && e.variableValues.equals(element.getInputVariableValues())) {
					if (!e.child.actions.equals(element.getActions().get(0))) {
						throw new RuntimeException("Illegal merge, different output actions with equal history!");
					}
					return e.child;
				}
			}
			
			if (children.size() == 1) {
				if (children.get(0).getInputEvent().isEmpty()) {
					return children.get(0).getChild().addChild(element);
				}
			}
			
			if (element.getActionsCount() == 1) {
				Edge child = new Edge(this, element);			
				children.add(child);			
				return child.getChild();
			} else if (element.getActionsCount() > 1) {
				Node current = this;

				//create partial element that contains only the first output action
				ScenarioElement head = new ScenarioElement(element.getInputEvent(), element.getInputVariableValues(), 
						element.getAction(0));
				Edge child = new Edge(current, head);
				current.children.add(child);
				current = child.getChild();
				for (int i = 1; i < element.getActionsCount(); i++) {
					ScenarioElement part = new ScenarioElement("", current.getIncomingEdge().getVariableValues(), element.getAction(i));
//					ScenarioElement part = new ScenarioElement("", "1", element.getAction(i));
					child = new Edge(current, part);
					current.children.add(child);
					current = child.getChild();
				}
				
//				//add final node
//				ScenarioElement last = new ScenarioElement("", "1", new OutputAction(element.getLastAction().getAlgorithm(), ""));
//				child = new Edge(current, last);
//				current.children.add(child);
//				current = child.getChild();
				return child.getChild();
			} else {
				throw new RuntimeException("");
			}

		}

		public int getId() {
			return id;
		}

		public Node getParent() {
			return parent;
		}

		public Edge getIncomingEdge() {
			return incomingEdge;
		}

		public String getOutputEvent() {
			return actions.getOutputEvent();
		}

		public String getAlgorithm() {
			return actions.getAlgorithm().toString();
		}

		public List<Edge> getChildren() {
			return children;
		}

		public int getChildrenCount() {
			return children.size();
		}

		public int getLoop() {
			return loopTo;
		}

		@Override
		public int hashCode() {
			return id;
		}

		public Node getPreviousActiveNode() {
			Node result = parent;
			while (result.getOutputEvent().isEmpty()) {
				result = result.parent;
			}
			return result;
		}

		public boolean isTerminal() { 
			return terminal;
		}

		public void markTerminal() {
			terminal = true;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Node)) {
				return false;
			}
			Node other = (Node)obj;
			return id == other.id;
		}
	}

	public class Edge {
		private String inputEvent;
		private String variableValues;
		private Node parent;
		private Node child;

		public Edge(Node parent, ScenarioElement element) {
			this.parent = parent;
			this.inputEvent = element.getInputEvent();
			this.variableValues = element.getInputVariableValues();
			this.child = new Node(parent, this, element.getActions().get(0));
		}

		public String getInputEvent() {
			return inputEvent;
		}

		public String getVariableValues() {
			return variableValues;
		}

		public Node getParent() {
			return parent;
		}

		public Node getChild() {
			return child;
		}
	}

	private Node root; 
	private List<Node> nodes;
	private int nodesCount;

	public ScenarioTree() {
		nodesCount = 0;
		nodes = new ArrayList<Node>();
		root = new Node(null, null, new OutputAction(new TernaryOutputAlgorithm(
				StringUtils.getString('0', MultiMaskEfsmSkeleton.OUTPUT_VARIABLES_COUNT)), "INITO"));
	}

	public void addScenario(VarsActionsScenario scenario) {
		Node current = root;
		for (ScenarioElement e : scenario.getElements()) {
			current = current.addChild(e);
		}
	}

	public void deleteSubtree(Node node) {
		nodes.remove(node);
		for (Edge e : node.children) {
			deleteSubtree(e.getChild());
		}
		node.children.clear();
	}

	public Node getRoot() {
		return root;
	}

	public List<Node> getNodes() {
		return nodes;
	}

	public int getNodesCount() {
		return nodes.size();
	}

	public void printToGraphViz(String filename, int[] x) {
		PrintWriter out = null; 
		try {
			out = new PrintWriter(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		String[] colors = new String[]{"red", "green", "blue", "gray", "yellow", "cyan", "white", "brown", "black", "lightgray"};

		out.println("digraph ecc{");
		out.println("node[style=filled];");

		for (Node node : nodes) {
			StringBuilder sb = new StringBuilder();
			sb.append(node.getId()).append(" [label=<").append(node.getId()).append("<br/>");
			String a = node.getAlgorithm();
			for (int i = 0; i < MultiMaskEfsmSkeleton.OUTPUT_VARIABLES_COUNT; i++) {
				String outputVar = MultiMaskEfsmSkeleton.OUTPUT_VARIABLES[i];
				if (node.getParent() != null) {
					if (node.getParent().getAlgorithm().charAt(i) == node.getAlgorithm().charAt(i)) {
						sb.append(outputVar).append(" = ").append(a.charAt(i) == '1' ? "True" : "False").append("<br/>");
					} else {
						sb.append("<font color='red'>").append(outputVar).append(" = ").append(a.charAt(i) == '1' ? "True" : "False").append("</font><br/>");
					}
				} else {
					sb.append(outputVar).append(" = ").append(a.charAt(i) == '1' ? "True" : "False").append("<br/>");
				}

			}
			if (!node.getOutputEvent().isEmpty()) {
				sb.append("<br/><i>e</i><sup><i>o</i></sup>=").append(node.getOutputEvent());
			}
			sb.append(">");
			if (node.isTerminal()) {
				sb.append(", fillcolor=red");
			}
			sb.append("];");
			out.println(sb.toString());
		}

		for (Node node : nodes) {
			if (node.getParent() == null) {
				continue;
			}

			StringBuilder sb = new StringBuilder();
			sb.append(node.getParent().getId() + " -> " + node.getId()).append( "[label=<");
			if (node.getIncomingEdge() != null) {
				sb.append(node.getIncomingEdge().inputEvent).append("<br/>");
			}
			for (int i = 0; i < MultiMaskEfsmSkeleton.PREDICATE_COUNT; i++) {
				String predicate = MultiMaskEfsmSkeleton.PREDICATE_NAMES.get(i);
				if (node.getIncomingEdge().getParent().getIncomingEdge() == null) {
					sb.append(predicate).append(" = ").append(node.getIncomingEdge().variableValues.charAt(i) == '1' ? "True" : "False").append("<br/>");
				} else {
					if (node.getIncomingEdge().getParent().getIncomingEdge().getVariableValues().charAt(i) == node.getIncomingEdge().getVariableValues().charAt(i)) {
						sb.append(predicate).append(" = ").append(node.getIncomingEdge().variableValues.charAt(i) == '1' ? "True" : "False").append("<br/>");
					} else {
						sb.append("<font color='red'>").append(predicate).append(" = ").append(node.getIncomingEdge().variableValues.charAt(i) == '1' ? "True" : "False").append("</font><br/>");
					}
				}
			}
			sb.append(">];");

			out.println(sb.toString());

			if (node.loopTo >= 0) {
				out.println(node.getId() + " -> " + node.loopTo
						+ " [style=dotted];");
			}
		}

		out.println("}");
		out.close();
	}
}
