package cz.keiras.rdfExample;

import java.util.ArrayList;

import java.util.List;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;


/*
 * FUSEKI setup:
 * - get apache-jena-fuseki-2.*.* @ https://jena.apache.org/download/index.cgi
 * - extract to arbitrary directory and run fuseki-server.bat
 * - it runs @ http://localhost:3030/ by default
 * - create new dataset "dbm2" (if you choose other name, change SERVICE_URL string)
 * - dataset -> dbm2 -> upload files -> select data file (best results when using xml or ttl formats with prefixes specified)
 * - dataset -> dbm2 -> edit -> list current graphs -> default -> content of the rdf graph should be displayed
 * 
 * common issues:
 * - When uploading data files with improper file extension, fuseki will return an error and abort upload.
 *   Try changing file extension or converting to other serialization.
 * 
 * */


public class Fuseki {
	private static final String SERVICE_URL = "http://localhost:3030/dbm2";
	private static final String SERVICE_QUERY = SERVICE_URL + "/query";
	private static final String SERVICE_UPDATE = SERVICE_URL + "/update";
	
	public static void main(String[] arg){
		//get submodel containing specified node and its children up to depth 2 (X0->X1->X2)
		Model m = fusekiConstruct(getConstructQueryStringDepth2("http://mre.zcu.cz/id/bd083f7637190a544a0ff4c1474934a23b4fd11b"));
		for(Statement s : m.listStatements().toList())
			System.out.println(s);
		
		System.out.println("----------");
		
		// selects Patient nodes -> IBD node -> Therapy node
		System.out.println(fusekiSelect(getSelectTherapyOnIbdString()));
		// selects Patient nodes -> Therapy node
		System.out.println(fusekiSelect(getSelectTherapyOnPatientString()));

		System.out.println("----------");
		
		// change Therapy nodes to be direct descendants of Patient nodes
		fusekiUpdate(moveTherapyFromIbdToPatient());
		
		// selects Patient nodes -> IBD node -> Therapy node
		System.out.println(fusekiSelect(getSelectTherapyOnIbdString()));
		// selects Patient nodes -> Therapy node
		System.out.println(fusekiSelect(getSelectTherapyOnPatientString()));
	}

	// dealing with update queries (DELETE, INSERT)
    public static boolean fusekiUpdate(String q) {
        // Strinq q is query to be executed
        try {
            UpdateProcessor upp = UpdateExecutionFactory.createRemote(UpdateFactory.create(q), SERVICE_UPDATE);
            upp.execute();
            return true;
        } catch (QueryParseException ex) {
            // query is not valid
            System.out.println("invalid query: " + q);
            return false;
        }
    }

    // dealing with SELECT queries
    public static List<QuerySolution> fusekiSelect(String q) {
        // run Select SPARQL query
        QueryExecution qe = QueryExecutionFactory.sparqlService(SERVICE_QUERY, q);
        ResultSet results = qe.execSelect();

        // ResultSet needs to be parsed before closing QueryExecution
        List<QuerySolution> qs = new ArrayList<>();
        while (results.hasNext()) {
            qs.add(results.next());
        }
        qe.close();

        // return the results as list
        return qs;
    }

    // dealing with CONSTRUCT queries
    public static Model fusekiConstruct(String q) {
        // run Construct SPARQL query
        QueryExecution qe = QueryExecutionFactory.sparqlService(SERVICE_QUERY, q);
        Model results = qe.execConstruct();
        qe.close();

        return results;
    }

    public static String getConstructQueryStringDepth2(String rootNodeUri) {
        return "CONSTRUCT {"
                + "?root ?p1 ?o1. "
                + "?o1 ?p2 ?o2. "
                + "}"
                + "WHERE { "
                + "  {?root ?p1 ?o1 .} "
                + "  union "
                + "  { ?root ?p1 ?o1 . "
                + "    ?o1 ?p2 ?o2 .} "
                + "FILTER (?root = <" + rootNodeUri + ">) "
                + "}";
    }


    // update query to move therapy nodes to patient node rather than ibd node
    public static String moveTherapyFromIbdToPatient() {
        return "prefix ds:    <http://mre.zcu.cz/ontology/dasta.owl#>"
                + "prefix ibd:   <http://mre.zcu.cz/ontology/ibd.owl#>"
                + "prefix mreid: <http://mre.zcu.cz/id/>"
                + "delete{"
                + "?ibd ibd:hasTherapy ?therapy ."
                + "}"
                + "insert{"
                + "?patient ibd:hasTherapy ?therapy ."
                + "}"
                + "where{"
                + "?patient a ds:Patient ."
                + "?patient ibd:hasInflammatoryBowelDisease ?ibd ."
                + "?ibd a ibd:InflammatoryBowelDisease ."
                + "?therapy a ibd:Therapy ."
                + "?ibd ibd:hasTherapy ?therapy ."
                + "}";
    }
    
    // Selects Patient -> Therapy node
    public static String getSelectTherapyOnPatientString() {
        return "prefix ds:    <http://mre.zcu.cz/ontology/dasta.owl#>"
                + "prefix ibd:   <http://mre.zcu.cz/ontology/ibd.owl#>"
                + "prefix mreid: <http://mre.zcu.cz/id/>"
                + "select ?patient ?therapy "
                + "where{"
                + "?patient a ds:Patient ."
                + "?therapy a ibd:Therapy ."
                + "?patient ibd:hasTherapy ?therapy ."
                + "}";
    }
    
    // Selects Patient -> IBD node -> Therapy node 
    public static String getSelectTherapyOnIbdString() {
        return "prefix ds:    <http://mre.zcu.cz/ontology/dasta.owl#>"
                + "prefix ibd:   <http://mre.zcu.cz/ontology/ibd.owl#>"
                + "select ?patient ?ibd ?therapy "
                + "where{"
                + "?patient a ds:Patient ."
                + "?patient ibd:hasInflammatoryBowelDisease ?ibd ."
                + "?ibd a ibd:InflammatoryBowelDisease ."
                + "?therapy a ibd:Therapy ."
                + "?ibd ibd:hasTherapy ?therapy ."
                + "}";
    }
}
