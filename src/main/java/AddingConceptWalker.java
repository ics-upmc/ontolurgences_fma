import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import org.javatuples.Pair;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;
import org.semanticweb.owlapi.vocab.SKOSVocabulary;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

public class AddingConceptWalker extends OWLOntologyWalkerVisitor<Object> {
	private static Logger logger = Logger.getLogger(AddingConceptWalker.class.getName());
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
	
	private OWLObjectProperty localized;
	private OWLAnnotationProperty fmaNameProp;
	private OWLAnnotationProperty rdfsLabelProp;
	private OWLClass structureAnatomique;
	private OWLClass diagnostic;
	private OWLAnnotationProperty fmaIDProp;
	private OWLAnnotationProperty aaaProp;
	
	private Set<IRI> stopFatherIris;

	private OWLOntology fmaOnto;
	private Map<String, OWLClass> fmaIndex = null;
	private FmaPartOfModel classIsPartOf = null;
	
	private Multiset<IRI> stats = null;

	public AddingConceptWalker(OWLOntologyWalker walker, OWLOntology fmaOnto,
			Map<String, OWLClass> fmaIndex, FmaPartOfModel classIsPartOf) {
		super(walker);
		this.fmaOnto = fmaOnto;
		this.fmaIndex = fmaIndex;
		this.classIsPartOf = classIsPartOf;
		this.stopFatherIris = new HashSet<IRI>();
		this.stats = HashMultiset.create();
	}
	
	public String getStatsString() {
		StringBuilder buffer = new StringBuilder();
		OWLOntology ont = getCurrentOntology();
		OWLOntologyManager manager = ont.getOWLOntologyManager();
		OWLDataFactory df = manager.getOWLDataFactory();
		
		for(Entry<IRI> entrySet: stats.entrySet()) {
			OWLAnnotationValue labelAnnot = extractLabelFromFma(df.getOWLClass(entrySet.getElement()));
			String label = ((OWLLiteral) labelAnnot).getLiteral();

			buffer.append(label+": "+entrySet.getCount());
			buffer.append('\n');
		}
		return buffer.toString();
	}
	
	@Override
	public Object visit(OWLOntology ontology) {
		String ontolurgenceNS = Namespace.ONTOLURGENCES.getNS();
		String fmaNS = Namespace.FMA.getNS();
		OWLDataFactory df = ontology.getOWLOntologyManager().getOWLDataFactory();
		
		localized = df.getOWLObjectProperty(IRI.create(ontolurgenceNS, "localized"));

		fmaNameProp = df.getOWLAnnotationProperty(IRI.create(fmaNS, "name"));
		fmaIDProp = df.getOWLAnnotationProperty(IRI.create(fmaNS, "FMAID"));

		rdfsLabelProp = df.getRDFSLabel();

		structureAnatomique = df.getOWLClass(IRI.create(ontolurgenceNS,	"StructureAnatomique"));
		diagnostic = df.getOWLClass(IRI.create(ontolurgenceNS, "EtatPathologique"));

		stopFatherIris.add(IRI.create(fmaNS, "fma61775")); // Physical_anatomical_entity
		stopFatherIris.add(IRI.create(fmaNS, "fma67115")); // Non-physical_anatomical_entity
		stopFatherIris.add(IRI.create(fmaNS, "fma29733")); // General_anatomical_term
		
		aaaProp = df.getOWLAnnotationProperty(IRI.create(ontolurgenceNS + "aaa"));

		return super.visit(ontology);
	}

	private Pair<PourFmaType, Set<OWLAnnotation>> getPourFmaAnnotation(OWLClass currentClass) {
		OWLOntology ont = getCurrentOntology();
		OWLOntologyManager manager = ont.getOWLOntologyManager();
		OWLDataFactory df = manager.getOWLDataFactory();

		PourFmaType foundType = null;
		Set<OWLAnnotation> annots = new HashSet<>();
		for(PourFmaType fmaType: PourFmaType.values()) {
			OWLAnnotationProperty pourFma = df.getOWLAnnotationProperty(fmaType.getIRI());
			Set<OWLAnnotation> localAnnotations = currentClass.getAnnotations(ont, pourFma);
			if(!annots.isEmpty() && !localAnnotations.isEmpty()) {
				throw new IllegalArgumentException("pourFMAO and pourFMA in the same class: "+currentClass);
			} else if(!localAnnotations.isEmpty()){
				annots = localAnnotations;
				foundType = fmaType;
			}
		}
		return new Pair<>(foundType, annots);
	}

