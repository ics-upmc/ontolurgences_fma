
public enum Namespace {
	FMA("http://purl.org/sig/fma/"),
	ONTOLURGENCES("http://doe-generated-ontology.com/UrgencesDMP#");

	private String namespace = null;
	private Namespace(String namespace) {
		this.namespace = namespace;
	}
	
	public String getNS() {
		return namespace;
	}

}
