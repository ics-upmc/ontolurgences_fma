import org.semanticweb.owlapi.model.IRI;


public enum PartOfType {
	REGIONAL_PART,
	CONSTITUTIONAL_PART,
	MEMBER;
	
	public IRI getPartIRI() {
		String name = name().toLowerCase();
		return IRI.create(Namespace.FMA.getNS(), name);
	}
	
	public IRI getPartOfIRI() {
		String name = name().toLowerCase() + "_of";
		return IRI.create(Namespace.FMA.getNS(), name);		
	}
	
	public static boolean isSomePart(IRI part) {
		for(PartOfType type: values()) {
			if(type.getPartIRI().equals(part)) return true;
		}
		return false;
	}

	public static boolean isSomePartOf(IRI part) {
		for(PartOfType type: values()) {
			if(type.getPartOfIRI().equals(part)) return true;
		}
		return false;
	}
	
	public static PartOfType getTypeFromIri(IRI relation) {
		for(PartOfType type: values()) {
			if(type.getPartOfIRI().equals(relation)) return type;
			if(type.getPartIRI().equals(relation)) return type;
		}
		throw new IllegalArgumentException("Relation "+relation.toQuotedString()+" is not a part relation");	
	}
}
