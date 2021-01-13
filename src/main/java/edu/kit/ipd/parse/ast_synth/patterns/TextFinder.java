package edu.kit.ipd.parse.ast_synth.patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.kit.ipd.parse.luna.data.ast.ASTConstants;
import edu.kit.ipd.parse.luna.data.ast.IASTPattern;
import edu.kit.ipd.parse.luna.graph.IArc;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.graph.INodeType;
import edu.kit.ipd.parse.luna.tools.ConfigManager;

/**
 * @author Sebastian Weigelt
 * @author Viktor Kiesel
 */
public class TextFinder implements IASTPattern {

	private static final String POINTER_SUM = ASTConstants.POINTER_SUM;
	INode cbase;
	List<INode> commands;

	final String AST_BLOCK_CHILD = ASTConstants.AST_TEXT;

	String[] blacklist;

	@Override
	public String getBaseType() {
		return AST_BLOCK_CHILD;
	}

	@Override
	public void init(IGraph graph, INode treeNode, INode symbols) {

		Properties props = ConfigManager.getConfiguration(TextFinder.class);
		blacklist = props.getProperty("blacklist").split(",");

		commands = new ArrayList<>();

		INodeType type;

		type = graph.createNodeType(AST_BLOCK_CHILD);
		type.addAttributeToType(Double.class.getName(), ASTConstants.POSITION);
		type.addAttributeToType(Integer.class.getName(), POINTER_SUM);
		type.addAttributeToType(String.class.getName(), "text");
	}

	@Override
	public IGraph extractPattern(IGraph graph) {

		for (INode token : graph.getNodesOfType(graph.getNodeType("token"))) {
			boolean found = false;
			for (IArc arc : token.getIncomingArcsOfType(graph.getArcType(ASTConstants.AST_POINTER))) {
				if (arc.getSourceNode().getType().equals(graph.getNodeType(ASTConstants.AST_TEXT))) {
					found = true; //jump to next node if token has AST-Method

				}
			}

			// Create Error-Node if token has no AST-Method
			if (!found) {
				int instructionNumber = (int) token.getAttributeValue("instructionNumber");
				//				int conditionNum = (int) token.getAttributeValue("conditionNumber");
				String msg = "";
				if (token.getIncomingArcsOfType(graph.getArcType(ASTConstants.AST_ERROR_POINTER)).isEmpty()) {
					INode astnode = graph.createNode(graph.getNodeType(ASTConstants.AST_TEXT));
					for (INode t : graph.getNodesOfType(graph.getNodeType("token"))) {
						if ((int) t.getAttributeValue("instructionNumber") == instructionNumber) {
							//								&& (int) t.getAttributeValue("conditionNumber") == (int) token.getAttributeValue("conditionNumber")) {
							graph.createArc(astnode, t, graph.getArcType(ASTConstants.AST_POINTER));
							msg += t.getAttributeValue("value").toString();

							// add Coreference
							if (!t.getIncomingArcsOfType(graph.getArcType("reference")).isEmpty()) {
								INode ref = t.getIncomingArcsOfType(graph.getArcType("reference")).get(0).getSourceNode();
								if (ref.getType().equals(graph.getNodeType("contextEntity"))) {
									if (ref.getAttributeValue("typeOfEntity").toString().equals("Pronoun")) {
										if (!ref.getOutgoingArcsOfType(graph.getArcType("contextRelation")).isEmpty()) {

											// run through all posible entities and pick the one with the highest confidence
											String entity = null;
											double confidence = 0;
											for (IArc entityArc : ref.getOutgoingArcsOfType(graph.getArcType("contextRelation"))) {
												if (entityArc.getAttributeValue("confidence") != null) {
													if (confidence < (double) entityArc.getAttributeValue("confidence")) {
														confidence = (double) entityArc.getAttributeValue("confidence");
														entity = (String) entityArc.getTargetNode().getAttributeValue("name");
													}
												}
											}

											//Coreference confidence is null
											if (entity == null
													&& !ref.getOutgoingArcsOfType(graph.getArcType("contextRelation")).isEmpty()) {
												entity = (String) ref.getOutgoingArcsOfType(graph.getArcType("contextRelation")).get(0)
														.getTargetNode().getAttributeValue("name").toString();
											}

											if (entity != null) {
												msg += "(" + entity + ")";
											}
										}
									}
								}
							}
							msg += " ";

							// remove words like twice
							for (String s : blacklist) {
								msg = msg.replace(s, "");
							}

						}
					}
					astnode.setAttributeValue("text", msg);
					astnode.setAttributeValue(ASTConstants.POINTER_SUM,
							astnode.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_POINTER)).size());
					astnode.setAttributeValue(ASTConstants.POSITION, (double) instructionNumber);
				}

			}

