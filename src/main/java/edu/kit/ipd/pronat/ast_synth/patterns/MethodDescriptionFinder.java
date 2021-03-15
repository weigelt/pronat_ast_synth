package edu.kit.ipd.pronat.ast_synth.patterns;

import java.util.Properties;

import edu.kit.ipd.pronat.ast_synth.patterns.util.MethodUtils;

import edu.kit.ipd.parse.luna.graph.IArc;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.tools.ConfigManager;
import edu.kit.ipd.pronat.postpipelinedatamodel.ast.ASTConstants;
import edu.kit.ipd.pronat.postpipelinedatamodel.ast.IASTPattern;
import edu.kit.ipd.pronat.postpipelinedatamodel.code.Method;
import edu.kit.ipd.pronat.postpipelinedatamodel.code.Parameter;

/**
 * @author Sebastian Weigelt
 * @author Viktor Kiesel
 */
public class MethodDescriptionFinder implements IASTPattern {

	private static final String AST_METHOD = ASTConstants.AST_METHOD;
	INode treeNode;
	INode symbols;

	IGraph graph;

	Properties props;

	@Override
	public void init(IGraph graph, INode treeNode, INode symbols) {
		this.graph = graph;
		this.treeNode = treeNode;
		this.symbols = symbols;

		graph.createNodeType(AST_METHOD);
		graph.getNodeType(AST_METHOD).addAttributeToType(Method.class.getName(), "method");
		graph.getNodeType(AST_METHOD).addAttributeToType(Double.class.getName(), ASTConstants.POSITION);
		//Get Function 

		props = ConfigManager.getConfiguration(MethodDescriptionFinder.class);

	}

	@Override
	public IGraph extractPattern(IGraph graph) {

		for (INode n : graph.getNodesOfType(graph.getNodeType("description"))) {
			//			if((Boolean)n.getAttributeValue("isTeachingSequence"))
			//Get Declaration Node
			for (IArc arc : n.getOutgoingArcs()) //functions
			{
				if (arc.getTargetNode().getType().equals(graph.getNodeType("functionCall"))) {
					if (!arc.getTargetNode().getOutgoingArcs().isEmpty()) {
						Method meth = MethodUtils.getMethodSignature(graph, arc.getTargetNode());
						if (meth.getScore() > Double.valueOf(props.getProperty("ScoreMethodThreshold"))) {
							INode methodNode = graph.createNode(graph.getNodeType(AST_METHOD));
							methodNode.setAttributeValue(ASTConstants.POSITION,
									((Integer) arc.getTargetNode().getAttributeValue("number")).doubleValue());
							methodNode.setAttributeValue("method", meth);

							for (IArc rangeArc : arc.getTargetNode().getOutgoingArcs()) {
								if (rangeArc.getTargetNode().getType().equals(graph.getNodeType("functionName"))) {
									if ((int) rangeArc.getTargetNode().getAttributeValue("topN") == 1) {
										for (INode token : graph.getNodesOfType(graph.getNodeType("Token"))) {
											if (token.getAttributeValue("instructionNumber")
													.equals(MethodUtils.getTokenRange(graph, rangeArc.getTargetNode()).get(0)
															.getAttributeValue("instructionNumber"))) {
												graph.createArc(methodNode, token, graph.getArcType(ASTConstants.AST_POINTER));
											}
										}

										//								for (INode token : MethodUtils.getTokenRange(graph, rangeArc.getTargetNode())) {
										//									graph.createArc(methodNode, token, graph.getArcType(ASTConstants.AST_POINTER));
										//							}
									}
								}
							}
						}
					}
				}
			}
		}

		return graph;

	}

	@Override
	public String getBaseType() {
		return AST_METHOD;
	}

	@Override
	public boolean checkPattern(IGraph graph) {
		boolean r = true;
		for (INode token : graph.getNodesOfType(graph.getNodeType("token"))) {
			boolean found = false;
			for (IArc arc : token.getIncomingArcsOfType(graph.getArcType(ASTConstants.AST_POINTER))) {
				if (arc.getSourceNode().getType().equals(graph.getNodeType(ASTConstants.AST_METHOD))) {
					found = true; //jump to next node if token has AST-Method

				}
			}

			// Create Error-Node if token has no AST-Method
			if (!found) {
				int instructionNumber = (int) token.getAttributeValue("instructionNumber");
				String msg = "";
				if (token.getIncomingArcsOfType(graph.getArcType(ASTConstants.AST_ERROR_POINTER)).isEmpty()) {
					INode error = graph.createNode(graph.getNodeType(ASTConstants.AST_ERROR));
					error.setAttributeValue("level", "warning");
					error.setAttributeValue("type", ASTConstants.Errors.MISSING_STRUCTURE);
					for (INode t : graph.getNodesOfType(graph.getNodeType("token"))) {
						if ((int) t.getAttributeValue("instructionNumber") == instructionNumber) {
							graph.createArc(error, t, graph.getArcType(ASTConstants.AST_ERROR_POINTER));
							msg += t.getAttributeValue("value") + " ";
						}
					}
					error.setAttributeValue("message", msg + " has no AST-Method.");
					r = false;
				}
			}

		}

		for (INode node : graph.getNodesOfType(graph.getNodeType(ASTConstants.AST_METHOD))) {
			Method method = (Method) node.getAttributeValue("method");
			String range = "";
			for (IArc a : node.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_POINTER))) {
				range += a.getTargetNode().getAttributeValue("value") + " ";
			}

