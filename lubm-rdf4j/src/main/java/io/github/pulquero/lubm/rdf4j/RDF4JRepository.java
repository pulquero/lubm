package io.github.pulquero.lubm.rdf4j;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailFactory;

public class RDF4JRepository implements edu.lehigh.swat.bench.ubt.api.Repository {

	private Repository repo;
	private String ontology;

	public RDF4JRepository() {
	}

	public RDF4JRepository(Repository repo) {
		this.repo = repo;
	}

	public RDF4JRepository(SailFactory sailFactory) {
		Sail sail = sailFactory.getSail(sailFactory.getConfig());
		this.repo = new SailRepository(sail);
	}

	public void setRepository(Repository repo) {
		this.repo = repo;
	}

	public void open(String database) {
		configure(database);
		repo.initialize();

		if (ontology != null) {
			System.out.println(String.format("Loading ontology: %s", ontology));
			try(RepositoryConnection conn = repo.getConnection()) {
				conn.begin();
				conn.add(new URL(ontology), null, RDFFormat.RDFXML);
				conn.commit();
			} catch(IOException ioe) {
				throw new RuntimeException(ioe);
			}
		}
	}

	protected void configure(String database) {
		repo.setDataDir(new File(database));
	}

	public void setOntology(String ontology) {
		this.ontology = ontology;
	}

	public void clear() {
		try(RepositoryConnection conn = repo.getConnection()) {
			conn.begin();
			conn.clear();
			conn.commit();
		}
	}

	public void close() {
		repo.shutDown();
	}

	public boolean load(String dataPath) {
		int threadCount = Runtime.getRuntime().availableProcessors();
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		ExecutorCompletionService<Void> completionService = new ExecutorCompletionService<>(executor);
		final Object endOfQueue = new Object();
		BlockingQueue<Object> ingestQueue = new LinkedBlockingQueue<>();
		completionService.submit(() -> {
			Object next;
			try (RepositoryConnection conn = repo.getConnection()) {
				conn.begin();
				while ((next = ingestQueue.take()) != endOfQueue) {
					File f = (File)next;
					System.out.println(
							String.format("[%s] Loading file: %s", Thread.currentThread().getName(), f));
					conn.add(f, null, RDFFormat.RDFXML);
				}
				conn.commit();
			}
			return null;
		});

		try {
			File dir = new File(dataPath);
			for (File f : dir.listFiles()) {
				ingestQueue.put(f);
			}

			for (int i = 0; i < threadCount; i++) {
				ingestQueue.put(endOfQueue);
				completionService.take().get();
			}
		}
		catch (ExecutionException e) {
			if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException)e.getCause();
			}
			else {
				throw new RuntimeException(e.getCause());
			}
		}
		catch (InterruptedException e) {
			return false;
		}
		finally {
			executor.shutdownNow();
		}

		return true;
	}

	public edu.lehigh.swat.bench.ubt.api.QueryResult issueQuery(edu.lehigh.swat.bench.ubt.api.Query query) {
		String sparql = query.getString();
		System.out.println(String.format("Executing query:\n%s", sparql));
		try(RepositoryConnection conn = repo.getConnection()) {
			final List<?> res = Iterations.asList(conn.prepareTupleQuery(QueryLanguage.SPARQL, sparql).evaluate());
			return new edu.lehigh.swat.bench.ubt.api.QueryResult() {
				Iterator<?> iter = res.iterator();

				public long getNum() {
					return res.size();
				}

				public boolean next() {
					boolean hasNext = iter.hasNext();
					if(hasNext) {
						iter.next();
					}
					return hasNext;
				}
			};
		}
	}
}
