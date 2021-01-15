package edu.kit.ipd.parse.ast_synth;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import edu.kit.ipd.parse.luna.data.MissingDataException;
import edu.kit.ipd.parse.luna.data.PipelineDataCastException;
import edu.kit.ipd.parse.luna.data.PostPipelineData;
import edu.kit.ipd.parse.luna.data.ast.ASTConstants;
import edu.kit.ipd.parse.luna.data.ast.IASTParentConfidenceCalculator;
import edu.kit.ipd.parse.luna.data.ast.IASTPattern;
import edu.kit.ipd.parse.luna.data.AbstractPipelineData;
import edu.kit.ipd.parse.luna.data.ast.visitor.IVisitor;
import edu.kit.ipd.parse.luna.graph.*;
import edu.kit.ipd.parse.luna.pipeline.IPipelineStage;
import edu.kit.ipd.parse.luna.pipeline.PipelineStageException;
import org.kohsuke.MetaInfServices;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.ipd.parse.luna.tools.ConfigManager;
import edu.kit.ipd.parse.luna.data.code.Method;

/**
 * @author Sebastian Weigelt
 * @author Viktor Kiesel
 */
@MetaInfServices(IPipelineStage.class)
public class ASTSynthStage implements IPipelineStage {

	private static final String NEW_METHOD = "method";
	private static final String AST_ERROR = ASTConstants.AST_ERROR;
	private static final String AST_ROOT = ASTConstants.AST_ROOT;

	public static final String ID = "ASTSynth";

	private static final Logger logger = LoggerFactory.getLogger(ASTSynthStage.class);

	Properties props;

	private PostPipelineData postPipelineData;
	private IGraph graph;
	ArrayList<IASTPattern> pipeline;
	IASTParentConfidenceCalculator pccalc;

	INode root;
	INode symbols;

	private String leafType = ASTConstants.AST_METHOD;

	//similarity funciton

