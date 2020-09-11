package edu.kit.ipd.parse.ast_synth.calc;

import java.util.*;

import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.ipd.parse.luna.data.ast.ASTConstants;
import edu.kit.ipd.parse.luna.data.ast.IASTParentConfidenceCalculator;
import edu.kit.ipd.parse.luna.graph.IArc;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.tools.ConfigManager;

/**
 * @author Sebastian Weigelt
 * @author Viktor Kiesel
 */
public class ASTCorePCCalc implements IASTParentConfidenceCalculator {

	class PathElement {
		INode node;
		double distance;
	}

	IGraph graph = null;

	Set<List<INode>> paths;

	private static final Logger logger = LoggerFactory.getLogger(ASTCorePCCalc.class);

	private String leafType = ASTConstants.AST_TEXT;

	Properties props;

	@Override public void init(IGraph graph) {
		props = ConfigManager.getConfiguration(ASTCorePCCalc.class);

		this.graph = graph; // init needed 
		paths = new HashSet<List<INode>>();
		if (!graph.getNodesOfType(graph.getNodeType(ASTConstants.AST_METHOD)).isEmpty()) {
			leafType = ASTConstants.AST_METHOD;
		}

		for (INode leaf : this.graph.getNodesOfType(graph.getNodeType(leafType))) {
			//logger.debug(""+leaf);
			Set<INode> core = findCore(leaf);
			Set<INode> structures = findStructures(core);
			List<INode> path = createPath(core, structures);
			paths.add(path);
		}
		// Ich möchte, hier eine Matrix? haben, in die ich nen Knoten rein werfen kann und 
		// ich bekomme dann daraus einen Vorgänger und die Konfidenz für diesen Knoten als Vater
		// Konfidenz: Distanz vom Vorgänger, also VaterStein - KindStein - Passt glaub nicht?
		// Erstmal den gesamten Graphen ausarbeiten 
		// Jedem Knoten mögliche Kerne zuweisen
		// Konfidenzen für alles berechnen

		// Show Paths:
		logger.debug("Paths:");
		for (List<INode> path : paths) {
			logger.debug("" + path);
		}
	}

