package io.github.pulquero.lubm.halyard;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.eclipse.rdf4j.repository.sail.SailRepository;

import com.msd.gin.halyard.sail.HBaseSail;

import io.github.pulquero.lubm.rdf4j.RDF4JRepository;

public class HalyardRepository extends RDF4JRepository {

	protected void configure(String tableName) {
		Configuration config = HBaseConfiguration.create();
		HBaseSail sail = new HBaseSail(config, tableName, true, 0, true, 60, null, null);
		setRepository(new SailRepository(sail));
	}
}
