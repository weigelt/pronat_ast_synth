package edu.kit.ipd.pronat.ast_synth.patterns;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.kit.ipd.parse.luna.graph.IArc;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.pronat.postpipelinedatamodel.ast.IASTPattern;
import edu.kit.ipd.pronat.postpipelinedatamodel.ast.Symbol;

/**
 * @author Sebastian Weigelt
 * @author Viktor Kiesel
 */
public class SymbolFinder implements IASTPattern {
	INode symbols;
	INode root;

	Map<String, Symbol> found;
	List<Symbol> needsReference;

	@Override
	public String getBaseType() {
		return null;
	}

	@Override
	public void init(IGraph graph, INode treeNode, INode symbols) {
		this.symbols = symbols;
		root = treeNode;
		found = new HashMap<>();
	}

	@Override
	public IGraph extractPattern(IGraph graph) {
		for (INode n : graph.getNodesOfType(graph.getNodeType("contextEntity"))) {
			Symbol s = extractSymbol(n, graph);
			found.put(s.getName(), s);
		}
		for (Symbol s : found.values()) // loop for iterateion?
		{
			if (s.getReference() == null) {
				found.put(s.getName(), extractSymbol(s.getNode(), graph));
			}
		}

		for (Symbol k : found.values()) {
			symbols.getType().addAttributeToType(Symbol.class.getName(), k.getName());
			symbols.setAttributeValue(k.getName(), k);
		}

		return graph;
	}

	private Symbol extractSymbol(INode n, IGraph graph) {
		String name = n.getAttributeValue("name").toString().replace(" ", "_").toLowerCase();
		// Asumption: Has exactly one contextRelation and OntologyIndividual is Class
		String type = "null";
		List<? extends IArc> arcs = n.getOutgoingArcsOfType(graph.getArcType("contextRelation"));
		Symbol s = new Symbol(name, type, n);
		if (arcs.size() > 0) {
			for (IArc a : arcs) {
				String relationType = (String) a.getAttributeValue("typeOfRelation");
				if (relationType.equals("entityConceptRelation")) {
					s.setType(a.getTargetNode().getAttributeValue("name").toString());
				} else if (relationType.equals("referentRelation")) {
					s.setType("reference");
					if (found.containsKey(a.getTargetNode().getAttributeValue("name").toString().toLowerCase())) {
						s.setReference(found.get(a.getTargetNode().getAttributeValue("name").toString().toLowerCase()));
					} else if (a.getTargetNode().getOutgoingArcsOfType(graph.getArcType("contextRelation")).isEmpty()) {
					}
				}
			}
		}
		return s;

	}

	@Override
	public boolean checkPattern(IGraph graph) {
		// TODO Auto-generated method stub
		return true;
	}

	//		for (IArc arc : graph.getArcsOfType(graph.getArcType("relationInAction"))) {
	//
	//			if (((BetweenRole) arc.getAttributeValue("type")).equals(BetweenRole.INSIDE_CHUNK)) {
	//				if ((int) arc.getSourceNode().getAttributeValue("predecessors") == 0) {
	//
	//					for (int i = 0; i < (int) arc.getSourceNode().getAttributeValue("successors"); i++)
	//						System.out.println(arc.getSourceNode().getAttributeValue("value"));
	//					arc.getSourceNode().getOutgoingArcsOfType(graph.getArcType("relationInAction"));
	//				}
	//			}
	//
	//			if (((BetweenRole) arc.getAttributeValue("type")).equals(BetweenRole.PREDICATE_TO_PARA)) {
	//				INode target = arc.getTargetNode();
	//				found.put(arc, new Symbol(target.getAttributeValue("value").toString(),
	//						target.getAttributeValue("value").toString(), target));
	//			}
	//		}

}
