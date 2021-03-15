package edu.kit.ipd.pronat.ast_synth.patterns.util;

import java.util.ArrayList;
import java.util.List;

import edu.kit.ipd.parse.luna.graph.IArc;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.pronat.postpipelinedatamodel.code.Method;
import edu.kit.ipd.pronat.postpipelinedatamodel.code.Parameter;

/**
 * @author Sebastian Weigelt
 * @author Viktor Kiesel
 */
public abstract class MethodUtils {

	public static final String NOT_FOUND = "not_found";

	public static Method getMethodSignature(IGraph graph, INode functionNode) {
		String methodName = null;
		double score = 0;
		List<Parameter> paras = new ArrayList<>();
		for (IArc arc : functionNode.getOutgoingArcs()) {
			if (arc.getTargetNode().getType().equals(graph.getNodeType("functionName"))) {
				if (arc.getTargetNode().getAttributeValue("topN").equals(1)) { //pick only the highest rated Rated

					if (arc.getTargetNode().getAttributeValue("topNScore") != null) {
						score = ((double) arc.getTargetNode().getAttributeValue("topNScore"));
					}
					methodName = (String) arc.getTargetNode().getAttributeValue("ontologyMethod");
					if (methodName.indexOf("#") > 0) {
						methodName = methodName.substring(methodName.indexOf("#") + 1); //remove onto#
					}
					for (IArc a : arc.getTargetNode().getOutgoingArcs()) {
						if (a.getTargetNode().getType().equals(graph.getNodeType("functionParameter"))) {
							paras.addAll(MethodUtils.getParas(graph, a.getTargetNode()));
						}
					}
				}
			}
		}
		return new Method(methodName, "void", paras, score);
	}

	public static List<Parameter> getParas(IGraph graph, INode paraNode) {
		System.out.println(paraNode);
		List<Parameter> paras = new ArrayList<>();
		String name = (String) paraNode.getAttributeValue("ontologyParameter");
		if (name.indexOf("#") > 0) {
			name = name.substring(name.indexOf("#") + 1); //remove onto#
		}
		String type = (String) paraNode.getAttributeValue("methodParameterToFill");
		if (type != null) {
			type = type.substring(type.indexOf("#") + 1); //remove onto#
		} else {
			type = NOT_FOUND;
		}
		Parameter parameter = new Parameter(name, type);
		paras.add(parameter);

		// Next parameter
		for (IArc arc : paraNode.getOutgoingArcs()) {
			if (arc.getTargetNode().getType().equals(graph.getNodeType("functionParameter"))) {
				paras.addAll(MethodUtils.getParas(graph, arc.getTargetNode()));
			}
		}
		return paras;
	}

	public static List<INode> getTokenRange(IGraph graph, INode functionNode) {
		List<INode> range = new ArrayList<>();

		for (IArc arc : functionNode.getOutgoingArcs()) {
			if (arc.getTargetNode().getType().equals(graph.getNodeType("functionParameter"))) {
				range.addAll(MethodUtils.getTokenRange(graph, arc.getTargetNode()));
			}
			if (arc.getTargetNode().getType().equals(graph.getNodeType("token"))) {
				range.add(arc.getTargetNode());
			}
		}
		return range;
	}

}
