package io.github.pulquero.lubm.rdf4j;

public class RDF4JRepositoryFactory extends edu.lehigh.swat.bench.ubt.api.RepositoryFactory {

	public edu.lehigh.swat.bench.ubt.api.Repository create() {
		return new RDF4JRepository();
	}
}