	@Override
	public void init() {
		setConfigs();

		leafType = props.getProperty("IgnoreLeafType");

		Reflections reflections = new Reflections("edu.kit.ipd.parse.ast_synth");
		Set<Class<? extends IASTPattern>> classesPattern = reflections.getSubTypesOf(IASTPattern.class).stream()
				.filter(e -> e.getPackageName().startsWith("edu.kit.ipd.parse.ast_synth")).collect(Collectors.toSet());

		Set<IASTPattern> patternLoader = new HashSet<>();

		for (Class<? extends IASTPattern> aClass : classesPattern) {
			if (!Modifier.isAbstract(aClass.getModifiers())) {
				try {
					patternLoader.add(aClass.getConstructor().newInstance());
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
			}

		}

		pipeline = new ArrayList<>();

		for (IASTPattern p : patternLoader) {
			if (!leafType.equals(p.getBaseType())) {
				pipeline.add(p);
				logger.debug(p.getClass().getName() + " added");
			}
		}

		Set<Class<? extends IASTParentConfidenceCalculator>> classesStrat = reflections.getSubTypesOf(IASTParentConfidenceCalculator.class)
				.stream().filter(e -> e.getPackageName().startsWith("edu.kit.ipd.parse.ast_synth")).collect(Collectors.toSet());

		Set<IASTParentConfidenceCalculator> pccalcLoader = new HashSet<>();

		for (Class<? extends IASTParentConfidenceCalculator> aClass : classesStrat) {
			if (!Modifier.isAbstract(aClass.getModifiers())) {
				try {
					pccalcLoader.add(aClass.getConstructor().newInstance());
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
			}

		}

		for (IASTParentConfidenceCalculator p : pccalcLoader) {
			//		logger.debug("Check {} as ParentConfidenceCalculator for {}", p.getID(), props.getProperty("ParentConfidenceCalculator"));
			if (p.getID().equalsIgnoreCase(props.getProperty("ParentConfidenceCalculator").trim())) {
				pccalc = p;
			}
		}

		if (pccalc == null) {
			throw new NullPointerException("No ParentConfidenceCalculator for " + props.getProperty("ParentConfidenceCalculator")
					+ " loaded. Check the Config-String or Maven-Dependencies");
		}

		logger.debug("Added {} as ParentConfidenceCalculator", pccalc.getID());
		logger.debug("Added {} Patterns", pipeline.size());
	}

	@Override
	public void exec(AbstractPipelineData data) throws PipelineStageException {
		logger.debug("Started {}", ID);

		try {
			postPipelineData = data.asPostPipelineData();
			graph = postPipelineData.getGraph();
		} catch (PipelineDataCastException e) {
			// TODO: auto-generated!
			e.printStackTrace();
		} catch (MissingDataException e) {
			// TODO: auto-generated!
			e.printStackTrace();
		}

		if (root == null) { // Do agents reset?
			root = graph.createNode(graph.createNodeType(AST_ROOT));
			root.getType().addAttributeToType(Double.class.getName(), ASTConstants.POSITION);
			root.getType().addAttributeToType(Method.class.getName(), NEW_METHOD);

		}

		if (symbols == null) {
			symbols = graph.createNode(graph.createNodeType("Symboltable"));
			symbols.getType().addAttributeToType("edu.kit.ipd.parse.ast.Symbol", "symbols");
		}

		initNodesAndArcs();

		for (IASTPattern plugin : pipeline) {
			graph = plugin.extractPattern(graph);
		}

		buildTree();
		logger.debug("Starting position arranging as {}", props.getProperty("PositionAlgo"));

		if (props.getProperty("PositionAlgo").trim().equalsIgnoreCase("Lowest")) {
			getAndSetLowestPosition(root);
		} else if (props.getProperty("PositionAlgo").trim().equalsIgnoreCase("Average")) {
			getAndSetAveragePosition(root);
		} else {
			throw new NullPointerException("PositionAlgo " + props.getProperty("PositionAlgo") + " does not exist. Check Config.");
		}

		checkTree();
	}

	@Override
	public String getID() {
		return ID;
	}

	// creates basic types for everything
	private void initNodesAndArcs() {
		if (graph.getArcType(ASTConstants.AST_CHILD) == null) {
			IArcType arctype = graph.createArcType(ASTConstants.AST_CHILD); //confidence to direct child?
			//			arctype.addAttributeToType("double", "position");
			graph.createArcType(ASTConstants.AST_POINTER);
			graph.createArcType(ASTConstants.AST_BASE_POINTER);
			graph.createArcType(ASTConstants.AST_CHILD_EXTERN);
			// graph.getArcType(AST_CHILD_EXTERN).addAttributeToType(String.class.getName(),
			// "To");
			graph.getArcType(ASTConstants.AST_CHILD_EXTERN).addAttributeToType(Double.class.getName(), "confidence");
			// arctype.addAttributeToType("int", "position");

			INodeType error = graph.createNodeType(AST_ERROR);
			graph.createArcType(ASTConstants.AST_ERROR_POINTER);

			error.addAttributeToType(String.class.getName(), "level");
			error.addAttributeToType(String.class.getName(), "type");
			error.addAttributeToType(String.class.getName(), "message");
		}

		graph.createArc(root, symbols, graph.createArcType("symbols"));

		for (IASTPattern plugin : pipeline) {
			plugin.init(graph, root, symbols);
		}

	}

	private void buildTree() {
		pccalc.init(graph);
		for (IASTPattern p : pipeline) {
			if (p.getBaseType() != null) {
				for (INode n : graph.getNodesOfType(graph.getNodeType(p.getBaseType()))) {
					//find possible parents and calculates confidence for each node
					Map<INode, Double> confidence = null;
					confidence = pccalc.findPossibleParentAndCalculateConfidence(n);

					logger.debug("Confidence for " + n.getType().getName());
					INode ppNode = chooseNodeFromConfidenceMatrix(confidence);

					logger.debug("Nodes :" + ppNode + "\n ->" + n);
					if (ppNode != null) { //conntect ppNode to highest confidence node
						graph.createArc(ppNode, n, graph.getArcType(ASTConstants.AST_CHILD_EXTERN)).setAttributeValue("confidence",
								confidence.get(ppNode));
					} else { //connect to root (legacy code of SimplePCCalc prototype)
						//TODO: Check for Root in PCCalc and if still null then return error for Dialog?
						//CorePCC assumes null is root anyway, so kinda useless ...
						graph.createArc(root, n, graph.getArcType(ASTConstants.AST_CHILD_EXTERN)).setAttributeValue("confidence", 1.0);
					}
				}
			}
		}
	}

	private boolean checkTree() {
		if (root.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD_EXTERN)).isEmpty()) {
			INode error = graph.createNode(graph.getNodeType(AST_ERROR));
			error.setAttributeValue("message", "AST-Root has no children! This probably means, no methods were found");
			error.setAttributeValue("level", "error");
			error.setAttributeValue("type", ASTConstants.Errors.MISSING_STRUCTURE);
			graph.createArc(error, root, graph.getArcType(ASTConstants.AST_ERROR_POINTER));
		}

		boolean r = true;
		for (IASTPattern p : pipeline) {
			if (!p.checkPattern(graph)) {
				r = false;
			}
		}
		return r;
	}

