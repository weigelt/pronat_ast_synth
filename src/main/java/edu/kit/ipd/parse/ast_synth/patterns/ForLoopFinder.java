package edu.kit.ipd.parse.ast_synth.patterns;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import edu.kit.ipd.parse.luna.data.ast.ASTConstants;
import edu.kit.ipd.parse.luna.data.ast.IASTPattern;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.graph.INodeType;

/**
 * @author Sebastian Weigelt
 * @author Viktor Kiesel
 */
public class ForLoopFinder implements IASTPattern {

	private static final String AST_CHILD = ASTConstants.AST_CHILD;
	private static final String AST_POINTER = ASTConstants.AST_POINTER;
	private static final String AST_BASE_POINTER = ASTConstants.AST_BASE_POINTER;
	private static final String AST_FOR_LOOP = ASTConstants.AST_FOR_LOOP;
	private static final String POINTER_SUM = ASTConstants.POINTER_SUM;
	private static final String ITERATIONS = ASTConstants.ITERATIONS;
	private static final String POSITION = ASTConstants.POSITION;
	private static final String LOOP_NUMBER = ASTConstants.LOOP_NUMBER;
	private static final String AST_FOR_BASE = ASTConstants.AST_FOR_BASE;

	INode cbase = null;
	INode cwhile = null;

	Map<String, Integer> phraseToIteration;
	final String BASE = AST_FOR_BASE;

	@Override
	public String getBaseType() {
		return BASE;
	}

	@Override
	public void init(IGraph graph, INode treeNode, INode symbols) {

		// TODO Improve to something that is complex enough to understand bigger numbers
		phraseToIteration = new HashMap<>();
		phraseToIteration.put("once", 1);
		phraseToIteration.put("one time", 1);
		phraseToIteration.put("two times", 2);
		phraseToIteration.put("twice", 2);
		phraseToIteration.put("three times", 3);
		phraseToIteration.put("four times", 4);
		phraseToIteration.put("five times", 5);

		INodeType type;
		type = graph.createNodeType(AST_FOR_BASE);
		type.addAttributeToType(Integer.class.getName(), LOOP_NUMBER);
		type.addAttributeToType(Double.class.getName(), POSITION);
		type.addAttributeToType(Integer.class.getName(), ITERATIONS);
		type.addAttributeToType(Integer.class.getName(), POINTER_SUM);

		//		type = graph.createNodeType("AST-For-If");
		//		type.addAttributeToType(Integer.class.getName(), "loopNumber");

		type = graph.createNodeType(AST_FOR_LOOP);
		type.addAttributeToType(Integer.class.getName(), LOOP_NUMBER);
		type.addAttributeToType(Integer.class.getName(), POINTER_SUM);
		type.addAttributeToType(Double.class.getName(), POSITION);

	}

	@Override
	public IGraph extractPattern(IGraph graph) {
		int loopNumber = -1;

		// Trennung: While hat Conditon aber For hat KeyPhrase
		for (INode n : graph.getNodesOfType(graph.getNodeType("loop"))) {
			if (n.getAttributeValue("type").toString().equals("LOOP")) {
				loopNumber++;

				cbase = graph.createNode(graph.getNodeType(AST_FOR_BASE));
				cbase.setAttributeValue(LOOP_NUMBER, loopNumber);
				cbase.setAttributeValue(POINTER_SUM, 0);

				cbase.setAttributeValue(ITERATIONS, getTableIterations((String) n.getAttributeValue("keyphrase")));
				//				cif = graph.createNode(graph.getNodeType("AST-For-If"));
				//				cif.setAttributeValue("loopNumber", loopNumber);
				cwhile = graph.createNode(graph.getNodeType(AST_FOR_LOOP));
				cwhile.setAttributeValue(LOOP_NUMBER, loopNumber);
				cwhile.setAttributeValue(POINTER_SUM, 0);

				graph.createArc(cbase, n, graph.getArcType(AST_BASE_POINTER));
				//				graph.createArc(cbase, cif, graph.getArcType("AST-Child"));
				graph.createArc(cbase, cwhile, graph.getArcType(AST_CHILD));

				INode loopnode = n;
				//				while (loopnode.getOutgoingArcsOfType(graph.getArcType("loopCondition")).size() > 0) {
				//					loopnode = loopnode.getOutgoingArcsOfType(graph.getArcType("loopCondition")).get(0).getTargetNode();
				//					graph.createArc(cif, loopnode, graph.getArcType("AST-Pointer"));
				//				}

				// Hacky
				loopnode = n.getOutgoingArcsOfType(graph.getArcType("dependentLoopAction")).get(0).getTargetNode();

				// Should be do while
				//				for (int i = 0; i < ((String) n.getAttributeValue("dependentPhrases")).split(",").length - 1; i++) {
				//					graph.createArc(cwhile, loopnode, graph.getArcType(AST_POINTER));
				//					loopnode = loopnode.getOutgoingArcsOfType(graph.getArcType("relation")).get(0).getTargetNode();
				//					cwhile.setAttributeValue(POINTER_SUM, (int) cwhile.getAttributeValue(POINTER_SUM) + 1);
				//				}

				for (INode token : graph.getNodesOfType(graph.getNodeType("token"))) {
					if ((token.getAttributeValue("instructionNumber")).equals(loopnode.getAttributeValue("instructionNumber"))) {
						//						if (!token.getAttributeValue("value").equals(n.getAttributeValue("keyphrase"))) 
						{
							graph.createArc(cwhile, token, graph.getArcType(AST_POINTER));
							cwhile.setAttributeValue(POINTER_SUM, (int) cwhile.getAttributeValue(POINTER_SUM) + 1);
						}
					}
				}

			}
		}
		return graph;
	}

	private int getTableIterations(String word) {
		if (phraseToIteration.containsKey(word)) {
			return phraseToIteration.get(word);
		} else {
			throw new NoSuchElementException("No Iteration for keyphrase: " + word);
		}
	}

	@Override
	public boolean checkPattern(IGraph graph) {
		for (INode f : graph.getNodesOfType(graph.getNodeType(AST_FOR_LOOP))) {
			if (f.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD_EXTERN)).isEmpty()) {
				INode error = graph.createNode(graph.getNodeType(ASTConstants.AST_ERROR));
				error.setAttributeValue("message", "For-Sequence is empty. MethodStructure is missing");
				error.setAttributeValue("level", "warning");
				error.setAttributeValue("type", ASTConstants.Errors.EMPTY_SEQUENCE);
				graph.createArc(error, f, graph.getArcType(ASTConstants.AST_ERROR_POINTER));
				return false;

			}
		}
		return true;
	}

}
