import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;

public class FmaIndexer extends OWLOntologyWalkerVisitor<Object> {
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

	private Map<String, OWLClass> fmaIndex;
	private OWLAnnotationProperty fmaIDProp;

	public FmaIndexer(OWLOntologyWalker walker) {
		super(walker);
		fmaIndex = new TreeMap<String, OWLClass>();
	}

	@Override
	public Object visit(OWLClass desc) {
		if(fmaIDProp == null) {
			OWLOntologyManager owlOntologyManager = getCurrentOntology().getOWLOntologyManager();
			OWLDataFactory df = owlOntologyManager.getOWLDataFactory();
			fmaIDProp = df.getOWLAnnotationProperty(IRI.create(Namespace.FMA.getNS(), "FMAID"));
		}
		
		Set<OWLAnnotation> annots = desc.getAnnotations(getCurrentOntology(), fmaIDProp);
		if (annots.size() > 1) {
			logger.warning("More than one FMAID for class: " + desc.toString());
		}
		if (annots.size() == 1) {
			OWLAnnotation annot = annots.iterator().next();
			OWLAnnotationValue v = annot.getValue();
			OWLLiteral l = (OWLLiteral) v;
			String fmaId = l.getLiteral();
			//logger.info("Found FMAID: " + fmaId + " for class "	+ desc.toString());
			fmaIndex.put(fmaId, desc);
		}
		return super.visit(desc);
	}
	
	public Map<String, OWLClass> getFmaIndex() {
		return fmaIndex;
	}
	
	public static Map<String, OWLClass> computeFmaIndex(OWLOntology fma) {
		OWLOntologyWalker fmaWalker = new OWLOntologyWalker(Collections.singleton(fma));
		FmaIndexer fmavisitor = new FmaIndexer(fmaWalker);
		fmaWalker.walkStructure(fmavisitor);
		return fmavisitor.getFmaIndex();
	}
}