	@Override
	public Object visit(OWLClass desc) {
		OWLOntology ont = getCurrentOntology();
		OWLOntologyManager manager = ont.getOWLOntologyManager();
		OWLDataFactory df = manager.getOWLDataFactory();
		
		Pair<PourFmaType, Set<OWLAnnotation>> fmaAnnotations = getPourFmaAnnotation(desc);
		PourFmaType pourFmaType= fmaAnnotations.getValue0();
		Set<OWLAnnotation> annots = fmaAnnotations.getValue1();

		Set<OWLClass> fmaClasses = new HashSet<OWLClass>();
		for (OWLAnnotation annot : annots) {
			OWLAnnotationValue v = annot.getValue();
			OWLLiteral l = (OWLLiteral) v;
			String fmaId = l.getLiteral().trim();

			if (fmaId.isEmpty())
				continue;
			else if (!fmaIndex.containsKey(fmaId)) {
				logger.warning("FMA does not defined ID: " + fmaId + " (class "
						+ desc.toString() + ")");
			} else {
				OWLClass fmaCls = fmaIndex.get(fmaId);
				convertFmaConceptToOntolUrgenceConcept(fmaCls, pourFmaType);
				fmaClasses.add(fmaCls);

				OWLAnnotationValue labelAnnot = extractLabelFromFma(fmaCls);
				OWLAnnotation aaa_annot = df.getOWLAnnotation(aaaProp,
						labelAnnot);
				OWLAnnotationAssertionAxiom annotAxiom = df
						.getOWLAnnotationAssertionAxiom(desc.getIRI(),
								aaa_annot);
				manager.addAxiom(getCurrentOntology(), annotAxiom);
			}
		}
		if (!fmaClasses.isEmpty()) {
			OWLEquivalentClassesAxiom ax = getAxiom(desc, fmaClasses, pourFmaType);
			manager.addAxiom(ont, ax);
			if (fmaClasses.size() > 1)
				logger.warning("Many FMA ID: (class " + desc.toString()
						+ ")");
		}
		return super.visit(desc);
	}

	private OWLAnnotationValue extractLabelFromFma(OWLClass cls) {
		OWLOntology ont = getCurrentOntology();
		OWLOntologyManager manager = ont.getOWLOntologyManager();
		OWLDataFactory df = manager.getOWLDataFactory();
		
		OWLAnnotationValue labelAnnot = null;
		Set<OWLAnnotation> labels = cls.getAnnotations(fmaOnto, rdfsLabelProp);
		if (labels.isEmpty()) {
			labels = cls.getAnnotations(fmaOnto, fmaNameProp);
		}
		if (labels.isEmpty()) {
			logger.warning("Cannot determine label for: " + cls.getIRI());
			labelAnnot = df.getOWLLiteral(cls.getIRI().getFragment());
		} else {
			labelAnnot = labels.iterator().next().getValue();
		}
		return labelAnnot;
	}

	private Set<IRI> alreadyMadeFma = new HashSet<IRI>();

