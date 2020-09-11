package edu.kit.ipd.parse.ast_synth.patterns;

import java.util.ArrayList;
import java.util.List;

import edu.kit.ipd.parse.luna.data.ast.ASTConstants;
import edu.kit.ipd.parse.luna.data.ast.IASTPattern;
import edu.kit.ipd.parse.luna.data.ast.tree.ASTBranch;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.graph.INodeType;

/**
 * @author Sebastian Weigelt
 * @author Viktor Kiesel
 */
public class ConditionFinder implements IASTPattern {

	private static final String AST_COND_BASE = ASTConstants.AST_COND_BASE;
	private static final String AST_COND_IF = ASTConstants.AST_COND_IF;
	private static final String AST_COND_ELSE = ASTConstants.AST_COND_ELSE;
	private static final String AST_COND_THEN = ASTConstants.AST_COND_THEN;
	private static final String CONDITION_NUMBER = ASTConstants.CONDITION_NUMBER;

	private static final String AST_CHILD = ASTConstants.AST_CHILD;
	private static final String AST_POINTER = ASTConstants.AST_POINTER;
	private static final String POINTER_SUM = ASTConstants.POINTER_SUM;

	ASTBranch cond;

	@Override
	public void init(IGraph graph, INode treeNode, INode symbols) {
		INodeType type = graph.createNodeType(AST_COND_BASE);
		type.addAttributeToType(Integer.class.getName(), CONDITION_NUMBER);
		type.addAttributeToType(Integer.class.getName(), POINTER_SUM);
		type.addAttributeToType(Double.class.getName(), ASTConstants.POSITION);

		type = graph.createNodeType(AST_COND_IF);
		type.addAttributeToType(Integer.class.getName(), CONDITION_NUMBER);
		type.addAttributeToType(Integer.class.getName(), POINTER_SUM);
		type.addAttributeToType(Double.class.getName(), ASTConstants.POSITION);

		type = graph.createNodeType(AST_COND_THEN);
		type.addAttributeToType(Integer.class.getName(), CONDITION_NUMBER);
		type.addAttributeToType(Integer.class.getName(), POINTER_SUM);
		type.addAttributeToType(Double.class.getName(), ASTConstants.POSITION);

		type = graph.createNodeType(AST_COND_ELSE);
		type.addAttributeToType(Integer.class.getName(), CONDITION_NUMBER);
		type.addAttributeToType(Integer.class.getName(), POINTER_SUM);
		type.addAttributeToType(Double.class.getName(), ASTConstants.POSITION);

	}

