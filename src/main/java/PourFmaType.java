import org.semanticweb.owlapi.model.IRI;


public enum PourFmaType {
	pourFMA,
	pourFMAO;
	
	public IRI getIRI() {
		return IRI.create(Namespace.ONTOLURGENCES.getNS(), name());
	}
			
	public static PourFmaType getTypeFromIri(IRI relation) {
		for(PourFmaType type: values()) {
			if(type.getIRI().equals(relation)) return type;
		}
		throw new IllegalArgumentException("Relation "+relation.toQuotedString()+" is not a pourFMA relation");	
	}
}
