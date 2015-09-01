import java.util.Collections;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;

public class FmaPartOfIndexer extends OWLOntologyWalkerVisitor<Object> {

	private FmaPartOfModel classIsPartOf;

	public FmaPartOfIndexer(OWLOntologyWalker walker) {
		super(walker);
		classIsPartOf = new FmaPartOfModel();
	}

	@Override
	public Object visit(OWLSubClassOfAxiom axiom) {
		OWLClassExpression superClass = axiom.getSuperClass();
		OWLClassExpression subClass = axiom.getSubClass();

		if(superClass.isAnonymous() && !subClass.isAnonymous() && superClass instanceof OWLObjectSomeValuesFrom) {
			OWLObjectSomeValuesFrom someObject = (OWLObjectSomeValuesFrom) superClass;
			OWLObjectPropertyExpression property = someObject.getProperty();
			OWLObjectProperty namedProperty = property.getNamedProperty();
			IRI propIri = namedProperty.getIRI();

			if (PartOfType.isSomePart(propIri)) {
				IRI objiri = someObject.getFiller().asOWLClass().getIRI();
				IRI subiri = subClass.asOWLClass().getIRI();
				PartOfType type = PartOfType.getTypeFromIri(propIri);
				classIsPartOf.addConcept(subiri, objiri, type);
			}
			if (PartOfType.isSomePartOf(propIri)) {
				IRI objiri = someObject.getFiller().asOWLClass().getIRI();
				IRI subiri = subClass.asOWLClass().getIRI();
				PartOfType type = PartOfType.getTypeFromIri(propIri);
				classIsPartOf.addConcept(objiri, subiri, type);
			}
		}
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
