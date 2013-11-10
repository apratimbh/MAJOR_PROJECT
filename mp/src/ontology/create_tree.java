package ontology;
import java.io.BufferedReader;
import java.io.FileReader;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

public class create_tree {
	public static void main(String[] args) throws OWLOntologyCreationException
	{
		BufferedReader br;
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		IRI ontologyIRI = IRI.create("http://www.co-ode.org/ontologies/cellcycle_FUN.owl");
		IRI documentIRI = IRI.create("file:/host/ontologies/cellcycle_FUN.owl");
		SimpleIRIMapper mapper = new SimpleIRIMapper(ontologyIRI, documentIRI);
		manager.addIRIMapper(mapper);
		OWLOntology ontology = manager.createOntology(ontologyIRI);
		OWLDataFactory factory = manager.getOWLDataFactory();
		OWLClass g1,g2;
		try {
			br = new BufferedReader(new FileReader("E:/dataset/cellcycle_FUN.train.arff"));
			String line;
			while((line = br.readLine())!=null)
			{
				if(line.contains("hierarchical"))
				{
					line=line.replaceAll(" +", " ");
					line=line.split(" ")[3];
					String[] part=line.split(",");
					for(String gene : part)
					{
						if(gene.contains("/"))
						{
							gene=gene.replace("/",".");
							//System.out.println(gene);
							String top=gene.substring(0,gene.length()-3);
							String bottom=gene;
							g1=factory.getOWLClass(IRI.create(ontologyIRI + "#"+top));
							g2=factory.getOWLClass(IRI.create(ontologyIRI + "#"+bottom));
							OWLAxiom axiom = factory.getOWLSubClassOfAxiom(g2, g1);
							AddAxiom addAxiom = new AddAxiom(ontology, axiom);
							manager.applyChange(addAxiom);
						}
						else
						{
							g1=factory.getOWLClass(IRI.create(ontologyIRI + "#"+gene));
							g2=factory.getOWLClass(IRI.create(ontologyIRI + "#01"));
							OWLAxiom axiom = factory.getOWLDisjointClassesAxiom(g1,g2);
							AddAxiom addAxiom = new AddAxiom(ontology,axiom);
							manager.applyChange(addAxiom);
						}
					}
					break;
				}
			}
			for (OWLClass cls : ontology.getClassesInSignature()) {
				System.out.println("Referenced class: " + cls);
			}
			manager.saveOntology(ontology);
		} catch (Exception e) {
			System.out.println("Exception: "+(e.getMessage()));
		}
	}
}
