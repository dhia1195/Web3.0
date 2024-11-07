package tn.sem.websem;

import java.io.OutputStream;

import java.util.*;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.update.UpdateAction;
import org.json.JSONArray;
import org.json.JSONObject;

@RestController
public class RestApi {

    private static final String NS = "http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#";
    Model model = JenaEngine.readModel("data/education.owl");


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

    @GetMapping("/plateformes")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> getAllPlateformes() {
        if (model != null) {
            try {
                String queryStr =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "SELECT ?id ?name ?subscriptionType ?type WHERE { " +
                                "  ?plateforme rdf:type ?type . " +
                                "  ?plateforme rescue:name ?name . " +
                                "  ?plateforme rescue:subscriptionType ?subscriptionType . " +
                                "  BIND(STR(?plateforme) AS ?id) . " +
                                "  FILTER (?type IN (rescue:teams, rescue:classroom, rescue:moodle, rescue:git)) " +
                                "}";

                Query query = QueryFactory.create(queryStr);
                QueryExecution qExec = QueryExecutionFactory.create(query, model);
                ResultSet results = qExec.execSelect();

                String jsonResponse = resultSetToJson(results);
                return new ResponseEntity<>(jsonResponse, HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error fetching plateformes: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    @PostMapping("/addPlateforme")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> addPlateforme(@RequestBody PlateformeDto plateformeDto) {
        if (model != null) {
            try {
                String plateformeId = "Plateforme_" + UUID.randomUUID().toString();
                String plateformeType = plateformeDto.getType(); // Assume the type is sent in the request

                String insertQuery =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "INSERT { " +
                                "  <" + plateformeId + "> rdf:type rescue:" + plateformeType + " . " + // Use the dynamic type
                                "  <" + plateformeId + "> rescue:name \"" + plateformeDto.getName() + "\" . " +
                                "  <" + plateformeId + "> rescue:subscriptionType \"" + plateformeDto.getSubscriptionType() + "\" . " +
                                "} WHERE { }";

                UpdateRequest updateRequest = UpdateFactory.create(insertQuery);
                UpdateAction.execute(updateRequest, model);

                JenaEngine.saveModel(model, "data/education.owl");

                return new ResponseEntity<>("Plateforme added successfully with ID: " + plateformeId, HttpStatus.CREATED);
            } catch (Exception e) {
                return new ResponseEntity<>("Error adding plateforme: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/modifyPlateforme/{id}")
    @CrossOrigin(origins = "http://localhost:4200", methods = {RequestMethod.PUT, RequestMethod.OPTIONS})
    public ResponseEntity<String> modifyPlateforme(@PathVariable String id, @RequestBody PlateformeDto plateformeDto) {
        if (model != null) {
            try {
                // Ensure the plateforme resource exists by ID
                Resource plateformeResource = model.getResource("http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#" + id);
                if (plateformeResource == null) {
                    return new ResponseEntity<>("Plateforme not found", HttpStatus.NOT_FOUND);
                }

                // Modify the SPARQL update query to include the 'type' field
                String modifyQuery =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "DELETE { " +
                                "  ?plateforme rescue:name ?oldName . " +
                                "  ?plateforme rescue:subscriptionType ?oldSubscriptionType . " +
                                "  ?plateforme rescue:type ?oldType . " +  // Delete the old type
                                "} " +
                                "INSERT { " +
                                "  ?plateforme rescue:name \"" + escapeSpecialCharacters(plateformeDto.getName()) + "\" . " +
                                "  ?plateforme rescue:subscriptionType \"" + escapeSpecialCharacters(plateformeDto.getSubscriptionType()) + "\" . " +
                                "  ?plateforme rescue:type <" + escapeSpecialCharacters(plateformeDto.getType()) + "> . " + // Insert the new type URI
                                "} " +
                                "WHERE { " +
                                "  BIND(<" + id + "> AS ?plateforme) ." +
                                "  OPTIONAL { ?plateforme rescue:name ?oldName } ." +
                                "  OPTIONAL { ?plateforme rescue:subscriptionType ?oldSubscriptionType } ." +
                                "  OPTIONAL { ?plateforme rescue:type ?oldType } ." +
                                "}";


                // Create and execute the update request
                UpdateRequest updateRequest = UpdateFactory.create(modifyQuery);
                UpdateAction.execute(updateRequest, model);

                // Save the updated model to the ontology file
                JenaEngine.saveModel(model, "data/education.owl");

                return new ResponseEntity<>("Plateforme modified successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error modifying plateforme: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String escapeSpecialCharacters(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"")
                .replace("\\", "\\\\");
    }

    @DeleteMapping("/delete/{id}")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> deletePlateforme(@PathVariable String id) {
        if (model != null) {
            try {
                Resource plateformeResource = model.getResource("http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#" + id);
                if (plateformeResource == null) {
                    return new ResponseEntity<>("Plateforme not found", HttpStatus.NOT_FOUND);
                }

                String modifyQuery =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "DELETE { " +
                                "  ?plateforme rescue:name ?oldName . " +
                                "  ?plateforme rescue:subscriptionType ?oldSubscriptionType . " +
                                "} " +

                                "WHERE { " +
                                "  BIND(<" + id + "> AS ?plateforme) ." + // Ensure the id is correctly formatted in the URI
                                "  OPTIONAL { ?plateforme rescue:name ?oldName } ." +
                                "  OPTIONAL { ?plateforme rescue:subscriptionType ?oldSubscriptionType } ." +
                                "}";


                // Create and execute the update request
                UpdateRequest updateRequest = UpdateFactory.create(modifyQuery);
                UpdateAction.execute(updateRequest, model);

                // Save the updated model to the ontology file
                JenaEngine.saveModel(model, "data/education.owl");

                // Check if the resource still exists after removal
                if (model.containsResource(plateformeResource)) {
                    return new ResponseEntity<>("Plateforme still exists in model after deletion", HttpStatus.INTERNAL_SERVER_ERROR);
                }

                return new ResponseEntity<>("Plateforme deleted successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error deleting plateforme: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @DeleteMapping("/delete/all")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> deleteAllPlateformes() {
        if (model != null) {
            try {
                // Define the SPARQL DELETE query to remove all plateforme resources
                String deleteQuery =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "DELETE { " +
                                "  ?plateforme rescue:name ?oldName . " +
                                "  ?plateforme rescue:subscriptionType ?oldSubscriptionType . " +
                                "} " +
                                "WHERE { " +
                                "  ?plateforme rescue:name ?oldName ." +
                                "  ?plateforme rescue:subscriptionType ?oldSubscriptionType ." +
                                "}";

                // Create and execute the delete request
                UpdateRequest updateRequest = UpdateFactory.create(deleteQuery);
                UpdateAction.execute(updateRequest, model);

                // Save the updated model to the ontology file
                JenaEngine.saveModel(model, "data/education.owl");

                return new ResponseEntity<>("All plateformes deleted successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error deleting all plateformes: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getAllTechnologies")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<List<Technologie_EducativeDto>> getAllTechnologies() {
        List<Technologie_EducativeDto> technologies = new ArrayList<>();

        if (model != null) {
            try {


                String queryStr =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "SELECT ?tech ?nom ?impactEnvironnemental WHERE { " +
                                "  ?tech a rescue:Technologie_Educative . " +
                                "  ?tech rescue:nom ?nom . " +
                                "  OPTIONAL { ?tech rescue:impactEnvironnemental ?impactEnvironnemental } " +
                                "}";

                Query query = QueryFactory.create(queryStr);
                try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
                    ResultSet results = qexec.execSelect();

                    while (results.hasNext()) {
                        QuerySolution solution = results.nextSolution();
                        String nom = solution.getLiteral("nom").getString();
                        String impactEnvironnemental = solution.contains("impactEnvironnemental") ?
                                solution.getLiteral("impactEnvironnemental").getString() : "";

                        Technologie_EducativeDto dto = new Technologie_EducativeDto(nom, impactEnvironnemental);
                        technologies.add(dto);
                    }
                }

                return new ResponseEntity<>(technologies, HttpStatus.OK);
            } catch (Exception e) {
                e.printStackTrace(); // Debugging
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            System.out.println("Model is null");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/addTechnologieEduc")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> addTechnologieEduc(@RequestBody Technologie_EducativeDto technologieDto) {
        if (model != null) {
            try {
                String technologyId = "Technologie_Educative_" + UUID.randomUUID();

                String insertQuery =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "INSERT { " +
                                "  <http://rescuefood.org/ontology/" + technologyId + "> a rescue:Technologie_Educative ; " +
                                "  rescue:nom \"" + technologieDto.getNom() + "\" ; " +
                                "  rescue:impactEnvironnemental \"" + technologieDto.getImpactEnvironnemental() + "\" . " +
                                "} WHERE {}";

                UpdateRequest updateRequest = UpdateFactory.create(insertQuery);
                UpdateAction.execute(updateRequest, model);

                JenaEngine.saveModel(model, "data/education.owl");

                return new ResponseEntity<>("Technologie Educative added successfully with ID: " + technologyId, HttpStatus.CREATED);
            } catch (Exception e) {
                e.printStackTrace(); // Debugging
                return new ResponseEntity<>("Error adding Technologie Educative: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/methodes")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> getAllMethodes() {
        if (model != null) {
            try {
                String queryStr =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "SELECT ?id ?nom ?duree WHERE { " +
                                "  ?methode rdf:type rescue:Methode . " +
                                "  ?methode rescue:nom ?nom . " +
                                "  ?methode rescue:duree ?duree . " +
                                "  BIND(STR(?methode) AS ?id) . " + // Include the ID (URI) as ?id
                                "}";

                Query query = QueryFactory.create(queryStr);
                QueryExecution qExec = QueryExecutionFactory.create(query, model);
                ResultSet results = qExec.execSelect();

                String jsonResponse = resultSetToJson(results);
                return new ResponseEntity<>(jsonResponse, HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error fetching plateformes: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/addMethode")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> addPlateforme(@RequestBody MethodeEnseignementDto methodeDto) {
        if (model != null) {
            try {
                String methodeId = "Methode_" + UUID.randomUUID().toString();

                String insertQuery =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "INSERT { " +
                                "  <" + methodeId + "> rdf:type rescue:Methode . " +
                                "  <" + methodeId + "> rescue:nom \"" + methodeDto.getNom() + "\" . " +
                                "  <" + methodeId + "> rescue:duree \"" + methodeDto.getDuree() + "\" . " +
                                "} WHERE { }"; // No need for a WHERE clause since we are inserting a new resource

                UpdateRequest updateRequest = UpdateFactory.create(insertQuery);
                UpdateAction.execute(updateRequest, model);

                JenaEngine.saveModel(model, "data/education.owl");

                return new ResponseEntity<>("Methode added successfully with ID: " + methodeId, HttpStatus.CREATED);
            } catch (Exception e) {
                return new ResponseEntity<>("Error adding methode: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PutMapping("/modifyMethode/{id}")
    @CrossOrigin(origins = "http://localhost:4200", methods = {RequestMethod.PUT, RequestMethod.OPTIONS})
    public ResponseEntity<String> modifyMethode(@PathVariable String id, @RequestBody MethodeEnseignementDto methodeDto) {
        if (model != null) {
            try {
                // Ensure the plateforme resource exists by ID
                Resource methodeResource = model.getResource("http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#" + id);
                if (methodeResource == null) {
                    return new ResponseEntity<>("Methode enseignement not found", HttpStatus.NOT_FOUND);
                }

                String modifyQuery =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "DELETE { " +
                                "  ?methode rescue:nom ?oldnom . " +
                                "  ?methode rescue:duree ?oldduree . " +
                                "} " +
                                "INSERT { " +
                                "  ?methode rescue:nom \"" + escapeSpecialCharacterss(methodeDto.getNom()) + "\" . " +
                                "  ?methode rescue:duree \"" + escapeSpecialCharacterss(methodeDto.getDuree()) + "\" . " +
                                "} " +
                                "WHERE { " +
                                "  BIND(<" + id + "> AS ?methode) ." + // Ensure the id is correctly formatted in the URI
                                "  OPTIONAL { ?methode rescue:nom ?oldnom } ." +
                                "  OPTIONAL { ?methode rescue:duree ?oldduree } ." +
                                "}";


                // Create and execute the update request
                UpdateRequest updateRequest = UpdateFactory.create(modifyQuery);
                UpdateAction.execute(updateRequest, model);

                // Save the updated model to the ontology file
                JenaEngine.saveModel(model, "data/education.owl");

                return new ResponseEntity<>("Methode modified successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error modifying methode: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    private String escapeSpecialCharacterss(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"")
                .replace("\\", "\\\\");
    }
    @DeleteMapping("/deleteMethode/{id}")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> deleteMethode(@PathVariable String id) {
        if (model != null) {
            try {
                Resource methodeResource = model.getResource("http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#" + id);
                if (methodeResource == null) {
                    return new ResponseEntity<>("Methode not found", HttpStatus.NOT_FOUND);
                }

                String modifyQuery =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "DELETE { " +
                                "  ?methode rescue:nom ?oldnom . " +
                                "  ?methode rescue:duree ?oldduree . " +
                                "} " +

                                "WHERE { " +
                                "  BIND(<" + id + "> AS ?methode) ." + // Ensure the id is correctly formatted in the URI
                                "  OPTIONAL { ?methode rescue:nom ?oldnom } ." +
                                "  OPTIONAL { ?methode rescue:duree ?oldduree } ." +
                                "}";


                // Create and execute the update request
                UpdateRequest updateRequest = UpdateFactory.create(modifyQuery);
                UpdateAction.execute(updateRequest, model);

                // Save the updated model to the ontology file
                JenaEngine.saveModel(model, "data/education.owl");

                // Check if the resource still exists after removal
                if (model.containsResource(methodeResource)) {
                    return new ResponseEntity<>("Methode still exists in model after deletion", HttpStatus.INTERNAL_SERVER_ERROR);
                }

                return new ResponseEntity<>("Methode deleted successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error deleting methode: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @DeleteMapping("/deleteMethodes/all")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> deleteAllMethodes() {
        if (model != null) {
            try {
                // Define the SPARQL DELETE query to remove all plateforme resources
                String deleteQuery =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "DELETE { " +
                                "  ?methode rescue:nom ?oldnom . " +
                                "  ?methode rescue:duree ?oldduree . " +
                                "} " +
                                "WHERE { " +
                                "  ?methode rescue:nom ?oldnom ." +
                                "  ?methode rescue:duree ?oldduree ." +
                                "}";

                // Create and execute the delete request
                UpdateRequest updateRequest = UpdateFactory.create(deleteQuery);
                UpdateAction.execute(updateRequest, model);

                // Save the updated model to the ontology file
                JenaEngine.saveModel(model, "data/education.owl");

                return new ResponseEntity<>("All methodes deleted successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error deleting all methodes: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @DeleteMapping("/deleteAllTechnologies")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> deleteAllTechnologies() {
        if (model != null) {
            try {
                // SPARQL query to delete all technologies
                String deleteQuery =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "DELETE WHERE { " +
                                "  ?tech a rescue:Technologie_Educative . " +
                                "}";

                // Execute the delete query
                UpdateRequest updateRequest = UpdateFactory.create(deleteQuery);
                UpdateAction.execute(updateRequest, model);

                // Save the updated model to the ontology
                JenaEngine.saveModel(model, "data/education.owl");

                return new ResponseEntity<>("All technologies have been deleted successfully.", HttpStatus.OK);
            } catch (Exception e) {
                e.printStackTrace(); // Debugging
                return new ResponseEntity<>("Error deleting all technologies: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    //personne


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
                JenaEngine.saveModel(model, "data/education.owl");

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
                JenaEngine.saveModel(model, "data/education.owl");

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
                JenaEngine.saveModel(model, "data/education.owl");

                return new ResponseEntity<>("Personne deleted successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error deleting Personne: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }


}



