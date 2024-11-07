package com.infinity.education;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.update.UpdateExecutionFactory;

import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;


@RestController
public class RestApi {

    private static final String NS = "http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#";
   ;
    Model model = JenaEngine.readModel("data/hekkamel.owl");



    private String resultSetToJson(ResultSet results) {
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{ \"results\": [");

        boolean first = true;
        while (results.hasNext()) {
            if (!first) {
                jsonBuilder.append(", ");
            }
            QuerySolution solution = results.nextSolution();
            jsonBuilder.append("{");

            // Loop through each variable in the result
            for (String var : results.getResultVars()) {
                RDFNode node = solution.get(var);
                jsonBuilder.append("\"").append(var).append("\": ");

                // Check if the node is null, handle it accordingly
                if (node != null) {
                    jsonBuilder.append("\"").append(node.toString()).append("\"");
                } else {
                    jsonBuilder.append("\"\""); // Default to empty string if node is null
                }
                jsonBuilder.append(", ");
            }
            // Remove the last comma and space
            jsonBuilder.setLength(jsonBuilder.length() - 2);
            jsonBuilder.append("}");
            first = false;
        }

        jsonBuilder.append("]}");
        return jsonBuilder.toString();
    }
    @GetMapping("/personne")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> getPersonne() {
        if (model != null) {
            String sparqlQuery = """
    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#>
    PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
    SELECT ?personne ?nom ?age
    WHERE {
        ?personne rdf:type base:Personne .  
        ?personne base:nom ?nom .   
        ?personne base:age ?age .  
    }
    LIMIT 10  # Optional: Add a limit if there are a lot of records
""";

            Query query = QueryFactory.create(sparqlQuery);
            try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
                ResultSet results = qexec.execSelect();

                // Convert the ResultSet to JSON and modify the 'age'
                JSONArray jsonResults = new JSONArray();
                while (results.hasNext()) {
                    QuerySolution solution = results.nextSolution();

                    // Get the values from the query solution
                    String personne = solution.getResource("personne").toString();
                    String nom = solution.getLiteral("nom").getString();
                    Literal ageLiteral = solution.getLiteral("age");

                    // Modify the 'age' value if needed (e.g., add 5 years to the age)
                    int age = ageLiteral.getInt();


                    // Create a JSONObject to represent the result
                    JSONObject result = new JSONObject();
                    result.put("personne", personne);
                    result.put("nom", nom);
                    result.put("age", age); // Add modified age

                    jsonResults.put(result);
                }

                // Convert the results to a JSON string
                String jsonString = jsonResults.toString();

                // Return the modified JSON as a response
                return new ResponseEntity<>(jsonString, HttpStatus.OK);

            } catch (Exception e) {
                return new ResponseEntity<>("Error executing SPARQL query: " + e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PostMapping("/personne")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> addPersonne(@RequestBody Map<String, Object> payload) {
        if (model != null) {
            try {
                String personneId = "" + UUID.randomUUID();


                String updateQuery =
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                                "PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#>" +
                                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                                "INSERT DATA {" +
                                "base:" + personneId + " rdf:type base:Personne ;" +
                                "base:nom \"" + payload.get("nom") + "\" ;" +
                                "base:age \"" + payload.get("age") + "\"^^xsd:int ." +
                                "}";

                UpdateRequest updateRequest = UpdateFactory.create(updateQuery);
                Dataset dataset = DatasetFactory.create(model);
                UpdateProcessor processor = UpdateExecutionFactory.create(updateRequest, dataset);
                processor.execute();
                JenaEngine.saveModel(model, "data/hekkamel.owl");

                return new ResponseEntity<>("Personne added successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error adding Personne: " + e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @PutMapping("/personne/{id}")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> updateInventory(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        if (model != null) {
            try {
                String newName = payload.get("nom").toString();
                int newAge = Integer.parseInt(payload.get("age").toString()); // Ensure age is parsed correctly as an integer

                // Ensure ID is properly formatted with a prefix (e.g., "base:")
                String fullId = "base:" + id;

                // Construct SPARQL Update query using plain string concatenation
                String sparqlUpdate =
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                                "DELETE { " +
                                "   " + fullId + " base:nom ?oldNom ; " +
                                "               base:age ?oldAge . " +
                                "} " +
                                "INSERT { " +
                                "   " + fullId + " base:nom \"" + newName + "\" ; " +
                                "               base:age \"" + newAge + "\"^^xsd:int . " +
                                "} " +
                                "WHERE { " +
                                "   " + fullId + " base:nom ?oldNom ; " +
                                "               base:age ?oldAge . " +
                                "}";

                // Execute the SPARQL update query
                JenaEngine.executeUpdate(sparqlUpdate, model);

                // Save the updated model to a file to persist changes
                JenaEngine.saveModel(model, "data/hekkamel.owl");

                return new ResponseEntity<>("Personne updated successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error updating Personne: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @DeleteMapping("/personne/{id}")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> deleteInventory(@PathVariable String id) {
        if (model != null) {
            try {
                // Ensure ID is properly formatted with a prefix (e.g., "base:")
                String fullId = "base:" + id;

                // Construct SPARQL Delete query using string concatenation
                String sparqlDelete =
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "DELETE { " +
                                "   " + fullId + " rdf:type base:Personne ; " +
                                "               base:nom ?oldNom ; " +
                                "               base:age ?oldAge . " +
                                "} " +
                                "WHERE { " +
                                "   " + fullId + " rdf:type base:Personne ; " +
                                "               base:nom ?oldNom ; " +
                                "               base:age ?oldAge . " +
                                "}";

                // Execute the SPARQL delete query
                JenaEngine.executeUpdate(sparqlDelete, model);

                // Save the updated model to a file to persist changes
                JenaEngine.saveModel(model, "data/hekkamel.owl");

                return new ResponseEntity<>("Personne deleted successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error deleting Personne: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }




}