			// Warning due to bad Method Score Threshold
			if (method.getScore() < Double.valueOf((String) props.get("ScoreErrorThreshold"))) {
				INode error = graph.createNode(graph.getNodeType(ASTConstants.AST_ERROR));
				error.setAttributeValue("message", "Method " + method + " has a low score(" + method.getScore() + ") for: " + range.trim()
						+ ". Ontology may lack the necessary Method");
				error.setAttributeValue("level", "error");
				error.setAttributeValue("type", ASTConstants.Errors.METHOD_ERROR_SCORE);
				graph.createArc(error, node, graph.getArcType(ASTConstants.AST_ERROR_POINTER));
				r = false;

			} else if (method.getScore() < Double.valueOf((String) props.get("ScoreWarningThreshold"))) {
				INode error = graph.createNode(graph.getNodeType(ASTConstants.AST_ERROR));
				error.setAttributeValue("message", "Method " + method + " has a medicore score(" + method.getScore() + ") for: "
						+ range.trim() + ". Ontology may lack the necessary Method");
				error.setAttributeValue("level", "warning");
				error.setAttributeValue("type", ASTConstants.Errors.METHOD_WARN_SCORE);
				graph.createArc(error, node, graph.getArcType(ASTConstants.AST_ERROR_POINTER));
				r = false;

			}
			for (Parameter p : method.getParameters()) {
				//				if (p.getName().contains(" ")) {
				//					INode error = graph.createNode(graph.getNodeType(ASTConstants.AST_ERROR));
				//					error.setAttributeValue("message", "Method " + method + " has strange Parameter with Name:" + p.getName()
				//							+ ". Ontology may lack the necessary Parameter");
				//					error.setAttributeValue("level", "error");
				//					error.setAttributeValue("type", ASTConstants.Errors.STRANGE_PARAMETER);
				//					graph.createArc(error, node, graph.getArcType(ASTConstants.AST_ERROR_POINTER));
				//					r = false;
				//
				//				}

				if (p.getType().contains(" ")) {
					INode error = graph.createNode(graph.getNodeType(ASTConstants.AST_ERROR));
					error.setAttributeValue("message", "Method " + method + " has strange Parameter with Type:" + p.getType()
							+ ". Ontology may lack the necessary Parameter");
					error.setAttributeValue("level", "error");
					error.setAttributeValue("type", ASTConstants.Errors.STRANGE_PARAMETER);
					graph.createArc(error, node, graph.getArcType(ASTConstants.AST_ERROR_POINTER));
					r = false;
				}

				if (p.getType().equals("void")) {
					INode error = graph.createNode(graph.getNodeType(ASTConstants.AST_ERROR));
					error.setAttributeValue("message",
							"Method " + method + " has void-type Parameter:" + p + ". Does it cause problems for " + p.getName() + "? ");
					error.setAttributeValue("level", "warning");
					error.setAttributeValue("type", ASTConstants.Errors.STRANGE_PARAMETER);
					graph.createArc(error, node, graph.getArcType(ASTConstants.AST_ERROR_POINTER));
					r = false;
				}

				if (p.getType().equals(MethodUtils.NOT_FOUND)) {
					INode error = graph.createNode(graph.getNodeType(ASTConstants.AST_ERROR));
					error.setAttributeValue("message", "Method " + method + " has " + MethodUtils.NOT_FOUND + "-type Parameter:" + p
							+ ". No type found in Ontology for " + p.getName() + "? ");
					error.setAttributeValue("level", "error");
					error.setAttributeValue("type", ASTConstants.Errors.STRANGE_PARAMETER);
					graph.createArc(error, node, graph.getArcType(ASTConstants.AST_ERROR_POINTER));
					r = false;
				}
			}

		}
		return r;
	}
}
