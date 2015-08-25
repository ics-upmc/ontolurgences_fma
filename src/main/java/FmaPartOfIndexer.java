import java.util.Collections;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;

public class FmaPartOfIndexer extends OWLOntologyWalkerVisitor<Object> {

	private FmaPartOfModel classIsPartOf;

	public FmaPartOfIndexer(OWLOntologyWalker walker) {
		super(walker);
		classIsPartOf = new FmaPartOfModel();
	}

	@Override
	public Object visit(OWLObjectPropertyAssertionAxiom axiom) {
		OWLObjectPropertyExpression property = axiom.getProperty();
		OWLObjectProperty namedProperty = property.getNamedProperty();
		IRI propIri = namedProperty.getIRI();
		
		if (PartOfType.isSomePart(propIri)) {
			IRI objiri = axiom.getObject().asOWLNamedIndividual().getIRI();
			IRI subiri = axiom.getSubject().asOWLNamedIndividual().getIRI();
			PartOfType type = PartOfType.getTypeFromIri(propIri);			
			classIsPartOf.addConcept(subiri, objiri, type);
		}
		if (PartOfType.isSomePartOf(propIri)) {
			IRI objiri = axiom.getObject().asOWLNamedIndividual().getIRI();
			IRI subiri = axiom.getSubject().asOWLNamedIndividual().getIRI();
			PartOfType type = PartOfType.getTypeFromIri(propIri);
			classIsPartOf.addConcept(objiri, subiri, type);		}
		return super.visit(axiom);
	}

	public FmaPartOfModel getClassIsPartOf() {
		return classIsPartOf;
	}
	
	public static FmaPartOfModel buildPartOfModel(OWLOntology fma) {
		OWLOntologyWalker fmaPartWalker = new OWLOntologyWalker(Collections.singleton(fma));
		FmaPartOfIndexer fmapartvisitor = new FmaPartOfIndexer(fmaPartWalker);
		fmaPartWalker.walkStructure(fmapartvisitor);
		return fmapartvisitor.getClassIsPartOf();		
	}
}
