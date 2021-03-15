package edu.kit.ipd.pronat.ast_synth.patterns;

import edu.kit.ipd.pronat.ast_synth.patterns.util.MethodUtils;

import edu.kit.ipd.parse.luna.graph.IArc;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.pronat.postpipelinedatamodel.ast.ASTConstants;
import edu.kit.ipd.pronat.postpipelinedatamodel.ast.IASTPattern;
import edu.kit.ipd.pronat.postpipelinedatamodel.code.Method;

/**
 * @author Sebastian Weigelt
 * @author Viktor Kiesel
 */
public class MethodDeclarationFinder implements IASTPattern {

	private static final String NEW_METHOD = ASTConstants.NEW_METHOD;
	IGraph graph;
	INode root;
	INode symbols;

	@Override
	public String getBaseType() {
		return null;
	}

	@Override
	public void init(IGraph graph, INode root, INode symbols) {
		this.graph = graph;
		this.root = root;
		this.symbols = symbols;

	}

	@Override
	public IGraph extractPattern(IGraph graph) {
		for (INode commandMapper : graph.getNodesOfType(graph.getNodeType("commandMapper"))) {
			for (IArc arc : commandMapper.getOutgoingArcs()) {
				if (arc.getTargetNode().getType().equals(graph.getNodeType("declaration"))) {
					for (IArc a : arc.getTargetNode().getOutgoingArcs()) {
						if (a.getTargetNode().getType().equals(graph.getNodeType("functionCall"))) {
							Method method = MethodUtils.getMethodSignature(graph, a.getTargetNode());
							root.setAttributeValue(NEW_METHOD, method);
						}
					}
				}
			}
		}
		return graph;
	}

	@Override
	public boolean checkPattern(IGraph graph) {
		if (((Method) root.getAttributeValue(NEW_METHOD)) != null) {
			if (graph.getNodesOfType(graph.getNodeType(ASTConstants.AST_METHOD)).isEmpty()) {
				INode error = graph.createNode(graph.getNodeType(ASTConstants.AST_ERROR));
				error.setAttributeValue("message", "Sentence is Teaching Sequence, but has no Tree. Mayor Error! ");
				error.setAttributeValue("level", "error");
				error.setAttributeValue("type", ASTConstants.Errors.EMPTY_SEQUENCE);
				graph.createArc(error, root, graph.getArcType(ASTConstants.AST_ERROR_POINTER));
				return false;
			}
		}
		return true;
	}

}
