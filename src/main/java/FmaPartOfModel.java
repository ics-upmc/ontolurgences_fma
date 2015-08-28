import java.util.HashSet;
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

import com.google.common.collect.EnumMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;


public class FmaPartOfModel {
	private static Logger logger = Logger.getLogger(FmaPartOfModel.class.getName());
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

	private Map<IRI, Map<PartOfType, Set<IRI>>> index = null;
	private Multiset<PartOfType> stats = null;
	
	public FmaPartOfModel() {
		index = new TreeMap<IRI, Map<PartOfType, Set<IRI>>>();
		stats = EnumMultiset.create(PartOfType.class);
	}
	
	public void addConcept(IRI father, IRI son, PartOfType property) {
		// Get the right son map
		if(!index.containsKey(son)) {
			index.put(son, new TreeMap<PartOfType, Set<IRI>>());
		}
		Map<PartOfType, Set<IRI>> content = index.get(son);
		
		if(!content.containsKey(property)) {
			content.put(property, new HashSet<IRI>());
		}
		content.get(property).add(father);
		stats.add(property);
	}
	
	public int size() {
		return index.size();
	}
	
	public String getStatsString() {
		StringBuilder buffer = new StringBuilder();
		for(Entry<PartOfType> entrySet: stats.entrySet()) {
			buffer.append(entrySet);
			buffer.append('\n');
		}
		return buffer.toString();
	}

	public Set<IRI> getUpConcepts(IRI cls, PartOfType objectProperty) {
		if(!index.containsKey(cls)) {
			return new HashSet<IRI>();
		}
		if(!index.get(cls).containsKey(objectProperty)) {
			return new HashSet<IRI>();
		}
		return index.get(cls).get(objectProperty);
	}
	
	public Set<IRI> getUpConcepts(IRI cls) {
		Set<IRI> result = new HashSet<IRI>();
		if(index.containsKey(cls)) {
			Map<PartOfType, Set<IRI>> content = index.get(cls);
			for(Set<IRI> iris: content.values()) {
				result.addAll(iris);
			}
		}
		return result;
	}
}