	@Override
	public IGraph extractPattern(IGraph graph) {
		cond = new ASTBranch();
		int conditionNumber = -1;
		List<INode> ifNodes = new ArrayList<>();
		List<INode> thenNodes = new ArrayList<>();
		List<INode> elseNodes = new ArrayList<>();
		INode cbase = null;
		INode cif = null;
		INode cthen = null;
		INode celse = null;

		for (INode n : graph.getNodesOfType(graph.getNodeType("token"))) {
			//			System.out.println(n.getAttributeValue("conditionNumber").getClass());
			if (conditionNumber != ((Integer) n.getAttributeValue(CONDITION_NUMBER)).intValue()) {
				conditionNumber = (int) n.getAttributeValue(CONDITION_NUMBER);
				if (conditionNumber != -1) {
					cbase = graph.createNode(graph.getNodeType(AST_COND_BASE));
					cbase.setAttributeValue(CONDITION_NUMBER, conditionNumber);
					cbase.setAttributeValue(POINTER_SUM, 0);

					cif = graph.createNode(graph.getNodeType(AST_COND_IF));
					cif.setAttributeValue(CONDITION_NUMBER, conditionNumber);
					cif.setAttributeValue(POINTER_SUM, 0);
					cthen = graph.createNode(graph.getNodeType(AST_COND_THEN));
					cthen.setAttributeValue(CONDITION_NUMBER, conditionNumber);
					cthen.setAttributeValue(POINTER_SUM, 0);
					celse = graph.createNode(graph.getNodeType(AST_COND_ELSE));
					celse.setAttributeValue(CONDITION_NUMBER, conditionNumber);
					celse.setAttributeValue(POINTER_SUM, 0);

					graph.createArc(cbase, cif, graph.getArcType(AST_CHILD));
					graph.createArc(cbase, cthen, graph.getArcType(AST_CHILD));
					graph.createArc(cbase, celse, graph.getArcType(AST_CHILD));
				}
			}
			switch ((String) n.getAttributeValue("commandType")) {
			case "IF_STATEMENT":
				graph.createArc(cif, n, graph.getArcType(AST_POINTER));
				cif.setAttributeValue(POINTER_SUM, (int) cif.getAttributeValue(POINTER_SUM) + 1);
				cbase.setAttributeValue(POINTER_SUM, (int) cbase.getAttributeValue(POINTER_SUM) + 1);
				break;
			case "ELSE_STATEMENT":
				graph.createArc(celse, n, graph.getArcType(AST_POINTER));
				celse.setAttributeValue(POINTER_SUM, (int) celse.getAttributeValue(POINTER_SUM) + 1);
				cbase.setAttributeValue(POINTER_SUM, (int) cbase.getAttributeValue(POINTER_SUM) + 1);
				break;
			case "THEN_STATEMENT":
				graph.createArc(cthen, n, graph.getArcType(AST_POINTER));
				cthen.setAttributeValue(POINTER_SUM, (int) cthen.getAttributeValue(POINTER_SUM) + 1);
				cbase.setAttributeValue(POINTER_SUM, (int) cbase.getAttributeValue(POINTER_SUM) + 1);
				break;
			case "INDEPENDENT_STATEMENT":
				//				if (cthen.getOutgoingArcsOfType(graph.getArcType("AST-Pointer")).isEmpty()) {
				//					graph.createArc(cthen, n, graph.getArcType("AST-Pointer"));
				//					 //Ensure it takes over and doesn't abort after one Pointer
				//					 //FIXME: Add DialogManagerWarning
				//				}
				break;
			default:
				break;
			}
			//			System.out.println(n.getAttributeValue("value"));
			//			System.out.println(n.getAttributeValue("commandType").getClass());

		}
		return graph;
	}

	@Override
	public String getBaseType() {

		return AST_COND_BASE;

	}

	@Override
	public boolean checkPattern(IGraph graph) {
		for (INode i : graph.getNodesOfType(graph.getNodeType(ASTConstants.AST_COND_IF))) {
			if (i.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD_EXTERN)).isEmpty()) {
				INode error = graph.createNode(graph.getNodeType(ASTConstants.AST_ERROR));
				error.setAttributeValue("message", "No Condition for COND-If found. MethodStructure is missing");
				error.setAttributeValue("level", "error");
				error.setAttributeValue("type", ASTConstants.Errors.MISSING_STRUCTURE);
				graph.createArc(error, i, graph.getArcType(ASTConstants.AST_ERROR_POINTER));
				return false;
			}
		}
		for (INode i : graph.getNodesOfType(graph.getNodeType(ASTConstants.AST_COND_THEN))) {
			if (i.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD_EXTERN)).isEmpty()) {
				INode error = graph.createNode(graph.getNodeType(ASTConstants.AST_ERROR));
				error.setAttributeValue("message", "Then-Sequence is empty. MethodStructure may be missing");
				error.setAttributeValue("level", "warning");
				error.setAttributeValue("type", ASTConstants.Errors.EMPTY_SEQUENCE);
				if (graph.getArcType(ASTConstants.AST_ERROR_POINTER) == null) {
					throw new NullPointerException("graph has no " + ASTConstants.AST_ERROR_POINTER);
				}
				graph.createArc(error, i, graph.getArcType(ASTConstants.AST_ERROR_POINTER));
				return false;
			}
		}
		return true;
	}

}
