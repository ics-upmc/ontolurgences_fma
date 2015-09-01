import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;

public class FmaReset {

	private static String output_folder = "/home/mazman/ontologies/OntolUrgences/";
	private static String ontolurgences = output_folder + "ontologie_fma_systeme_20150817.owl";
	private static String ontolurgences_reseted = output_folder + "ontologie_fma_systeme_20150817.reset.owl";

	private static String fma3NS = "http://purl.org/sig/fma/";
	private static String fma4NS = "http://purl.org/sig/ont/fma/";
	private static String ontolurgenceNS = "http://doe-generated-ontology.com/UrgencesDMP#";
	

	private static class Collector extends OWLOntologyWalkerVisitor<Object> {
		private Set<OWLClass> collected = new HashSet<OWLClass>();

		public Collector(OWLOntologyWalker walker) {
			super(walker);
		}

		public Set<OWLClass> getCollected() {
			return collected;
		}

		@Override
		public Object visit(OWLClass desc) {
			if (desc.getIRI().getNamespace().equals(fma3NS))
				collected.add(desc);
			if (desc.getIRI().getNamespace().equals(fma4NS))
				collected.add(desc);
			if (desc.getIRI().getFragment().startsWith("DiseaseOf"))
				collected.add(desc);

			return super.visit(desc);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			System.out.println("Adding defined to FMA ontology");
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			final OWLDataFactory df = manager.getOWLDataFactory();
			IRI iriFilename = IRI.create("file:///"+ ontolurgences);
			OWLOntology ontology = manager.loadOntologyFromOntologyDocument(iriFilename);

			OWLEntityRemover remover = new OWLEntityRemover(manager, Collections.singleton(ontology));

			OWLOntologyWalker walker = new OWLOntologyWalker(Collections.singleton(ontology));
			Collector visitor = new Collector(walker);
			walker.walkStructure(visitor);

			for(OWLClass cls : visitor.getCollected()) {
				cls.accept(remover);
			}
			
			OWLAnnotationProperty aaaProp = df.getOWLAnnotationProperty(IRI.create(ontolurgenceNS + "aaa"));
			aaaProp.accept(remover);

			List<OWLOntologyChange> changes = remover.getChanges();
			System.out.println("Will apply "+changes.size()+" changes");
			manager.applyChanges(changes);
			
			IRI iriOutputFilename = IRI.create("file:///" + ontolurgences_reseted);
			manager.saveOntology(ontology, iriOutputFilename);
			System.out.println("Finished!!!");

		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