	@Override
	public Map<INode, Double> findPossibleParentAndCalculateConfidence(INode n) {

		Map<INode, Double> confidence = new HashMap<>();
		//	confidence.put(findParent(n),1.0);
		//		List<? extends IArc> children = n.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD));
		//		if (children.isEmpty()) { // Empty: n is ActionNode (Method/Leaf)
		//		} else { // Others
		//			for (IArc child : children) {
		//				confidence = pccalc.findPossibleParentAndCalculateConfidence(child.getTargetNode());
		//			}
		//		}

		// Und dann separat Knoten rauspicken
		if (n.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_POINTER)).size() == 0
				&& (n.getIncomingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD)).size() > 0)) {
			// Special Handling of AST-Cond-Else just return it's AST-Cond-Base
			confidence.put(n.getIncomingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD)).get(0).getSourceNode(), 1.0);
		} else {
			confidence = findParent(n);
		}
		//Add AST-Root if no other parent possible
		if (confidence.keySet().isEmpty()) {
			confidence.put(graph.getNodesOfType(graph.getNodeType(ASTConstants.AST_ROOT)).get(0), 1.0);
		}

		return confidence;
	}

	// n should be Leaf of Tree = Method = CoreStruct = Core
	private Set<INode> findCore(INode n) {
		// finds all related structures to a specific leaf 
		Set<INode> core = new HashSet<INode>();
		for (IArc arc : n.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_POINTER))) { // dafuq?
			if (!core.contains(arc.getTargetNode())) {
				core.add(arc.getTargetNode());
			}
		}

		return core;
	}

	// Finds all structures that have overlap with core
	private Set<INode> findStructures(Set<INode> core) {
		Set<INode> structures = new HashSet<INode>();
		for (INode c : core) {
			System.out.println("Arc core: " + c);
			for (IArc arc : c.getIncomingArcsOfType(graph.getArcType(ASTConstants.AST_POINTER))) {
				System.out.println("ArcSource:" + arc.getSourceNode());
				if (!structures.contains(arc.getSourceNode())) {
					structures.add(arc.getSourceNode());
				}
			}
		}
		return structures;
	}

	private double calcSimilarity(Set<INode> core, INode structure) {
		Set<INode> structCore = new HashSet<>();
		if (structure.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_POINTER)).isEmpty()) {
			for (IArc childArc : structure.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD))) {
				for (IArc arc : childArc.getTargetNode().getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_POINTER))) {
					structCore.add(arc.getTargetNode());
				}
			}
		} else {
			for (IArc arc : structure.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_POINTER))) {
				structCore.add(arc.getTargetNode());
			}
		}
		List<INode> sortedCore = sortCore(core);
		List<INode> sortedStruct = sortCore(structCore);

		// TODO: Easy change for other Algos in config
		int distance = matrixLevenshteinDistance(sortedCore, sortedStruct);

		return distance;
	}

	private List<INode> createPath(Set<INode> core, Set<INode> structures) {
		//a path is nothing more than a sorted list
		// TODO: I probably have to prove this?

		//System.out.println("Structures:" + structures + " Core:" + core);
		List<INode> path = new ArrayList<>(structures);
		path.sort(new Comparator<>() { // Sort list based on similarity to core

			private double calculateScore(INode o, Set<INode> core) {
				double score = 0;
				double nodeScore = calcSimilarity(core, o) * Double.valueOf((String) props.getOrDefault("ScoreNodeEffect", 1));
				double baseScore = 0;
				if (!o.getIncomingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD)).isEmpty()) {
					INode base = o.getIncomingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD)).get(0).getSourceNode();
					baseScore = calcSimilarity(core, base) * Double.valueOf((String) props.getOrDefault("ScoreBaseEffect", 1));
				}
				double leafBonus = 0;
				if (o.getType().equals(graph.getNodeType(leafType))) {
					leafBonus = 1 * Double.valueOf((String) props.getOrDefault("ScoreLeafEffect", 1));
				}
				score = nodeScore + baseScore + leafBonus;
				logger.debug("Score for " + o + ":");
				logger.debug(score + " = NodeScore:" + nodeScore + " + BaseScore:" + baseScore + " + LeafBonus:" + leafBonus);

				return score;
			}

			@Override
			public int compare(INode o1, INode o2) { //Can cause problems if Structure and Corestructure have same size -> fixed by corestructure preference 
				logger.debug("Compare two Nodes in Path:");
				int c = Double.compare(calculateScore(o1, core), calculateScore(o2, core));

				return c;

				//				//If equal check for Leafs
				//				if (c == 0) { //Exception Handling for Leafs -  may as well always check first? No, because it may choose the wrong leaf if there is overlap
				//					if (o1.getType().equals(graph.getNodeType(leafType)) && !o2.getType().equals(graph.getNodeType(leafType)))
				//						return -1;
				//					if (o2.getType().equals(graph.getNodeType(leafType)) && !o1.getType().equals(graph.getNodeType(leafType)))
				//						return 1;
				//					// Check Base Structures: 
				//					INode base1 = o1.getIncomingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD)).get(0).getSourceNode();
				//					INode base2 = o2.getIncomingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD)).get(0).getSourceNode();
				//					c = Double.compare(calcSimilarity(core, base1), calcSimilarity(core, base2));
				//				}
				//				return c;
			}
		});
		//System.out.println("SortedPath:" + path);

		// Add base Structures to path
		Set<INode> bases = new HashSet<>();
		for (int n = 0; n < path.size(); n++) {
			for (IArc arc : path.get(n).getIncomingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD))) {
				if (!bases.contains(arc.getSourceNode())) {
					path.add(n + 1, arc.getSourceNode());
					bases.add(arc.getSourceNode());
				} else {
					path.remove(n--);
				}

				//				// if two childs of the same structure follow each other, remove the weaker one
				//				if (n + 2 < path.size() && path.get(n + 2).getIncomingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD)).size() > 0)
				//					if (path.get(n + 2).getIncomingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD)).get(0).getSourceNode()
				//							.equals(path.get(n + 1)))
				//						path.remove(n + 2);
				//				//could be changed to remove all second comings
			}

		}
		return path;

	}

	private Map<INode, Double> findParent(INode node) {
		Map<INode, Double> confidence = new HashMap<>();
		Map<INode, Integer> counter = new HashMap<>();
		for (List<INode> path : paths) {
			for (int i = 0; i < path.size(); i++) {
				if (path.get(i).equals(node)) {
					INode in;
					if (i + 1 < path.size()) { //is parent +1 or -1? => +1 as Leaf is first element
						in = path.get(i + 1);
						//					} else {
						//						in = (graph.getNodesOfType(graph.getNodeType(ASTConstants.AST_ROOT)).get(0)); // Is root or null
						//					}
						Integer c = counter.get(in);
						if (c == null) {
							c = 0;
						}
						counter.put(in, c + 1);
						System.out.println("Put in:" + node + " with " + (c + 1));
					}
				}
			}
		}

		//		if (counter.keySet().isEmpty())
		//			throw new NullPointerException("Structure not found in any Path. This can happen if tolerance is to low");

		int sum = 0;
		for (int i : counter.values()) {
			sum += i;
		}

		for (INode n : counter.keySet()) {
			confidence.put(n, counter.get(n) / (double) sum);
		}
		return confidence;
	}

	@Override
	public String getID() {
		return "Core";
	}

	// Sorts a core into a list by position
	private List<INode> sortCore(Set<INode> core) {
		List<INode> sortedCore = new ArrayList<>(core);
		sortedCore.sort(new Comparator<>() {

			@Override
			public int compare(INode o1, INode o2) {
				return Integer.compare((Integer) o1.getAttributeValue("position"), (Integer) o2.getAttributeValue("position"));
			}
		});

		return sortedCore;
	}

	// Transfered from: https://en.wikipedia.org/wiki/Levenshtein_distance#Computing_Levenshtein_distance 
	// Inefficient but our cores shouldn't be that big ... hopefully
	@Deprecated
	private int recursiveLevenshteinDistance(List<INode> s, int len_s, List<INode> t, int len_t) {
		int cost = 0;

		// base cases: empty Strings
		if (len_s == 0) {
			return len_t;
		}
		if (len_t == 0) {
			return len_s;
		}

		// test it last characters of strings match
		if (s.get(len_s - 1).equals(t.get(len_t))) {
			cost = 0;
		} else {
			cost = 1;
		}

		// return minimum of delete node from s, delete node from t, and delete node from both
		return Math.min(recursiveLevenshteinDistance(s, len_s - 1, t, len_t) + 1, Math.min(
				recursiveLevenshteinDistance(s, len_s, t, len_t - 1) + 1, recursiveLevenshteinDistance(s, len_s - 1, t, len_t - 1) + cost));
	}

	// Transfered from: https://en.wikipedia.org/wiki/Levenshtein_distance#Computing_Levenshtein_distance 
	// should be faster than recursive
	private int matrixLevenshteinDistance(List<INode> s, List<INode> t) {
		int[][] array = new int[s.size() + 1][t.size() + 1];

		//lacks cache optimisation?
		for (int i = 0; i < array.length; i++) {
			for (int j = 0; j < array[0].length; j++) {
				array[i][j] = 0;
			}
		}

		// Empty String cases
		for (int i = 1; i < s.size() + 1; i++) {
			array[i][0] = i;
		}
		for (int j = 1; j < t.size() + 1; j++) {
			array[0][j] = j;
		}

		int cost = 0;
		for (int j = 1; j < t.size() + 1; j++) {
			for (int i = 1; i < s.size() + 1; i++) {
				if (s.get(i - 1).equals(t.get(j - 1))) {
					cost = 0;
				} else {
					cost = 1;
				}

				array[i][j] = Math.min(array[i - 1][j] + 1, Math.min(array[i][j - 1] + 1, array[i - 1][j - 1] + cost));
			}
		}

		return array[s.size()][t.size()];
	}

}
