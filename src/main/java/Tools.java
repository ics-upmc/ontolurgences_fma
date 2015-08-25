import java.io.File;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;


public class Tools {
	private static Logger logger = Logger.getLogger("FmaIndexer");
	static {
		Handler stdoutHandler = new StreamHandler(System.out, new Formatter() {
			@Override
			public String format(LogRecord record) {
				return record.getLevel().getName() + ": " + record.getMessage()	+ "\n";
			}
		});
		logger.addHandler(stdoutHandler);
		logger.setLevel(Level.ALL);
		logger.setUseParentHandlers(false);
	}

	private Tools() {}
	
	public static OWLOntology loadFromFile(OWLOntologyManager manager, File file) {
		try {
			logger.info("Load " + file.getAbsolutePath());
			IRI ontoFilename = IRI.create("file:///" + file.getAbsolutePath());
			return manager.loadOntologyFromOntologyDocument(ontoFilename);
		} catch (Exception e) {
			e.printStackTrace();
			logger.severe("Cannot load "+file.getAbsolutePath()+", exit now");
			System.exit(1);
		}
		return null; // Unreachable
	}

	public static void saveToFile(OWLOntology onto, File file) {
		try {
			IRI iriOutputFilename = IRI.create("file:///" + file.getAbsolutePath());
			OWLOntologyManager manager = onto.getOWLOntologyManager();
			manager.saveOntology(onto, iriOutputFilename);
			logger.info("Onto saved to " + file.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
			logger.severe("Cannot save "+file.getAbsolutePath()+", exit now");
			System.exit(1);
		}
	}

}
