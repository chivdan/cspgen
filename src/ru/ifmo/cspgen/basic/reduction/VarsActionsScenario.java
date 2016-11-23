package ru.ifmo.cspgen.basic.reduction;

import java.util.ArrayList;
import java.util.List;

import ru.ifmo.cspgen.basic.ecc.OutputAction;

public class VarsActionsScenario {
  private int outputCount = 0;
  private List<ScenarioElement> elements = new ArrayList<ScenarioElement>();

  public VarsActionsScenario() {
  }

  public VarsActionsScenario(List<ScenarioElement> elements) {
    this.elements.addAll(elements);
    for (int i = 0; i < elements.size(); i++) {
      outputCount += elements.get(i).getActions().size();
    }
  }

  public void add(ScenarioElement e) {
    elements.add(e);
    outputCount += e.getActions().size();
  }

  public ScenarioElement get(int i) {
    return elements.get(i);
  }

  public int size() {
    return elements.size();
  }

  public int getOutputCount() {
    return outputCount;
  }

  public List<ScenarioElement> getElements() {
    return elements;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (ScenarioElement e : elements) {
      sb.append(e);
    }
    return sb.toString();
  }

  public String[] getOutputs() {
    String[] outputs = new String[elements.size()];
    for (int i = 0; i < elements.size(); i++) {
      outputs[i] = "";
      for (OutputAction a : elements.get(i).getActions()) {
        outputs[i] += a.getAlgorithm() + a.getOutputEvent();
      }
    }
    return outputs;
  }

  public String[] getOutputEvents() {
    List<String> result = new ArrayList<String>();
    for (int i = 0; i < elements.size(); i++) {
      for (OutputAction a : elements.get(i).getActions()) {
        result.add(a.getOutputEvent());
      }
    }
    return result.toArray(new String[0]);
  }

  public List<OutputAction> getActions(int i) {
    return elements.get(i).getActions();
  }

  @Override
  public int hashCode() {
    return elements.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof VarsActionsScenario && elements.equals(((VarsActionsScenario) obj).elements);
  }
}
