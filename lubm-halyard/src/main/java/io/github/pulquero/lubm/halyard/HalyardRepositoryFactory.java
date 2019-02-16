package io.github.pulquero.lubm.halyard;

public class HalyardRepositoryFactory extends edu.lehigh.swat.bench.ubt.api.RepositoryFactory {

	public edu.lehigh.swat.bench.ubt.api.Repository create() {
		return new HalyardRepository();
	}
}
