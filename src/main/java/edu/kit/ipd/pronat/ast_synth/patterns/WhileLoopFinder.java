package edu.kit.ipd.pronat.ast_synth.patterns;

import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.graph.INodeType;
import edu.kit.ipd.pronat.postpipelinedatamodel.ast.ASTConstants;
import edu.kit.ipd.pronat.postpipelinedatamodel.ast.IASTPattern;

/**
 * @author Sebastian Weigelt
 * @author Viktor Kiesel
 */
public class WhileLoopFinder implements IASTPattern {

	private static final String DO_WHILE = ASTConstants.DO_WHILE;
	private static final String AST_CHILD = ASTConstants.AST_CHILD;
	private static final String AST_POINTER = ASTConstants.AST_POINTER;
	private static final String POINTER_SUM = ASTConstants.POINTER_SUM;
	private static final String LOOP_NUMBER = ASTConstants.LOOP_NUMBER;
	private static final String AST_WHILE_LOOP = ASTConstants.AST_WHILE_LOOP;
	private static final String AST_WHILE_IF = ASTConstants.AST_WHILE_IF;
	private static final String AST_WHILE_BASE = ASTConstants.AST_WHILE_BASE;

	private INode cbase = null;
	private INode cif = null;
	private INode cwhile = null;

	private final String BASE = AST_WHILE_BASE;

	@Override
	public String getBaseType() {
		return BASE;
	}

	@Override
	public void init(IGraph graph, INode treeNode, INode symbols) {
		INodeType type;
		type = graph.createNodeType(AST_WHILE_BASE);
		type.addAttributeToType(Integer.class.getName(), LOOP_NUMBER);
		type.addAttributeToType(Integer.class.getName(), POINTER_SUM);
		type.addAttributeToType(Boolean.class.getName(), DO_WHILE);
		type.addAttributeToType(Boolean.class.getName(), "negated");
		type.addAttributeToType(Double.class.getName(), ASTConstants.POSITION);

		type = graph.createNodeType(AST_WHILE_IF);
		type.addAttributeToType(Integer.class.getName(), LOOP_NUMBER);
		type.addAttributeToType(Integer.class.getName(), POINTER_SUM);
		type.addAttributeToType(Double.class.getName(), ASTConstants.POSITION);

		type = graph.createNodeType(AST_WHILE_LOOP);
		type.addAttributeToType(Integer.class.getName(), LOOP_NUMBER);
		type.addAttributeToType(Integer.class.getName(), POINTER_SUM);
		type.addAttributeToType(Double.class.getName(), ASTConstants.POSITION);
	}

