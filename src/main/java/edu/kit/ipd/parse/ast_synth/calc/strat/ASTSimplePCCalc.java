package edu.kit.ipd.parse.ast_synth.calc.strat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.MetaInfServices;

import edu.kit.ipd.parse.luna.data.ast.ASTConstants;
import edu.kit.ipd.parse.luna.data.ast.IASTParentConfidenceCalculator;
import edu.kit.ipd.parse.luna.graph.IArc;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;

/**
 * @author Sebastian Weigelt
 * @author Viktor Kiesel
 */
public class ASTSimplePCCalc implements IASTParentConfidenceCalculator {

	IGraph graph;

	@Override
	public void init(IGraph graph) {
		this.graph = graph;
	}

	@Override
	public Map<INode, Double> findPossibleParentAndCalculateConfidence(INode n) {
		// finds possible parents for node n and puts them in confidence matrix
		// looks at nodes it refers to and gets other treeparts refering to the same node
		// modifies confidence matrix

		Map<INode, Double> confidence = new HashMap<>();
		List<? extends IArc> children = n.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD));
		if (children.isEmpty()) { // Empty: n is ActionNode (Method/Leaf)
			confidence = calcConfidence(n);
		} else { // Others
			for (IArc child : children) {
				confidence = calcConfidence(child.getTargetNode());
			}
		}

		return confidence;
	}

	private Map<INode, Double> calcConfidence(INode n) {
		Map<INode, Double> confidence = new HashMap<>();
		for (IArc arc : n.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_POINTER))) { // dafuq?
			for (IArc a : arc.getTargetNode().getIncomingArcsOfType(graph.getArcType(ASTConstants.AST_POINTER))) {
				if (!a.getSourceNode().equals(n) // Can't be same
						// can't be Block-Child
						&& (!a.getSourceNode().getType().equals(graph.getNodeType(ASTConstants.AST_METHOD)))) {
					if (!confidence.containsKey(a.getSourceNode())) {
						confidence.put(a.getSourceNode(), 0.0);
					}
					// Similarity function: Size of other 
					confidence.put(a.getSourceNode(), confidence.get(a.getSourceNode())
							+ 1.0 / (double) n.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_POINTER)).size());

				}
			}
		}
		return confidence;

	}

	@Override
	public String getID() {
		return "Simple";
	}

}
