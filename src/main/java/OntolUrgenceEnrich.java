import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;


public class OntolUrgenceEnrich {
	private static Logger logger = Logger.getLogger(OntolUrgenceEnrich.class.getName());
	static {
		Handler stdoutHandler = new StreamHandler(System.out, new Formatter() {
			@Override
			public String format(LogRecord record) {
				return record.getLevel().getName() + ": " + record.getMessage() + "\n";
			}
		});
		logger.addHandler(stdoutHandler);
		logger.setLevel(Level.ALL);
		logger.setUseParentHandlers(false);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		
		logger.info("Load FMA");
		final File fmaPath = new File("/home/mazman/ontologies/fma_3.2.1_owl_file/fma3.2.owl");
		OWLOntology fma = Tools.loadFromFile(manager, fmaPath);

		logger.info("Index FMA");
		Map<String, OWLClass> fmaIndex = FmaIndexer.computeFmaIndex(fma);
		logger.info("I have indexed " + fmaIndex.size() + " concepts");

		logger.info("Index part/partOf");
		FmaPartOfModel classIsPartOf = FmaPartOfIndexer.buildPartOfModel(fma);
		logger.info("I have found " + classIsPartOf.size() + " concepts part of something");
		logger.info(classIsPartOf.getStatsString());
	
		logger.info("Load OntolUrgences");
		final File ontolurgencesPath = new File(
				"/home/mazman/Bureau/FMA/OntolUrgences_reset.owl");
		OWLOntology ontolurgences = Tools.loadFromFile(manager, ontolurgencesPath);		

		logger.info("Insert FMA class");
		OWLOntologyWalker ontolWalker = new OWLOntologyWalker(Collections.singleton(ontolurgences));
		OWLOntologyWalkerVisitor<Object> ontolvisitor = new AddingConceptWalker(
				ontolWalker, fma, fmaIndex, classIsPartOf);
		ontolWalker.walkStructure(ontolvisitor);

		final File outputFilePath = new File("/home/mazman/Bureau/FMA/OntolUrgences_new.owl");
		Tools.saveToFile(ontolurgences, outputFilePath);
		logger.info("Finished!!!");
	}

}
