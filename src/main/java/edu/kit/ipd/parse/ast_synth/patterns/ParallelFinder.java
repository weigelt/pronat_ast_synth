package edu.kit.ipd.parse.ast_synth.patterns;

import java.util.HashSet;
import java.util.Set;

import edu.kit.ipd.parse.luna.data.ast.ASTConstants;
import edu.kit.ipd.parse.luna.data.ast.IASTPattern;
import edu.kit.ipd.parse.luna.graph.IArc;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.graph.INodeType;

/**
 * @author Sebastian Weigelt
 * @author Viktor Kiesel
 */
public class ParallelFinder implements IASTPattern {

	private static final String AST_PARALLEL_ROOT = ASTConstants.AST_PARALLEL_ROOT;
	private static final String AST_PARALLEL_BASE = ASTConstants.AST_PARALLEL_BASE;
	private static final String PARALLEL_NUMBER = "parallelNumber";
	final String AST_POINTER = ASTConstants.AST_POINTER;
	final String POINTER_SUM = ASTConstants.POINTER_SUM;

	INode cbase;

	@Override
	public String getBaseType() {
		return AST_PARALLEL_BASE;
	}

	@Override
	public void init(IGraph graph, INode treeNode, INode symbols) {
		INodeType type;
		type = graph.createNodeType(AST_PARALLEL_BASE);
		type.addAttributeToType(Integer.class.getName(), PARALLEL_NUMBER);
		type.addAttributeToType(Integer.class.getName(), ASTConstants.POINTER_SUM);
		type.addAttributeToType(Double.class.getName(), ASTConstants.POSITION);

		type = graph.createNodeType(AST_PARALLEL_ROOT);
		type.addAttributeToType(Integer.class.getName(), PARALLEL_NUMBER);
		type.addAttributeToType(Integer.class.getName(), "rootID");
		type.addAttributeToType(Integer.class.getName(), ASTConstants.POINTER_SUM);
		type.addAttributeToType(Double.class.getName(), ASTConstants.POSITION);

	}

	@Override
	public IGraph extractPattern(IGraph graph) {
		int parallelNumber = -1;

		// Trennung: While hat Conditon aber For hat KeyPhrase

		for (INode n : graph.getNodesOfType(graph.getNodeType("concurrentAction"))) {
			//			System.out.println(n.getAttributeValue("type"));
			//if (!n.getAttributeValue("type").equals("LOOP")) 
			{ //Change to not equal FOr
				parallelNumber++;

				cbase = graph.createNode(graph.getNodeType(AST_PARALLEL_BASE));
				cbase.setAttributeValue(PARALLEL_NUMBER, parallelNumber);
				cbase.setAttributeValue(ASTConstants.POINTER_SUM, 0);

				graph.createArc(cbase, n, graph.getArcType(ASTConstants.AST_BASE_POINTER));

				//Condition
				INode loopnode = n;
				Set<INode> roots = new HashSet<>();
				for (IArc arc : loopnode.getOutgoingArcsOfType(graph.getArcType("dependentconcurrentAction"))) {

					//Init root
					INode root = graph.createNode(graph.getNodeType(AST_PARALLEL_ROOT));
					root.setAttributeValue(PARALLEL_NUMBER, parallelNumber);
					root.setAttributeValue(ASTConstants.POINTER_SUM, 0);
					root.setAttributeValue("rootID", arc.getAttributeValue("position"));
					roots.add(root);

					for (INode token : graph.getNodesOfType(graph.getNodeType("token"))) {
						if ((token.getAttributeValue("instructionNumber"))
								.equals(arc.getTargetNode().getAttributeValue("instructionNumber"))) {
							graph.createArc(root, token, graph.getArcType(AST_POINTER));
						}
					}
				}

				for (INode r : roots) {
					graph.createArc(cbase, r, graph.getArcType(ASTConstants.AST_CHILD));
					cbase.setAttributeValue(POINTER_SUM, (int) r.getAttributeValue(POINTER_SUM) + 1);
				}

			}
		}
		return graph;
	}

	@Override
	public boolean checkPattern(IGraph graph) {
		for (INode node : graph.getNodesOfType(graph.getNodeType(ASTConstants.AST_PARALLEL_ROOT))) {
			if (node.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD_EXTERN)).isEmpty()) {
				INode error = graph.createNode(graph.getNodeType(ASTConstants.AST_ERROR));
				error.setAttributeValue("message",
						"Parallel Sequence is empty. Either Multiple Parallel-Roots refer to same Sequence or MethodStructure is missing");
				error.setAttributeValue("level", "warning");
				error.setAttributeValue("type", ASTConstants.Errors.EMPTY_SEQUENCE);
				graph.createArc(error, node, graph.getArcType(ASTConstants.AST_ERROR_POINTER));
				return false;
			}
		}

		return true;
	}

}