	@Override
	public IGraph extractPattern(IGraph graph) {
		int loopNumber = -1;

		// Trennung: While hat Conditon aber For hat KeyPhrase

		for (INode n : graph.getNodesOfType(graph.getNodeType("loop"))) {
			if (!n.getAttributeValue("type").equals("LOOP")) { //Change to not equal FOr
				loopNumber++;

				cbase = graph.createNode(graph.getNodeType(AST_WHILE_BASE));
				cbase.setAttributeValue(LOOP_NUMBER, loopNumber);
				cbase.setAttributeValue(POINTER_SUM, 0);
				cbase.setAttributeValue(DO_WHILE, false);
				cbase.setAttributeValue("negated", false);

				// until signals breaking loop
				if (((String) n.getAttributeValue("keyphrase").toString()).contains("until")) {
					cbase.setAttributeValue("negated", true);
				}
				if (n.getAttributeValue("type").toString().equals("ENDING")) {
					cbase.setAttributeValue(DO_WHILE, true);
				}

				cif = graph.createNode(graph.getNodeType(AST_WHILE_IF));
				cif.setAttributeValue(LOOP_NUMBER, loopNumber);
				cif.setAttributeValue(POINTER_SUM, 0);
				cwhile = graph.createNode(graph.getNodeType(AST_WHILE_LOOP));
				cwhile.setAttributeValue(LOOP_NUMBER, loopNumber);
				cwhile.setAttributeValue(POINTER_SUM, 0);

				graph.createArc(cbase, n, graph.getArcType(ASTConstants.AST_BASE_POINTER));
				graph.createArc(cbase, cif, graph.getArcType(AST_CHILD));
				graph.createArc(cbase, cwhile, graph.getArcType(AST_CHILD));

				//Condition
				INode loopnode = n;
				while (loopnode.getOutgoingArcsOfType(graph.getArcType("loopCondition")).size() > 0) {
					loopnode = loopnode.getOutgoingArcsOfType(graph.getArcType("loopCondition")).get(0).getTargetNode();
					graph.createArc(cif, loopnode, graph.getArcType(AST_POINTER));
					cif.setAttributeValue(POINTER_SUM, (int) cif.getAttributeValue(POINTER_SUM) + 1);
					cbase.setAttributeValue(POINTER_SUM, (int) cbase.getAttributeValue(POINTER_SUM) + 1);
				}

				// Hacky
				//				for (String s : ((String) n.getAttributeValue("dependentPhrases")).split(","))
				//					System.out.println(s);
				// Dependent Phrase
				loopnode = n.getOutgoingArcsOfType(graph.getArcType("dependentLoopAction")).get(0).getTargetNode();
				for (INode token : graph.getNodesOfType(graph.getNodeType("token"))) {
					if ((token.getAttributeValue("instructionNumber")).equals(loopnode.getAttributeValue("instructionNumber"))) {
						graph.createArc(cwhile, token, graph.getArcType(AST_POINTER));
						cwhile.setAttributeValue(POINTER_SUM, (int) cwhile.getAttributeValue(POINTER_SUM) + 1);
					}
				}

				//				for (int i = 0; i < ((String) n.getAttributeValue("dependentPhrases")).split(",").length - 1; i++) {
				//					graph.createArc(cwhile, loopnode, graph.getArcType(AST_POINTER));
				//					loopnode = loopnode.getOutgoingArcsOfType(graph.getArcType("relation")).get(0).getTargetNode();
				//					cwhile.setAttributeValue(POINTER_SUM, (int) cwhile.getAttributeValue(POINTER_SUM) + 1);
				//					cbase.setAttributeValue(POINTER_SUM, (int) cbase.getAttributeValue(POINTER_SUM) + 1);
				//				}

				// update rest of Pointersum
				//				graph.createArc(cwhile, loopnode, graph.getArcType(AST_POINTER));
				//				cwhile.setAttributeValue(POINTER_SUM, (int) cwhile.getAttributeValue(POINTER_SUM) + 1);
				//				cbase.setAttributeValue(POINTER_SUM, (int) cbase.getAttributeValue(POINTER_SUM) + 1);

				//				while (loopnode.getOutgoingArcsOfType(graph.getArcType("dependentLoopAction")).size() > 0) {
				//					loopnode = loopnode.getOutgoingArcsOfType(graph.getArcType("dependentLoopAction")).get(0)
				//							.getTargetNode();
				//					graph.createArc(cwhile, loopnode, graph.getArcType("AST-Pointer"));
				//				}
				//				;

			}
		}
		return graph;
	}

	@Override
	public boolean checkPattern(IGraph graph) {
		for (INode wi : graph.getNodesOfType(graph.getNodeType(AST_WHILE_IF))) {
			if (wi.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD_EXTERN)).isEmpty()) {
				INode error = graph.createNode(graph.getNodeType(ASTConstants.AST_ERROR));
				error.setAttributeValue("message", "While-If has no Condition. MethodStructure is missing");
				error.setAttributeValue("level", "error");
				error.setAttributeValue("type", ASTConstants.Errors.MISSING_STRUCTURE);
				graph.createArc(error, wi, graph.getArcType(ASTConstants.AST_ERROR_POINTER));
				return false;
			}
		}

		for (INode wl : graph.getNodesOfType(graph.getNodeType(AST_WHILE_LOOP))) {
			if (wl.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD_EXTERN)).isEmpty()) {
				INode error = graph.createNode(graph.getNodeType(ASTConstants.AST_ERROR));
				error.setAttributeValue("message", "While-Loop-Sequence is empty. MethodStructure may be missing");
				error.setAttributeValue("level", "warning");
				error.setAttributeValue("type", ASTConstants.Errors.EMPTY_SEQUENCE);
				graph.createArc(error, wl, graph.getArcType(ASTConstants.AST_ERROR_POINTER));
				return false;
			}
		}
		return true;
	}

}