	public void convertFmaConceptToOntolUrgenceConcept(OWLClass cls, PourFmaType pourFmaType) {
		if (alreadyMadeFma.contains(cls.getIRI()))
			return;
		alreadyMadeFma.add(cls.getIRI());
		
		OWLOntology ont = getCurrentOntology();
		OWLOntologyManager manager = ont.getOWLOntologyManager();
		OWLDataFactory df = manager.getOWLDataFactory();		

		OWLAnnotationValue labelAnnot = extractLabelFromFma(cls);

		Set<OWLAnnotation> ids = cls.getAnnotations(fmaOnto, fmaIDProp);
		if (!ids.isEmpty()) {
			OWLAnnotation annot = ids.iterator().next();
			OWLAnnotationAssertionAxiom annotAxiom = df
					.getOWLAnnotationAssertionAxiom(cls.getIRI(), annot);
			manager.addAxiom(getCurrentOntology(), annotAxiom);
		}

		OWLAnnotationProperty prefLabelProp = df
				.getOWLAnnotationProperty(SKOSVocabulary.PREFLABEL.getIRI());
		OWLAnnotation annot = df.getOWLAnnotation(prefLabelProp, labelAnnot);
		OWLAnnotationAssertionAxiom annotAxiom = df
				.getOWLAnnotationAssertionAxiom(cls.getIRI(), annot);
		manager.addAxiom(getCurrentOntology(), annotAxiom);

		convertFMAConceptToDisease(cls, pourFmaType);

		Set<IRI> fathers = classIsPartOf.getUpConcepts(cls.getIRI());
		if (!fathers.isEmpty()) {
			for (IRI fatheriri : fathers) {
				OWLClass father = df.getOWLClass(fatheriri);
				convertFmaConceptToOntolUrgenceConcept(father, pourFmaType);
			}
		}

		for (OWLClassExpression fatherExp : cls.getSuperClasses(fmaOnto)) {
			if (fatherExp.isAnonymous())
				continue;
			OWLClass father = fatherExp.asOWLClass();

			if (stopFatherCriteria(father.getIRI(), cls.getIRI())) {
				OWLSubClassOfAxiom subAxiom = df.getOWLSubClassOfAxiom(cls,
						structureAnatomique);
				manager.addAxiom(getCurrentOntology(), subAxiom);
			} else {
				OWLSubClassOfAxiom subAxiom = df.getOWLSubClassOfAxiom(cls,
						father);
				manager.addAxiom(getCurrentOntology(), subAxiom);
				convertFmaConceptToOntolUrgenceConcept(father, pourFmaType);
			}
		}
	}

	private Set<IRI> alreadyMadeDisease = new HashSet<IRI>();

	public void convertFMAConceptToDisease(OWLClass cls, PourFmaType pourFmaType) {
		if (alreadyMadeDisease.contains(cls.getIRI()))
			return;
		alreadyMadeDisease.add(cls.getIRI());

		OWLOntology ont = getCurrentOntology();
		OWLOntologyManager manager = ont.getOWLOntologyManager();
		OWLDataFactory df = manager.getOWLDataFactory();
		
		String ontolurgenceNS = Namespace.ONTOLURGENCES.getNS();
		
		OWLAnnotationValue labelAnnot = extractLabelFromFma(cls);
		String label = ((OWLLiteral) labelAnnot).getLiteral();

		IRI clsiri = cls.getIRI();
		IRI newIri = IRI.create(ontolurgenceNS,
				"DiseaseOf" + clsiri.getFragment());
		OWLClass newCls = df.getOWLClass(newIri);

		OWLAnnotationValue newLabelAnnot = df.getOWLLiteral("Disease of "
				+ label, "en");
		OWLAnnotationProperty prefLabelProp = df
				.getOWLAnnotationProperty(SKOSVocabulary.PREFLABEL.getIRI());
		OWLAnnotation annot = df.getOWLAnnotation(prefLabelProp, newLabelAnnot);

		OWLAnnotationAssertionAxiom annotAxiom = df
				.getOWLAnnotationAssertionAxiom(newIri, annot);
		manager.addAxiom(getCurrentOntology(), annotAxiom);

		OWLEquivalentClassesAxiom ax = getAxiom(newCls,
				Collections.singleton(cls), pourFmaType);
		manager.addAxiom(getCurrentOntology(), ax);

		final Collection<PartOfType> partOfPriority = Arrays.asList(
				PartOfType.REGIONAL_PART,
				PartOfType.CONSTITUTIONAL_PART,
				PartOfType.MEMBER);
		
		Set<IRI> fathers = null;
		for(PartOfType type: partOfPriority) {
			Set<IRI> localFathers = classIsPartOf.getUpConcepts(cls.getIRI(), type);
			if(!localFathers.isEmpty()) {
				fathers = localFathers;
				break;
			}
		}
		if (fathers != null) {
			for (IRI father : fathers) {
				IRI fatherNewIri = IRI.create(ontolurgenceNS, "DiseaseOf"
						+ father.getFragment());
				OWLClass diseaseFather = df.getOWLClass(fatherNewIri);

				if (stopFatherCriteria(father, cls.getIRI())) {
					OWLSubClassOfAxiom subAxiom = df.getOWLSubClassOfAxiom(
							newCls, diagnostic);
					manager.addAxiom(getCurrentOntology(), subAxiom);
				} else {
					OWLSubClassOfAxiom subAxiom = df.getOWLSubClassOfAxiom(
							newCls, diseaseFather);
					manager.addAxiom(getCurrentOntology(), subAxiom);
					convertFMAConceptToDisease(df.getOWLClass(father), pourFmaType);
				}
			}
			return;
		}

		for (OWLClassExpression fatherExp : cls.getSuperClasses(fmaOnto)) {
			if (fatherExp.isAnonymous())
				continue;
			OWLClass father = fatherExp.asOWLClass();
			IRI fatherNewIri = IRI.create(ontolurgenceNS, "DiseaseOf"+ father.getIRI().getFragment());
			OWLClass diseaseFather = df.getOWLClass(fatherNewIri);

			if (stopFatherCriteria(father.getIRI(), cls.getIRI())) {
				OWLSubClassOfAxiom subAxiom = df.getOWLSubClassOfAxiom(newCls, diagnostic);
				manager.addAxiom(getCurrentOntology(), subAxiom);
			} else {
				OWLSubClassOfAxiom subAxiom = df.getOWLSubClassOfAxiom(newCls, diseaseFather);
				manager.addAxiom(getCurrentOntology(), subAxiom);
				convertFMAConceptToDisease(father, pourFmaType);
			}
		}
	}
	
