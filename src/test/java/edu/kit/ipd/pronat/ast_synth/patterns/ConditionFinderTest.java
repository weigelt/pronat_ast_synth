package edu.kit.ipd.pronat.ast_synth.patterns;

import edu.kit.ipd.parse.luna.graph.*;
import edu.kit.ipd.pronat.condition_detection.model.CommandType;
import org.junit.Test;

public class ConditionFinderTest {

	@Test
	public void testPresence() {
		IGraph pG = new ParseGraph();
		INodeType nt = pG.createNodeType("token");
		nt.addAttributeToType(CommandType.class.getName(), "commandType");
		INode node = pG.createNode(nt);
		node.setAttributeValue("commandType", CommandType.IF_STATEMENT);
		switch ((String) node.getAttributeValue("commandType").toString()) {
		case "IF":
			System.out.println((String) node.getAttributeValue("commandType").toString());
			break;
		default:
			System.out.println("fail");
			break;
		//		ConditionFinder cf = new ConditionFinder();
		//		cf.extractPattern(pG);
		}
	}
}
