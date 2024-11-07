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

/*activite_educatif*/
@GetMapping("/activite_educative")
@CrossOrigin(origins = "http://localhost:4200")
public ResponseEntity<String> getActivite() {
    if (model != null) {
        String sparqlQuery = """
    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#>
    PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
    SELECT ?activite_educative ?niveauDifficulte ?dateDebut ?dateFin ?dureeAE ?descriptionActivite ?nomActivite ?nombreParticipantsMax ?objectif ?typeActivite
    WHERE {
        ?activite_educative rdf:type base:activite_educative .  
        ?activite_educative base:niveauDifficulte ?niveauDifficulte . 
        ?activite_educative base:dateDebut ?dateDebut . 
        ?activite_educative base:dateFin ?dateFin .  
        ?activite_educative base:dureeAE ?dureeAE . 
        ?activite_educative base:descriptionActivite ?descriptionActivite . 
        ?activite_educative base:nomActivite ?nomActivite . 
        ?activite_educative base:nombreParticipantsMax ?nombreParticipantsMax . 
        ?activite_educative base:objectif ?objectif .   
        ?activite_educative base:typeActivite ?typeActivite .   
       
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
                String activite_educative = solution.getResource("activite_educative").toString();
                String niveauDifficulte = solution.getLiteral("niveauDifficulte").getString();
                String dateDebut = solution.getLiteral("dateDebut").getString();
                String dateFin = solution.getLiteral("dateFin").getString();
               Literal dureeLiteral = solution.getLiteral("dureeAE");
                String descriptionActivite = solution.getLiteral("descriptionActivite").getString();
                String nomActivite = solution.getLiteral("nomActivite").getString();
                Literal participantLiteral = solution.getLiteral("nombreParticipantsMax");
                String objectif = solution.getLiteral("objectif").getString();
                String typeActivite = solution.getLiteral("typeActivite").getString();



                // Modify the 'age' value if needed (e.g., add 5 years to the age)
               int dureeAE = dureeLiteral.getInt();
                int nombreParticipantsMax = participantLiteral.getInt();


                // Create a JSONObject to represent the result
                JSONObject result = new JSONObject();
                result.put("activite_educative", activite_educative);
                result.put("niveauDifficulte", niveauDifficulte);
                result.put("dateDebut", dateDebut);
                result.put("dateFin", dateFin);
                result.put("dureeAE", dureeAE);// Add modified age
                result.put("descriptionActivite", descriptionActivite);
                result.put("nomActivite", nomActivite);
                result.put("nombreParticipantsMax", nombreParticipantsMax);
                result.put("objectif", objectif);
                result.put("typeActivite", typeActivite);

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


    @PostMapping("/activite_educative")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> addActivite(@RequestBody Map<String, Object> payload) {
        if (model != null) {
            try {
                String activiteId = "" + UUID.randomUUID();


                String updateQuery =
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                                "PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#>" +
                                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                                "INSERT DATA {" +
                                "base:" + activiteId + " rdf:type base:activite_educative ;" +
                                "base:niveauDifficulte \"" + payload.get("niveauDifficulte") + "\" ;" +
                                "base:dateDebut \"" + payload.get("dateDebut") + "\"^^xsd:dateTime ;" +
                                "base:dateFin \"" + payload.get("dateFin") + "\" ;" +
                                "base:dureeAE \"" + payload.get("dureeAE") + "\"^^xsd:integer ;" +
                                "base:descriptionActivite \"" + payload.get("descriptionActivite") + "\" ;" +
                                "base:nomActivite \"" + payload.get("nomActivite") + "\" ;" +
                                "base:nombreParticipantsMax \"" + payload.get("nombreParticipantsMax") + "\" ^^xsd:integer ;" +
                                "base:objectif \"" + payload.get("objectif") + "\" ;" +
                                "base:typeActivite \"" + payload.get("typeActivite") + "\" ;" +

                                "}";

                UpdateRequest updateRequest = UpdateFactory.create(updateQuery);
                Dataset dataset = DatasetFactory.create(model);
                UpdateProcessor processor = UpdateExecutionFactory.create(updateRequest, dataset);
                processor.execute();
                JenaEngine.saveModel(model, "data/hekkamel.owl");

                return new ResponseEntity<>("activite educative added successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error adding activite educative : " + e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @DeleteMapping("/activite_educative/{id}")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> deleteactivity(@PathVariable String id) {
        if (model != null) {
            try {
                // Ensure ID is properly formatted with a prefix (e.g., "base:")
                String fullId = "base:" + id;

                // Construct SPARQL Delete query using string concatenation
                String sparqlDelete =
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "DELETE { " +
                                "   " + fullId + " rdf:type base:activite_educative ; " +
                                "               base:niveauDifficulte ?oldniveauDifficulte ; " +
                                "               base:dateDebut ?olddateDebut ; " +
                                "               base:dateFin ?olddateFin ; " +
                                "               base:dureeAE ?olddureeAE ; " +
                                "               base:descriptionActivite ?olddescriptionActivite ; " +
                                "                       base:nomActivite ?oldniveauDifficulte ; " +
                                "               base:nombreParticipantsMax ?oldnombreParticipantsMax ; " +
                                "               base:objectif ?oldobjectif ; " +
                                "               base:typeActivite ?oldtypeActivite . " +


                                "} " +
                                "WHERE { " +
                                "   " + fullId + " rdf:type base:activite_educative ; " +
                                "               base:niveauDifficulte ?oldniveauDifficulte ; " +
                                "               base:dateDebut ?olddateDebut ; " +
                                "               base:dateFin ?olddateFin ; " +
                                "               base:dureeAE ?olddureeAE ; " +
                                "               base:descriptionActivite ?olddescriptionActivite ; " +
                                "                       base:nomActivite ?nomActivite ; " +
                                "               base:nombreParticipantsMax ?oldnombreParticipantsMax ; " +
                                "               base:objectif ?oldobjectif ; " +
                                "               base:typeActivite ?oldtypeActivite . " +
                                "}";

                // Execute the SPARQL delete query
                JenaEngine.executeUpdate(sparqlDelete, model);

                // Save the updated model to a file to persist changes
                JenaEngine.saveModel(model, "data/hekkamel.owl");

                return new ResponseEntity<>("activite educative deleted successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error deleting activite educative: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @PutMapping("/activite_educative/{id}")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> updateactivite(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        if (model != null) {
            try {
                String newniveauDifficulte = payload.get("niveauDifficulte").toString();
                String newdateDebut = payload.get("dateDebut").toString();
                String newdateFin = payload.get("dateFin").toString();
                int newdureeAE = Integer.parseInt(payload.get("dureeAE").toString());
                String newdescriptionActivite= payload.get("descriptionActivite").toString();
                String newnomActivite= payload.get("nomActivite").toString();
                int newnombreParticipantsMax = Integer.parseInt(payload.get("nombreParticipantsMax").toString());// Ensure age is parsed correctly as an integer

                String newobjectif= payload.get("objectif").toString();
                String newtypeActivite= payload.get("typeActivite").toString();

                // Ensure ID is properly formatted with a prefix (e.g., "base:")
                String fullId = "base:" + id;

                // Construct SPARQL Update query using plain string concatenation
                String sparqlUpdate =
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                                "DELETE { " +
                                "   " + fullId + "               base:niveauDifficulte ?oldniveauDifficulte ; " +
                                "               base:dateDebut ?olddateDebut ; " +
                                "               base:dateFin ?olddateFin ; " +
                                "               base:dureeAE ?olddureeAE ; " +
                                "               base:descriptionActivite ?olddescriptionActivite ; " +
                                "               base:nomActivite ?oldnomActivite ; " +
                                "               base:nombreParticipantsMax ?oldnombreParticipantsMax ; " +
                                "               base:objectif ?oldobjectif ; " +
                                "               base:typeActivite ?oldtypeActivite . " +
                                "} " +
                                "INSERT { " +
                                "   " + fullId + " base:niveauDifficulte \"" + newniveauDifficulte + "\" ; " +
                                "               base:dateDebut \"" + newdateDebut + "\"^^xsd:dateTime; " +
                                "               base:dateFin \"" + newdateFin + "\"^^xsd:dateTime; " +
                                "               base:dureeAE \"" + newdureeAE + "\"^^xsd:int ; " +
                                "               base:descriptionActivite \"" + newdescriptionActivite + "\"; " +
                                "               base:nomActivite \"" + newnomActivite + "\"; " +
                                "               base:nombreParticipantsMax \"" + newnombreParticipantsMax + "\"^^xsd:int ; " +
                                "               base:objectif \"" + newobjectif + "\"; " +
                                "               base:typeActivite \"" + newtypeActivite + "\". " +
                                "} " +
                                "WHERE { " +
                                "   " + fullId + "               base:niveauDifficulte ?oldniveauDifficulte ; " +
                                "               base:dateDebut ?olddateDebut ; " +
                                "               base:dateFin ?olddateFin ; " +
                                "               base:dureeAE ?olddureeAE ; " +
                                "               base:descriptionActivite ?olddescriptionActivite ; " +
                                "               base:nomActivite ?oldnomActivite ; " +
                                "               base:nombreParticipantsMax ?oldnombreParticipantsMax ; " +
                                "               base:objectif ?oldobjectif ; " +
                                "               base:typeActivite ?oldtypeActivite . " +
                                "}";

                // Execute the SPARQL update query
                JenaEngine.executeUpdate(sparqlUpdate, model);

                // Save the updated model to a file to persist changes
                JenaEngine.saveModel(model, "data/hekkamel.owl");

                return new ResponseEntity<>("activite educative updated successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error updating activite educative: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    /*Cours*/
    @GetMapping("/cours")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> getCours() {
        if (model != null) {
            String sparqlQuery = """
    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#>
    PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
    SELECT ?Cours ?credits ?duréeDuCours ?descriptionCours ?heuresEnseignement ?nomDuCours ?objectifDuCours ?prerequis
    WHERE {
        ?Cours rdf:type base:Cours .  
        ?Cours base:credits ?credits . 
        ?Cours base:duréeDuCours ?duréeDuCours . 
        ?Cours base:descriptionCours ?descriptionCours . 
        ?Cours base:heuresEnseignement ?heuresEnseignement .  
        ?Cours base:nomDuCours ?nomDuCours . 
        ?Cours base:objectifDuCours ?objectifDuCours . 
         ?Cours base:prerequis ?prerequis .   
    }
    LIMIT 10
""" ;

            Query query = QueryFactory.create(sparqlQuery);
            try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
                ResultSet results = qexec.execSelect();
                JSONArray jsonResults = new JSONArray();
                while (results.hasNext()) {
                    QuerySolution solution = results.nextSolution();
                    String cours = solution.getResource("Cours").toString();
                    Literal creditsLiteral = solution.getLiteral("credits");

                    Literal duréeDuCoursLiteral = solution.getLiteral("duréeDuCours");
                    String descriptionCours = solution.getLiteral("descriptionCours").getString();
                    Literal heuresEnseignementLiteral = solution.getLiteral("heuresEnseignement");
                    String nomDuCours = solution.getLiteral("nomDuCours").getString();
                    String objectifDuCours = solution.getLiteral("objectifDuCours").getString();
                    String prerequis = solution.getLiteral("prerequis").getString();

                    int credits = creditsLiteral.getInt();
                    int duréeDuCours = duréeDuCoursLiteral.getInt();
                    int heuresEnseignement = heuresEnseignementLiteral.getInt();


                    JSONObject result = new JSONObject();
                    result.put("Cours", cours);
                    result.put("credits", credits);
                    result.put("duréeDuCours", duréeDuCours);
                    result.put("descriptionCours", descriptionCours);
                    result.put("heuresEnseignement", heuresEnseignement);
                    result.put("nomDuCours", nomDuCours);
                    result.put("objectifDuCours", objectifDuCours);
                    result.put("prerequis", prerequis);

                    jsonResults.put(result);
                }

                String jsonString = jsonResults.toString();
                return new ResponseEntity<>(jsonString, HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error executing SPARQL query: " + e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PostMapping("/cours")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> addCours(@RequestBody Map<String, Object> payload) {
        if (model != null) {
            try {
                String coursId = "" + UUID.randomUUID();

                String updateQuery =
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                                "PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#>" +
                                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                                "INSERT DATA {" +
                                "base:" + coursId + " rdf:type base:Cours ;" +
                                "base:credits \"" + payload.get("credits") + "\"^^xsd:int ;" +
                                "base:duréeDuCours \"" + payload.get("duréeDuCours") + "\"^^xsd:int ;" +
                                "base:descriptionCours \"" + payload.get("descriptionCours") + "\" ;" +
                                "base:heuresEnseignement \"" + payload.get("heuresEnseignement") + "\"^^xsd:int ;" +
                                "base:nomDuCours \"" + payload.get("nomDuCours") + "\" ;" +
                                "base:objectifDuCours \"" + payload.get("objectifDuCours") + "\" ;" +
                                "base:prerequis \"" + payload.get("prerequis") + "\" ;" +
                                "}";

                System.out.println("SPARQL Update Query: " + updateQuery);  // Log the query

                UpdateRequest updateRequest = UpdateFactory.create(updateQuery);
                Dataset dataset = DatasetFactory.create(model);
                UpdateProcessor processor = UpdateExecutionFactory.create(updateRequest, dataset);
                processor.execute();

                // Save and reload the model to ensure persistence
                JenaEngine.saveModel(model, "data/hekkamel.owl");

                return new ResponseEntity<>("Cours added successfully", HttpStatus.OK);
            } catch (Exception e) {
                e.printStackTrace();
                return new ResponseEntity<>("Error adding cours: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @DeleteMapping("/cours/{id}")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> deleteCours(@PathVariable String id) {
        if (model != null) {
            try {
                String fullId = "base:" + id;

                String sparqlDelete =
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "DELETE { " +
                                "   " + fullId + " rdf:type base:Cours . " +
                                "   " + fullId + " base:credits ?oldcredits . " +
                                "   " + fullId + " base:duréeDuCours ?oldduréeDuCours . " +
                                "   " + fullId + " base:descriptionCours ?olddescriptionCours . " +
                                "   " + fullId + " base:heuresEnseignement ?oldheuresEnseignement . " +
                                "   " + fullId + " base:nomDuCours ?oldnomDuCours . " +
                                "   " + fullId + " base:objectifDuCours ?oldobjectifDuCours . " +
                                "   " + fullId + " base:prerequis ?oldprerequis . " +
                                "} " +
                                "WHERE { " +
                                "   " + fullId + " rdf:type base:Cours . " +
                                "   " + fullId + " base:credits ?oldcredits . " +
                                "   " + fullId + " base:duréeDuCours ?oldduréeDuCours . " +
                                "   " + fullId + " base:descriptionCours ?olddescriptionCours . " +
                                "   " + fullId + " base:heuresEnseignement ?oldheuresEnseignement . " +
                                "   " + fullId + " base:nomDuCours ?oldnomDuCours . " +
                                "   " + fullId + " base:objectifDuCours ?oldobjectifDuCours . " +
                                "   " + fullId + " base:prerequis ?oldprerequis . " +
                                "}";

                System.out.println("SPARQL Delete Query: " + sparqlDelete);  // Log the query

                JenaEngine.executeUpdate(sparqlDelete, model);

                // Save and reload model to ensure persistence
                JenaEngine.saveModel(model, "data/hekkamel.owl");
               //model = JenaEngine.loadModel("data/hekkamel.owl"); // Reload model

                return new ResponseEntity<>("Cours deleted successfully", HttpStatus.OK);
            } catch (Exception e) {
                e.printStackTrace();
                return new ResponseEntity<>("Error deleting cours: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PutMapping("/cours/{id}")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> updatecours(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        if (model != null) {
            try {
                int newCredits = Integer.parseInt(payload.get("credits").toString());
                int newDuréeDuCours = Integer.parseInt(payload.get("duréeDuCours").toString());
                String newDescriptionCours = payload.get("descriptionCours").toString();
                int newHeuresEnseignement = Integer.parseInt(payload.get("heuresEnseignement").toString());
                String newNomDuCours = payload.get("nomDuCours").toString();
                String newPrerequis = payload.get("prerequis").toString();

                // Ensure ID is properly formatted with a prefix (e.g., "base:")
                String fullId = "base:" + id;

                // Construct SPARQL Update query
                String sparqlUpdate =
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                                "DELETE { " +
                                "   " + fullId + " base:credits ?oldCredits ; " +
                                "               base:duréeDuCours ?oldDuréeDuCours ; " +
                                "               base:descriptionCours ?oldDescriptionCours ; " +
                                "               base:heuresEnseignement ?oldHeuresEnseignement ; " +
                                "               base:nomDuCours ?oldNomDuCours ; " +
                                "               base:prerequis ?oldPrerequis . " +
                                "} " +
                                "INSERT { " +
                                "   " + fullId + " base:credits \"" + newCredits + "\"^^xsd:int ; " +
                                "               base:duréeDuCours \"" + newDuréeDuCours + "\"^^xsd:int ; " +
                                "               base:descriptionCours \"" + newDescriptionCours + "\" ; " +
                                "               base:heuresEnseignement \"" + newHeuresEnseignement + "\"^^xsd:int ; " +
                                "               base:nomDuCours \"" + newNomDuCours + "\" ; " +
                                "               base:prerequis \"" + newPrerequis + "\" . " +
                                "} " +
                                "WHERE { " +
                                "   " + fullId + " base:credits ?oldCredits ; " +
                                "               base:duréeDuCours ?oldDuréeDuCours ; " +
                                "               base:descriptionCours ?oldDescriptionCours ; " +
                                "               base:heuresEnseignement ?oldHeuresEnseignement ; " +
                                "               base:nomDuCours ?oldNomDuCours ; " +
                                "               base:prerequis ?oldPrerequis . " +
                                "}";

                // Execute the SPARQL update query
                JenaEngine.executeUpdate(sparqlUpdate, model);

                // Save the updated model to a file to persist changes
                JenaEngine.saveModel(model, "data/hekkamel.owl");

                return new ResponseEntity<>("activite educative updated successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error updating activite educative: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    /*evaluation*/

    @GetMapping("/evaluation")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> getEvaluation() {
        if (model != null) {
            String sparqlQuery = """
    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#>
    PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
    SELECT ?Evaluation ?dateEvaluation ?descriptionEvaluation ?dureeEvaluation ?note 
    WHERE {
        ?Evaluation rdf:type base:Evaluation .  
        ?Evaluation base:dateEvaluation ?dateEvaluation . 
        ?Evaluation base:descriptionEvaluation ?descriptionEvaluation . 
        ?Evaluation base:dureeEvaluation ?dureeEvaluation .  
        ?Evaluation base:note ?note . 
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
                    String Evaluation = solution.getResource("Evaluation").toString();
                    String dateEvaluation = solution.getLiteral("dateEvaluation").getString();
                    String descriptionEvaluation = solution.getLiteral("descriptionEvaluation").getString();
                    Literal evaluationLiteral = solution.getLiteral("dureeEvaluation");
                    Literal noteLiteral = solution.getLiteral("note");




                    // Modify the 'age' value if needed (e.g., add 5 years to the age)
                    int dureeEvaluation = evaluationLiteral.getInt();
                    float note = noteLiteral.getInt();


                    // Create a JSONObject to represent the result
                    JSONObject result = new JSONObject();
                    result.put("Evaluation", Evaluation);
                    result.put("dateEvaluation", dateEvaluation);
                    result.put("descriptionEvaluation", descriptionEvaluation);
                    result.put("dureeEvaluation", dureeEvaluation);
                    result.put("note", note);// Add modified age


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


    @PostMapping("/evaluation")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> addEvaluation(@RequestBody Map<String, Object> payload) {
        if (model != null) {
            try {
                String activiteId = "" + UUID.randomUUID();


                String updateQuery =
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                                "PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#>" +
                                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                                "INSERT DATA {" +
                                "base:" + activiteId + " rdf:type base:Evaluation ;" +
                                "base:dateEvaluation \"" + payload.get("dateEvaluation") + "\"^^xsd:dateTime ;" +
                                "base:descriptionEvaluation \"" + payload.get("descriptionEvaluation") + "\";" +
                                "base:dureeEvaluation \"" + payload.get("dureeEvaluation") + "\"^^xsd:integer ;" +
                                "base:note \"" + payload.get("note") + "\"^^xsd:float ;" +
                                "}";

                UpdateRequest updateRequest = UpdateFactory.create(updateQuery);
                Dataset dataset = DatasetFactory.create(model);
                UpdateProcessor processor = UpdateExecutionFactory.create(updateRequest, dataset);
                processor.execute();
                JenaEngine.saveModel(model, "data/hekkamel.owl");

                return new ResponseEntity<>("activite educative added successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error adding activite educative : " + e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @DeleteMapping("/evaluation/{id}")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> deleteevaluation(@PathVariable String id) {
        if (model != null) {
            try {
                // Ensure ID is properly formatted with a prefix (e.g., "base:")
                String fullId = "base:" + id;

                // Construct SPARQL Delete query using string concatenation
                String sparqlDelete =
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "DELETE { " +
                                "   " + fullId + " rdf:type base:Evaluation ; " +
                                "               base:dateEvaluation ?olddateEvaluation ; " +
                                "               base:descriptionEvaluation ?olddescriptionEvaluation ; " +
                                "               base:dureeEvaluation ?olddureeEvaluation ; " +
                                "               base:note ?oldnote ; " +

                                "} " +
                                "WHERE { " +
                                "   " + fullId + " rdf:type base:Evaluation ; " +
                                "               base:dateEvaluation ?olddateEvaluation ; " +
                                "               base:descriptionEvaluation ?olddescriptionEvaluation ; " +
                                "               base:dureeEvaluation ?olddureeEvaluation ; " +
                                "               base:note ?oldnote ; " +
                                "}";

                // Execute the SPARQL delete query
                JenaEngine.executeUpdate(sparqlDelete, model);

                // Save the updated model to a file to persist changes
                JenaEngine.saveModel(model, "data/hekkamel.owl");

                return new ResponseEntity<>("activite educative deleted successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error deleting activite educative: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PutMapping("/evaluation/{id}")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> updateevaluation(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        if (model != null) {
            try {
                String newdateEvaluation = payload.get("dateEvaluation").toString();
                String newdescriptionEvaluation = payload.get("descriptionEvaluation").toString();

                int newdureeEvaluation = Integer.parseInt(payload.get("dureeEvaluation").toString());
                int newnote = Integer.parseInt(payload.get("note").toString());// Ensure age is parsed correctly as an integer



                // Ensure ID is properly formatted with a prefix (e.g., "base:")
                String fullId = "base:" + id;

                // Construct SPARQL Update query using plain string concatenation
                String sparqlUpdate =
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                                "DELETE { " +
                                "   " + fullId + "               base:dateEvaluation ?olddateEvaluation ; " +
                                "               base:descriptionEvaluation ?olddescriptionEvaluation ; " +
                                "               base:dureeEvaluation ?olddureeEvaluation ; " +
                                "               base:note ?oldnote ; " +
                                "} " +
                                "INSERT { " +
                                "   " + fullId + " base:dateEvaluation \"" + newdateEvaluation + "\"^^xsd:dateTime ; " +
                                "               base:descriptionEvaluation \"" + newdescriptionEvaluation + "\" ; " +
                                "               base:dureeEvaluation \"" + newdureeEvaluation + "\"^^xsd:integer; " +
                                "               base:note \"" + newnote + "\"^^xsd:float ; " +
                                "} " +
                                "WHERE { " +
                                "   " + fullId + "               base:dateEvaluation ?olddateEvaluation ; " +
                                "               base:descriptionEvaluation ?olddescriptionEvaluation ; " +
                                "               base:dureeEvaluation ?olddureeEvaluation ; " +
                                "               base:note ?oldnote ; " +
                                "}";

                // Execute the SPARQL update query
                JenaEngine.executeUpdate(sparqlUpdate, model);

                // Save the updated model to a file to persist changes
                JenaEngine.saveModel(model, "data/hekkamel.owl");

                return new ResponseEntity<>("activite educative updated successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error updating activite educative: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    /*instution*/

    @GetMapping("/institution")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> getInstitution() {
        if (model != null) {
            String sparqlQuery = """
    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#>
    PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
    SELECT ?Institution ?adresseInstitution ?nomInstitution ?numeroContact ?typeInstitution 
    WHERE {
        ?Institution rdf:type base:Institution .  
        ?Institution base:adresseInstitution ?adresseInstitution . 
        ?Institution base:nomInstitution ?nomInstitution . 
        ?Institution base:numeroContact ?numeroContact .  
        ?Institution base:typeInstitution ?typeInstitution . 
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
                    String Institution = solution.getResource("Institution").toString();
                    String adresseInstitution = solution.getLiteral("adresseInstitution").getString();
                    String nomInstitution = solution.getLiteral("nomInstitution").getString();
                    String numeroContact = solution.getLiteral("numeroContact").getString();
                    String typeInstitution = solution.getLiteral("typeInstitution").getString();







                    // Create a JSONObject to represent the result
                    JSONObject result = new JSONObject();
                    result.put("Institution", Institution);
                    result.put("adresseInstitution", adresseInstitution);
                    result.put("nomInstitution", nomInstitution);
                    result.put("numeroContact", numeroContact);
                    result.put("typeInstitution", typeInstitution);// Add modified age


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
    @PostMapping("/institution")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> addInstitution(@RequestBody Map<String, Object> payload) {
        if (model != null) {
            try {
                String activiteId = "" + UUID.randomUUID();


                String updateQuery =
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                                "PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#>" +
                                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                                "INSERT DATA {" +
                                "base:" + activiteId + " rdf:type base:Institution ;" +
                                "base:adresseInstitution \"" + payload.get("adresseInstitution") + "\" ;" +
                                "base:nomInstitution \"" + payload.get("nomInstitution") + "\";" +
                                "base:numeroContact \"" + payload.get("numeroContact") + "\" ;" +
                                "base:typeInstitution \"" + payload.get("typeInstitution") + "\" ;" +
                                "}";

                UpdateRequest updateRequest = UpdateFactory.create(updateQuery);
                Dataset dataset = DatasetFactory.create(model);
                UpdateProcessor processor = UpdateExecutionFactory.create(updateRequest, dataset);
                processor.execute();
                JenaEngine.saveModel(model, "data/hekkamel.owl");

                return new ResponseEntity<>("activite educative added successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error adding activite educative : " + e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @DeleteMapping("/institution/{id}")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> deleteInstitution(@PathVariable String id) {
        if (model != null) {
            try {
                // Ensure ID is properly formatted with a prefix (e.g., "base:")
                String fullId = "base:" + id;

                // Construct SPARQL Delete query using string concatenation
                String sparqlDelete =
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "DELETE { " +
                                "   " + fullId + " rdf:type base:Institution ; " +
                                "               base:adresseInstitution ?oldadresseInstitution ; " +
                                "               base:nomInstitution ?oldnomInstitution ; " +
                                "               base:numeroContact ?oldnumeroContact ; " +
                                "               base:typeInstitution ?oldtypeInstitution ; " +

                                "} " +
                                "WHERE { " +
                                "   " + fullId + " rdf:type base:Institution ; " +
                                "               base:adresseInstitution ?oldadresseInstitution ; " +
                                "               base:nomInstitution ?oldnomInstitution ; " +
                                "               base:numeroContact ?oldnumeroContact ; " +
                                "               base:typeInstitution ?oldtypeInstitution ; " +
                                "}";

                // Execute the SPARQL delete query
                JenaEngine.executeUpdate(sparqlDelete, model);

                // Save the updated model to a file to persist changes
                JenaEngine.saveModel(model, "data/hekkamel.owl");

                return new ResponseEntity<>("activite educative deleted successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error deleting activite educative: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PutMapping("/institution/{id}")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> updateInstitution(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        if (model != null) {
            try {
                String newadresseInstitution = payload.get("adresseInstitution").toString();
                String newnomInstitution = payload.get("nomInstitution").toString();
                String newnumeroContact = payload.get("numeroContact").toString();
                String newtypeInstitution = payload.get("typeInstitution").toString();



                // Ensure ID is properly formatted with a prefix (e.g., "base:")
                String fullId = "base:" + id;

                // Construct SPARQL Update query using plain string concatenation
                String sparqlUpdate =
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                                "DELETE { " +
                                "   " + fullId + "               base:adresseInstitution ?oldadresseInstitution; " +
                                "               base:nomInstitution ?oldnomInstitution; " +
                                "               base:numeroContact ?oldnumeroContact; " +
                                "               base:typeInstitution ?oldtypeInstitution; " +
                                "} " +
                                "INSERT { " +
                                "   " + fullId + " base:adresseInstitution \"" + newadresseInstitution + "\"; " +
                                "               base:nomInstitution \"" + newnomInstitution + "\" ; " +
                                "               base:numeroContact \"" + newnumeroContact + "\"; " +
                                "               base:typeInstitution \"" + newtypeInstitution + "\" ; " +
                                "} " +
                                "WHERE { " +
                                "   " + fullId + "           base:adresseInstitution ?oldadresseInstitution; " +
                                "               base:nomInstitution ?oldnomInstitution; " +
                                "               base:numeroContact ?oldnumeroContact; " +
                                "               base:typeInstitution ?oldtypeInstitution; " +
                                "}";

                // Execute the SPARQL update query
                JenaEngine.executeUpdate(sparqlUpdate, model);

                // Save the updated model to a file to persist changes
                JenaEngine.saveModel(model, "data/hekkamel.owl");

                return new ResponseEntity<>("activite educative updated successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error updating activite educative: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