	private boolean stopFatherCriteria(IRI father_iri, IRI myself_iri) {
		boolean isFatherNode = (myself_iri.getFragment().toLowerCase().contains("system") && 
				father_iri.getFragment().toLowerCase().contains("system")) ||
			   stopFatherIris.contains(father_iri);
		if(isFatherNode) stats.add(father_iri);
		return isFatherNode;
	}

	private OWLEquivalentClassesAxiom getAxiom(
			OWLClass ontolClass,
			Collection<OWLClass> fma_classes,
			PourFmaType fmaRelationType) {
		OWLOntology ont = getCurrentOntology();
		OWLOntologyManager manager = ont.getOWLOntologyManager();
		OWLDataFactory df = manager.getOWLDataFactory();		

		// pourFMAO
		// A and ((localized some L1) or (localized some L2)) and (localized only (L1 or L2))

		// pourFMAO
		// A and ((localized some L1) and (localized some L2)) and (localized only (L1 and L2))
		
		Set<OWLClassExpression> classesForFinalIntersection = ontolClass.getSuperClasses(getCurrentOntology());
		Set<OWLClassExpression> classesForSome = new HashSet<OWLClassExpression>();
		Set<OWLClassExpression> classesForOnly = new HashSet<OWLClassExpression>();
		for (OWLClass fma_class : fma_classes) {
			OWLClassExpression localizedSomeFMA = df.getOWLObjectSomeValuesFrom(localized, fma_class);
			classesForSome.add(localizedSomeFMA);
			classesForOnly.add(fma_class);
		}

		OWLClassExpression someAxiom = null;
		OWLClassExpression onlyAxiom = null;
		if(classesForSome.size() == 1) {
			someAxiom = classesForSome.iterator().next();
			onlyAxiom = df.getOWLObjectAllValuesFrom(localized, classesForOnly.iterator().next());
		}
		else {
			OWLClassExpression forOnlyAxiom = null;
			if(fmaRelationType == PourFmaType.pourFMA) {
				someAxiom = df.getOWLObjectIntersectionOf(classesForSome);
				forOnlyAxiom = df.getOWLObjectIntersectionOf(classesForOnly);
			} else if(fmaRelationType == PourFmaType.pourFMAO) {
				someAxiom = df.getOWLObjectUnionOf(classesForSome);
				forOnlyAxiom = df.getOWLObjectUnionOf(classesForOnly);
			} else {
				throw new IllegalArgumentException("Cannot handle this unkwon relation: "+fmaRelationType);
			}
			onlyAxiom = df.getOWLObjectAllValuesFrom(localized, forOnlyAxiom);
		}
		classesForFinalIntersection.add(someAxiom);
		classesForFinalIntersection.add(onlyAxiom);

		OWLClassExpression intersection = df.getOWLObjectIntersectionOf(classesForFinalIntersection);
		return df.getOWLEquivalentClassesAxiom(ontolClass, intersection);
	}
}
