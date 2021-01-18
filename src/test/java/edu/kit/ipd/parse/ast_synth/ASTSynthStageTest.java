package edu.kit.ipd.parse.ast_synth;

import org.junit.Assert;
import org.junit.Test;

public class ASTSynthStageTest {

	@Test
	public void initTest() {
		ASTSynthStage astSynthStage = new ASTSynthStage();
		astSynthStage.init();
		Assert.assertTrue(!astSynthStage.pipeline.isEmpty());
	}

	//	private GraphDrawer gd;
	//	private Pipeline<PrePipelineData> prepipe;
	//	private PrePipelineData ppd;
	//	private IGraph graph;
	//	private ShallowNLP snlp;
	//	private String input;
	//
	//	// Agents
	//	Wsd wsd;
	//	LoopDetectionMock loop;
	//	ConditionDetector dt;
	//	ActionRecognizerMock acRec;
	//	ContextAnalyzer context;
	//	CorefAnalyzer coref;
	//	ConcurrencyMock ccA;
	//	ASTSynth synth;
	//
	//	public void setUp() {
	//		ppd = new PrePipelineData();
	//		prepipe = new Pipeline<>(ppd);
	//
	//		snlp = new ShallowNLP();
	//		snlp.init();
	//		prepipe.addStage(snlp);
	//
	//		SRLabeler srl = new SRLabeler();
	//		srl.init();
	//		prepipe.addStage(srl);
	//
	//		NERTagger ner = new NERTagger();
	//		ner.init();
	//		prepipe.addStage(ner);
	//
	//		GraphBuilder gb = new GraphBuilder();
	//		gb.init();
	//		prepipe.addStage(gb);
	//
	//		// Agents
	//		wsd = new Wsd();
	//		wsd.init();
	//
	//		loop = new LoopDetectionMock();
	//		loop.init();
	//
	//		dt = new ConditionDetector();
	//		dt.init();
	//
	//		acRec = new ActionRecognizerMock();
	//		acRec.init();
	//
	//		ccA = new ConcurrencyMock();
	//		ccA.init();
	//
	//		context = new ContextAnalyzer();
	//		context.init();
	//
	//		coref = new CorefAnalyzer();
	//		coref.init();
	//
	//		synth = new ASTSynth();
	//		synth.init();
	//
	//		// Post Pipeline
	//		gd = new GraphDrawer();
	//		gd.init();
	//	}
	//
	//	public void piperun(String input) {
	//		// input = "Armar bring me the orange juice and then clean the table";
	//		ppd.setMainHypothesis(StringToHypothesis.stringToMainHypothesis(input, true));
	//		try {
	//			prepipe.run();
	//			// graph = ppd.getGraph();
	//		} catch (PipelineStageException e) {
	//			e.printStackTrace();
	//			fail("Pipeline Stage Error");
	//		}
	//
	//		agentsrun();
	//
	//		gd.exec(ppd);
	//	}
	//
	//	private void agentsrun() {
	//
	//		try {
	//			graph = ppd.getGraph();
	//
	//			acRec.setGraph(graph); // crash due to missing? SRL -> Has to be run first
	//			acRec.exec();
	//			graph = acRec.getGraph();
	//
	//			wsd.setGraph(graph);
	//			wsd.exec();
	//			graph = wsd.getGraph();
	//
	//			dt.setGraph(graph);
	//			dt.exec();
	//			graph = dt.getGraph();
	//
	//			loop.setGraph(graph);
	//			loop.exec();
	//			graph = loop.getGraph();
	//
	//			ccA.setGraph(graph);
	//			ccA.exec();
	//			graph = ccA.getGraph();
	//
	//			context.setGraph(graph);
	//			context.exec();
	//			graph = context.getGraph();
	//
	//			coref.setGraph(graph);
	//			coref.exec();
	//			graph = coref.getGraph();
	//
	//			synth.setGraph(graph);
	//			synth.exec();
	//			graph = synth.getGraph();
	//
	//			ppd.setGraph(graph);
	//
	//		} catch (MissingDataException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		}
	//
	//	}
	//
	//	//	@Test
	//	//	public void bringJuice() {
	//	//		setUp();
	//	//		File out = new File("./out/test/bringJuice.tex");
	//	//		gd.setFile(out);
	//	//		piperun("Armar if the juice is empty bring me vodka");
	//	//		// latexCompile(out); //doesn't work for single tests for whatever reason?
	//	//	}
	//	//
	//	//	@Test
	//	//	public void cleanOnIt() {
	//	//		setUp();
	//	//		File out = new File("./out/test/cleanOnIt.tex");
	//	//		gd.setFile(out);
	//	//		piperun("Armar get the orange juice and put it on the table");
	//	//		// latexCompile(out); //doesn't work for single tests for whatever reason?
	//	//	}
	//	//
	//	@Test
	//	public void ifJuice() {
	//		setUp();
	//		File out = new File("./out/test/ifjuice.tex");
	//		gd.setFile(out);
	//		piperun("Grab the cup if the vodka is empty open the fridge and close it again");
	//		// latexCompile(out); //doesn't work for single tests for whatever reason?
	//	}
	//
	//	@Test
	//	public void fridgeTwice() {
	//		setUp();
	//		File out = new File("./out/test/fridgetwice.tex");
	//		gd.setFile(out);
	//		piperun("Clean the table if the fridge is open grab some vodka and close the fridge");
	//		// latexCompile(out); //doesn't work for single tests for whatever reason?
	//	}
	//
	//	@Test
	//	public void cups() {
	//		setUp();
	//		File out = new File("./out/test/cups.tex");
	//		gd.setFile(out);
	//		piperun("Armar clean the table bring me the cups and put them on the table");
	//		// latexCompile(out); //doesn't work for single tests for whatever reason?
	//	}
	//
	//	@Test
	//	public void tableJuice() {
	//		setUp();
	//		File out = new File("./out/test/tablejuice.tex");
	//		gd.setFile(out);
	//		piperun("While the table is empty bring orange juice and put it on the table");
	//		// latexCompile(out); //doesn't work for single tests for whatever reason?
	//	}
	//
	//	@Test
	//	public void parallel() {
	//		setUp();
	//		File out = new File("./out/test/parallel.tex");
	//		gd.setFile(out);
	//		piperun("Armar go to the fridge in the meantime look at the recipe and bring coffee");
	//		// latexCompile(out); //doesn't work for single tests for whatever reason?
	//	}
	//	//	// @TestFactory
	//	//	public Collection<DynamicTest> fileTests() {
	//	//		Collection<DynamicTest> tests = new ArrayList<>();
	//	//		File dir = new File("./../../../parse_ba/");
	//	//		for (File f : dir.listFiles()) {
	//	//			DynamicTest t = DynamicTest.dynamicTest(f.getName(), new Executable() {
	//	//
	//	//				@Override
	//	//				public void execute() throws Throwable {
	//	//					setUp();
	//	//					BufferedReader br = new BufferedReader(new FileReader(f));
	//	//					String line;
	//	//					String text = "";
	//	//					while ((line = br.readLine()) != null)
	//	//						text += line + " ";
	//	//					br.close();
	//	//					System.out.println("======== " + f.getPath() + " ========");
	//	//					File out = new File("./out/" + f.getParentFile().getName() + "/" + f.getName() + ".tex");
	//	//					gd.setFile(out);
	//	//					System.out.println(text);
	//	//					piperun(text);
	//	//					// Not necessary, easier with bat and doesn't accidently forkbomb
	//	//					// latexCompile(out);
	//	//					/*
	//	//					 * new Thread(new Runnable() {
	//	//					 * 
	//	//					 * @Override public void run() { try { String command = "dir";
	//	//					 * Runtime.getRuntime().exec(command); } catch (IOException e) {
	//	//					 * e.printStackTrace(); } } }).start();
	//	//					 */
	//	//				}
	//	//			});
	//	//			tests.add(t);
	//	//		}
	//	//		return tests;
	//	//	}
	//
	//	private void latexCompile(File out) {
	//		// Latex somewhere else in bat or python? => Much easier in bat
	//		try {
	//			String command[] = { "cmd", "/c", "lualatex --output-directory=\"" + out.getParentFile().getCanonicalPath() + "/graphs/\" "
	//					+ "\"" + out.getCanonicalPath() + "\"" };
	//			System.out.println(command[2]);
	//			// WARNING: Spawns children silently!
	//			Process process = Runtime.getRuntime().exec(command);
	//		} catch (IOException e) {
	//			// TODO Auto-generated catch block
	//			e.printStackTrace();
	//		}
	//		//					BufferedReader stream = new BufferedReader(new InputStreamReader(process.getInputStream()));
	//		//					String output = "";
	//		//					while ((line = stream.readLine()) != null)
	//		//						output += line + "\n";
	//		//					System.out.println(output);
	//	}
	//
	//	private static class ActionRecognizerMock extends ActionRecognizer {
	//		public ActionRecognizerMock() {
	//			super();
	//		}
	//
	//		@Override
	//		public void exec() {
	//			super.exec();
	//		}
	//
	//	}
	//
	//	private static class LoopDetectionMock extends LoopDetectionAgent {
	//		public LoopDetectionMock() {
	//			super();
	//		}
	//
	//		@Override
	//		public void exec() {
	//			super.exec();
	//		}
	//
	//	}
	//
	//	private static class ConcurrencyMock extends ConcurrencyAgent {
	//		public ConcurrencyMock() {
	//			super();
	//		}
	//
	//		@Override
	//		public void exec() {
	//			super.exec();
	//		}
	//
	//	}

}