	// Selection Function: Find Node with highest Confidence, but lowest Outgoing Pointer-Arcs
	private INode chooseNodeFromConfidenceMatrix(Map<INode, Double> confidence) {
		INode ppNode = null;
		for (INode possibleParent : confidence.keySet()) {
			logger.debug(possibleParent + ": " + confidence.get(possibleParent));
			if (ppNode == null) {
				ppNode = possibleParent;
			}
			if (confidence.get(possibleParent) >= confidence.get(ppNode)) {
				if (confidence.get(possibleParent) > confidence.get(ppNode)) { //highest confidence always taken
					ppNode = possibleParent;
					//if confidence is equal, pick the one, with smaller core
				} else if (possibleParent.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_POINTER)).size() < ppNode
						.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_POINTER)).size()) {
					ppNode = possibleParent;
				}
			}
		}
		return ppNode;
	}

	private Double getAndSetLowestPosition(INode node) {
		Double lowestPos = Double.MAX_VALUE;
		if (node.getAttributeValue(ASTConstants.POSITION) != null) {
			lowestPos = (Double) node.getAttributeValue(ASTConstants.POSITION);
		}

		//pick lowestPosition of intern children
		for (IArc arc : node.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD))) {
			Double position = (Double) arc.getTargetNode().getAttributeValue(ASTConstants.POSITION);
			logger.debug("checking child " + arc.getTargetNode());
			if (position == null) {
				position = getAndSetLowestPosition(arc.getTargetNode());
			}
			if (position >= 0) {
				lowestPos = Math.min(position, lowestPos);
			}
		}

		// pick lowest Position of extern children
		for (IArc arc : node.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD_EXTERN))) {
			Double position = (Double) arc.getTargetNode().getAttributeValue(ASTConstants.POSITION);
			logger.debug("checking extern " + arc.getTargetNode());
			if (position == null) {
				position = getAndSetLowestPosition(arc.getTargetNode());
			}
			if (position >= 0) {
				lowestPos = Math.min(position, lowestPos);
			}

		}
		node.setAttributeValue(ASTConstants.POSITION, lowestPos);
		return lowestPos;
	}

	private double getAndSetAveragePosition(INode node) {
		Double avgPos = null;
		if (node.getAttributeValue(ASTConstants.POSITION) != null) {
			avgPos = (Double) node.getAttributeValue(ASTConstants.POSITION);
		}

		//pick lowestPosition of intern children
		for (IArc arc : node.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD))) {
			logger.debug("checking child " + arc.getTargetNode());
			Double position = (Double) arc.getTargetNode().getAttributeValue(ASTConstants.POSITION);
			//
			if (position == null) {
				position = getAndSetAveragePosition(arc.getTargetNode()); //check children recursive until found
			}
			if (position >= 0) {
				if (avgPos == null) {
					avgPos = position;
				}
				avgPos = (avgPos + position) / 2.0;
			} else {
				//logger.error("Negative Position");
			}
		}

		// pick lowest Position of extern children
		for (IArc arc : node.getOutgoingArcsOfType(graph.getArcType(ASTConstants.AST_CHILD_EXTERN))) {
			logger.debug("checking extern " + arc.getTargetNode());
			Double position = (Double) arc.getTargetNode().getAttributeValue(ASTConstants.POSITION);
			if (position == null) {
				position = getAndSetAveragePosition(arc.getTargetNode());
			}
			if (position >= 0) {
				if (avgPos == null) {
					avgPos = position;
				}
				avgPos = (avgPos + position) / 2.0;
			}

		}
		if (avgPos == null) // Has no children. So can't take a position. Is empty anyway, so position doesn't matter.
		{
			avgPos = -1.0; // FIXME: problem is that it has influence on parent!
		}
		node.setAttributeValue(ASTConstants.POSITION, avgPos);
		return avgPos;
	}

	/**
	 * This method loads the configparameters.
	 */
	public void setConfigs() {
		props = ConfigManager.getConfiguration(ASTSynthStage.class);

		//ASTConstants.getProps();
	}

}

/*
 * Types: token alternative_token newPredicate loop concurrentAction
 * contextEntity contextAction contextConcept AST-Node Symboltable
 */