			//		for (IArc a : graph.getArcsOfType(graph.getArcType("relationInAction"))) {
			//			System.out.println(a.getAttributeValue("type").toString());
			//			if (a.getAttributeValue("type").toString().equals("NEXT_ACTION")) {
			//				instructionNumber++;
			//				cbase = graph.createNode(graph.getNodeType("AST-Block-Base"));
			//				System.out.println("Check");
			//				INode n = a.getTargetNode();
			//				commands.add(graph.createNode(graph.getNodeType("AST-Block-Child")));
			//				commands.get(instructionNumber).setAttributeValue("position",
			//						(int) n.getAttributeValue("instructionNumber"));
			//				graph.createArc(cbase, commands.get(instructionNumber), graph.getArcType("AST-Child"));
			//			}
			//		}

			// Schwer

			//		double iterator = -1;
			//		int instructionNumber = -1;
			//		int conditionNumber = -2;
			//		String text = "";
			//		for (INode n : graph.getNodesOfType(graph.getNodeType("token"))) {
			//			if ((((int) n.getAttributeValue("instructionNumber")) != instructionNumber)
			//					|| (((int) n.getAttributeValue("conditionNumber")) != conditionNumber)) {
			//
			//				//conditionNumber is probably deprecated
			//				instructionNumber = (int) n.getAttributeValue("instructionNumber");
			//				conditionNumber = (int) n.getAttributeValue("conditionNumber");
			//				iterator++;
			//				commands.add(graph.createNode(graph.getNodeType(AST_BLOCK_CHILD)));
			//				commands.get((int) iterator).setAttributeValue(ASTConstants.POSITION, iterator);
			//				commands.get((int) iterator).setAttributeValue(POINTER_SUM, 0);
			//				// graph.createArc(cbase, commands.get(instructionNumber),
			//				// graph.getArcType("AST-Child"));
			//			}
			//			graph.createArc(commands.get((int) iterator), n, graph.getArcType(AST_POINTER));
			//
			//			// add word to text
			//			text += n.getAttributeValue("value");
			//
			//			// add Coreference
			//			if (!n.getIncomingArcsOfType(graph.getArcType("reference")).isEmpty()) {
			//				System.out.println(text);
			//				INode ref = n.getIncomingArcsOfType(graph.getArcType("reference")).get(0).getSourceNode();
			//				if (ref.getType().equals(graph.getNodeType("contextEntity")))
			//					if (!ref.getOutgoingArcsOfType(graph.getArcType("contextRelation")).isEmpty())
			//						text += "(" + ref.getOutgoingArcsOfType(graph.getArcType("contextRelation")).get(0).getTargetNode()
			//								.getAttributeValue("name") + ")";
			//
			//				// remove words like twice
			//				for (String s : blacklist)
			//					text = text.replace(s, "");
			//
			//				commands.get((int) iterator).setAttributeValue("text", commands.get((int) iterator).getAttributeValue("text") + text);
			//			}
			//			text += " ";
			//
			//			commands.get((int) iterator).setAttributeValue(POINTER_SUM,
			//					(int) commands.get((int) iterator).getAttributeValue(POINTER_SUM) + 1);
			//
			//		}

			//		for (INode n : graph.getNodesOfType(graph.getNodeType("AST-Block-Child"))) {
			//			List<? extends IArc> arcs = n.getOutgoingArcsOfType(graph.getArcType("AST-Pointer")).get(0).getTargetNode()
			//					.getOutgoingArcsOfType(graph.getArcType("AST-Pointer"));
			//			HashMap<INode, Integer> counter = new HashMap<>();
			//			for (IArc a : arcs) {
			//				if (!a.getSourceNode().getType().equals(graph.getNodeType("AST-Block-Child"))) {
			//					System.out.println(a.getSourceNode().toString());
			//					graph.createArc(n, a.getSourceNode(), graph.getArcType("AST-Block"));
			//				}
			//			} 
			//		 }

		}
		return graph;
	}

	@Override
	public boolean checkPattern(IGraph graph) {
		// TODO Auto-generated method stub
		return true;
	}

}
