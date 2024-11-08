package tn.sem.websem;

import java.io.FileOutputStream;
import java.io.IOException;
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
                                "SELECT ?tech ?nom ?impactEnvironnemental ?type WHERE { " +
                                "  ?tech a rescue:Technologie_Educative . " +
                                "  ?tech rescue:nom ?nom . " +
                                "  OPTIONAL { ?tech rescue:impactEnvironnemental ?impactEnvironnemental } . " +
                                "  OPTIONAL { ?tech a ?type } . " +
                                "}";

                Query query = QueryFactory.create(queryStr);
                try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
                    ResultSet results = qexec.execSelect();

                    while (results.hasNext()) {
                        QuerySolution solution = results.nextSolution();
                        String nom = solution.getLiteral("nom").getString();
                        String impactEnvironnemental = solution.contains("impactEnvironnemental") ?
                                solution.getLiteral("impactEnvironnemental").getString() : "";
                        String type = solution.contains("type") ? solution.getResource("type").getLocalName() : "";

                        Technologie_EducativeDto dto = new Technologie_EducativeDto(nom, impactEnvironnemental, type);
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

                // Get the type (subclass) from the DTO (e.g., "application_mobile", "tableau_interactif")
                String type = technologieDto.getType();
                String typeRDF = "";

                // Add the specific subclass type if it's provided
                if (type != null && !type.isEmpty()) {
                    typeRDF = " ; a rescue:" + type;
                }

                // Construct the insert query, making sure to add the proper type only once
                String insertQuery =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "INSERT { " +
                                "  <http://rescuefood.org/ontology/" + technologyId + "> a rescue:Technologie_Educative ; " +
                                "  rescue:nom \"" + technologieDto.getNom() + "\" ; " +
                                "  rescue:impactEnvironnemental \"" + technologieDto.getImpactEnvironnemental() + "\" " +
                                typeRDF +  // Add the specific type if provided
                                "} WHERE {}";

                // Execute the update query to add the new technology to the ontology model
                UpdateRequest updateRequest = UpdateFactory.create(insertQuery);
                UpdateAction.execute(updateRequest, model);

                // Save the model after the update
                JenaEngine.saveModel(model, "data/education.owl");

                // Return success response
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
                // Updated SPARQL query to include the 'type' field
                String queryStr =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "SELECT ?id ?nom ?duree ?type WHERE { " +
                                "  ?methode rescue:nom ?nom . " +
                                "  ?methode rescue:duree ?duree . " +
                                "  ?methode rdf:type ?type . " + // Added type field
                                "  BIND(STR(?methode) AS ?id) . " + // Include the ID (URI) as ?id
                                "  FILTER (?type IN (rescue:apprentissage_par_projet, rescue:classe_inversee, rescue:enseignement_differencie, rescue:jeu_educatif)) " +
                                "}";

                // Execute the query
                Query query = QueryFactory.create(queryStr);
                QueryExecution qExec = QueryExecutionFactory.create(query, model);
                ResultSet results = qExec.execSelect();

                // Convert the results to JSON format
                String jsonResponse = resultSetToJson(results);
                return new ResponseEntity<>(jsonResponse, HttpStatus.OK);
            } catch (Exception e) {
                // Handle any exceptions that occur
                return new ResponseEntity<>("Error fetching methodes: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            // Handle the case where the model is not available
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    @PostMapping("/addMethode")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> addMethode(@RequestBody MethodeEnseignementDto methodeDto) {
        if (model != null) {
            try {
                String methodeId = "Methode_" + UUID.randomUUID().toString();
                String methodeType = methodeDto.getType();
                String insertQuery =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "INSERT { " +
                                "  <" + methodeId + "> rdf:type rescue:" + methodeType + " . " +
                                "  <" + methodeId + "> rescue:duree \"" + methodeDto.getDuree() + "\" . " +
                                "  <" + methodeId + "> rescue:nom \"" + methodeDto.getNom() + "\" . " + // Add the type
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
                // Ensure the methode resource exists by ID
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
                                "  ?methode rescue:type ?oldtype . " + // Delete existing type
                                "} " +
                                "INSERT { " +
                                "  ?methode rescue:nom \"" + escapeSpecialCharacterss(methodeDto.getNom()) + "\" . " +
                                "  ?methode rescue:duree \"" + escapeSpecialCharacterss(methodeDto.getDuree()) + "\" . " +
                                "  ?methode rescue:type <" + escapeSpecialCharacterss(methodeDto.getType()) + "> . " + // Insert the new type
                                "} " +
                                "WHERE { " +
                                "  BIND(<" + id + "> AS ?methode) ." + // Ensure the id is correctly formatted in the URI
                                "  OPTIONAL { ?methode rescue:nom ?oldnom } ." +
                                "  OPTIONAL { ?methode rescue:duree ?oldduree } ." +
                                "  OPTIONAL { ?methode rescue:type ?oldtype } ." +
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

                String deleteQuery =
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
                UpdateRequest updateRequest = UpdateFactory.create(deleteQuery);
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
                // Define the SPARQL DELETE query to remove all methode resources
                String deleteQuery =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "DELETE { " +
                                "  ?methode rescue:nom ?oldnom . " +
                                "  ?methode rescue:duree ?oldduree . " +
                                "  ?methode rescue:type ?oldtype . " + // Delete the type field
                                "} " +
                                "WHERE { " +
                                "  ?methode rescue:nom ?oldnom ." +
                                "  ?methode rescue:duree ?oldduree ." +
                                "  ?methode rescue:type ?oldtype ." +
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



    @GetMapping("/personne")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> getPersonne() {
        if (model != null) {
            String sparqlQuery = """
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#>
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
            SELECT ?personne ?nom ?age ?niveau ?dateNaissance ?role ?est_parent_de ?estSuperviséPar ?participe_à ?utilisePlateforme ?evalue
            WHERE {
                ?personne rdf:type base:Personne .  
                ?personne base:nom ?nom .   
                ?personne base:age ?age .
                
                OPTIONAL { ?personne base:niveau ?niveau }
                OPTIONAL { ?personne base:dateNaissance ?dateNaissance }
                OPTIONAL { ?personne base:est_parent_de ?est_parent_de }  
                OPTIONAL { ?personne base:estSuperviséPar ?estSuperviséPar }
                OPTIONAL { ?personne base:participe_à ?participe_à }
                OPTIONAL { ?personne base:utilisePlateforme ?utilisePlateforme }  
                OPTIONAL { ?personne base:evalue ?evalue }
                
                OPTIONAL { ?personne rdf:type base:Administrateur . BIND("Administrateur" AS ?role) }
                OPTIONAL { ?personne rdf:type base:Parent . BIND("Parent" AS ?role) }
                OPTIONAL { ?personne rdf:type base:Etudiant . BIND("Etudiant" AS ?role) }
                OPTIONAL { ?personne rdf:type base:Enseignant . BIND("Enseignant" AS ?role) }
            }
            LIMIT 10
        """;

            Query query = QueryFactory.create(sparqlQuery);
            try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
                ResultSet results = qexec.execSelect();
                JSONArray jsonResults = new JSONArray();

                while (results.hasNext()) {
                    QuerySolution solution = results.nextSolution();

                    // Get basic fields
                    String personne = solution.getResource("personne").toString();
                    String nom = solution.getLiteral("nom").getString();
                    int age = solution.contains("age") ? solution.getLiteral("age").getInt() : 0;

                    // Collect optional fields with null checks
                    String niveau = solution.contains("niveau") ? solution.getLiteral("niveau").getString() : null;
                    String dateNaissance = solution.contains("dateNaissance") ? solution.getLiteral("dateNaissance").getString() : null;
                    String estParentDe = solution.contains("est_parent_de") ? solution.getResource("est_parent_de").toString() : null;
                    String estSuperviséPar = solution.contains("estSuperviséPar") ? solution.getResource("estSuperviséPar").toString() : null;
                    String participeA = solution.contains("participe_à") ? solution.getResource("participe_à").toString() : null;
                    String utilisePlateforme = solution.contains("utilisePlateforme") ? solution.getResource("utilisePlateforme").toString() : null;
                    String evalue = solution.contains("evalue") ? solution.getResource("evalue").toString() : null;

                    // Get role (it’s assigned via the BIND in SPARQL)
                    String role = solution.contains("role") ? solution.getLiteral("role").getString() : "Unknown";

                    // Create a JSONObject to represent the result
                    JSONObject result = new JSONObject();
                    result.put("personne", personne);
                    result.put("nom", nom);
                    result.put("age", age);
                    result.put("role", role);
                    result.put("niveau", niveau);
                    result.put("dateNaissance", dateNaissance);
                    result.put("est_parent_de", estParentDe);
                    result.put("estSuperviséPar", estSuperviséPar);
                    result.put("participe_à", participeA);
                    result.put("utilisePlateforme", utilisePlateforme);
                    result.put("evalue", evalue);

                    jsonResults.put(result);
                }

                // Convert the results to a JSON string
                String jsonString = jsonResults.toString();

                // Return the modified JSON as a response
                return new ResponseEntity<>(jsonString, HttpStatus.OK);

            } catch (Exception e) {
                return new ResponseEntity<>("Error executing SPARQL query: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }



    @PostMapping("/personne")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> addPersonne(@RequestBody Map<String, Object> payload) {
        if (model != null) {
            try {
                // Generate a unique ID for the new Personne
                String personneId = "Personne_" + UUID.randomUUID().toString().replace("-", "_");

                // Start building the SPARQL update query
                StringBuilder updateQuery = new StringBuilder(
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                                "INSERT DATA { " +
                                "  base:" + personneId + " rdf:type base:Personne ; " +
                                "  base:nom \"" + sanitizeLiteral(payload.get("nom")) + "\" ; " +
                                "  base:niveau \"" + sanitizeLiteral(payload.get("niveau")) + "\" ; " +
                                "  base:dateNaissance \"" + sanitizeLiteral(payload.get("dateNaissance")) + "\" ; " +
                                "  base:age \"" + sanitizeLiteral(payload.get("age")) + "\"^^xsd:int . "
                );
                if (payload.containsKey("est_parent_de")) {
                    String etudiantId = payload.get("est_parent_de").toString();
                    updateQuery.append("  base:").append(personneId).append(" base:est_parent_de base:").append(etudiantId).append(" . ");
                }

                // Autres attributs similaires
                if (payload.containsKey("estSuperviséPar")) {
                    String superviseurId = payload.get("estSuperviséPar").toString();
                    updateQuery.append("  base:").append(personneId).append(" base:estSuperviséPar base:").append(superviseurId).append(" . ");
                }
                if (payload.containsKey("evalue")) {
                    String etudiantId = payload.get("evalue").toString();
                    updateQuery.append("  base:").append(personneId).append(" base:evalue base:").append(etudiantId).append(" . ");
                }

                // Autres attributs similaires
                if (payload.containsKey("participe_à")) {
                    String superviseurId = payload.get("participe_à").toString();
                    updateQuery.append("  base:").append(personneId).append(" base:participe_à base:").append(superviseurId).append(" . ");
                }
                if (payload.containsKey("utilisePlatforme")) {
                    String superviseurId = payload.get("utilisePlatforme").toString();
                    updateQuery.append("  base:").append(personneId).append(" base:utilisePlatforme base:").append(superviseurId).append(" . ");
                }



                // Check for sub-entity roles in the payload and add them to the query
                if (payload.containsKey("role")) {
                    String role = payload.get("role").toString().toLowerCase();
                    switch (role) {
                        case "administrateur":
                            updateQuery.append("  base:").append(personneId).append(" rdf:type base:Administrateur . ");
                            break;
                        case "enseignant":
                            updateQuery.append("  base:").append(personneId).append(" rdf:type base:Enseignant . ");
                            break;
                        case "etudiant":
                            updateQuery.append("  base:").append(personneId).append(" rdf:type base:Etudiant . ");
                            break;
                        case "parent":
                            updateQuery.append("  base:").append(personneId).append(" rdf:type base:Parent . ");
                            break;
                        default:
                            break; // No action for unrecognized roles
                    }
                }

                // Close the SPARQL update query
                updateQuery.append("}");

                // Execute the SPARQL update query
                JenaEngine.executeUpdate(updateQuery.toString(), model);

                // Save the updated model to a file to persist changes
                JenaEngine.saveModel(model, "data/education.owl");

                return new ResponseEntity<>("Personne added successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error adding Personne: " + e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Helper function to sanitize literal values
    private String sanitizeLiteral(Object value) {
        return value.toString().replace("\"", "\\\""); // Escape double quotes
    }
    @PutMapping("/personne/{id}")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> updatePersonne(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        if (model != null) {
            try {
                // Safely retrieve values from the payload, using default values if necessary
                String newName = payload.containsKey("nom") ? sanitizeLiteral((String) payload.get("nom")) : null;
                Integer newAge = payload.containsKey("age") ? Integer.parseInt(payload.get("age").toString()) : null;
                String newNiveau = payload.containsKey("niveau") ? sanitizeLiteral((String) payload.get("niveau")) : null;
                String newDateNaissance = payload.containsKey("dateNaissance") ? sanitizeLiteral((String) payload.get("dateNaissance")) : null;
                String newEstParentDe = payload.containsKey("est_parent_de") ? sanitizeLiteral((String) payload.get("est_parent_de")) : null;

                // Ensure ID is properly formatted with a prefix (e.g., "base:")
                String fullId = "base:" + id;

                // Build the SPARQL query with dynamic DELETE/INSERT clauses
                StringBuilder sparqlUpdate = new StringBuilder();
                sparqlUpdate.append("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ")
                        .append("PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> ")
                        .append("PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ")
                        .append("DELETE { ");

                // Only delete properties that are present in the payload
                boolean hasDeleteClause = false;
                if (newName != null) {
                    sparqlUpdate.append(fullId).append(" base:nom ?oldNom . ");
                    hasDeleteClause = true;
                }
                if (newAge != null) {
                    sparqlUpdate.append(fullId).append(" base:age ?oldAge . ");
                    hasDeleteClause = true;
                }
                if (newNiveau != null) {
                    sparqlUpdate.append(fullId).append(" base:niveau ?oldNiveau . ");
                    hasDeleteClause = true;
                }
                if (newDateNaissance != null) {
                    sparqlUpdate.append(fullId).append(" base:dateNaissance ?oldDateNaissance . ");
                    hasDeleteClause = true;
                }
                if (newEstParentDe != null) {
                    sparqlUpdate.append(fullId).append(" base:est_parent_de ?oldEstParentDe . ");
                    hasDeleteClause = true;
                }

                // Ensure DELETE clause ends with a period
                if (hasDeleteClause) {
                    sparqlUpdate.append("} ");
                } else {
                    sparqlUpdate.append("} ");
                }

                sparqlUpdate.append("INSERT { ");

                // Only insert values that are provided in the payload
                boolean hasInsertClause = false;
                if (newName != null) {
                    sparqlUpdate.append(fullId).append(" base:nom \"").append(newName).append("\" . ");
                    hasInsertClause = true;
                }
                if (newAge != null) {
                    sparqlUpdate.append(fullId).append(" base:age \"").append(newAge).append("\"^^xsd:int . ");
                    hasInsertClause = true;
                }
                if (newNiveau != null) {
                    sparqlUpdate.append(fullId).append(" base:niveau \"").append(newNiveau).append("\" . ");
                    hasInsertClause = true;
                }
                if (newDateNaissance != null) {
                    sparqlUpdate.append(fullId).append(" base:dateNaissance \"").append(newDateNaissance).append("\" . ");
                    hasInsertClause = true;
                }
                if (newEstParentDe != null) {
                    sparqlUpdate.append(fullId).append(" base:est_parent_de <").append(newEstParentDe).append("> . ");
                    hasInsertClause = true;
                }

                // Ensure INSERT clause ends with a period
                if (hasInsertClause) {
                    sparqlUpdate.append("} ");
                } else {
                    sparqlUpdate.append("} ");
                }

                sparqlUpdate.append("WHERE { ")
                        .append("   ").append(fullId).append(" base:nom ?oldNom ; ")
                        .append("               base:age ?oldAge ; ")
                        .append("               base:niveau ?oldNiveau ; ")
                        .append("               base:dateNaissance ?oldDateNaissance ; ")
                        .append("               base:est_parent_de ?oldEstParentDe . ")
                        .append("}");

                System.out.println(sparqlUpdate.toString());

                // Execute the SPARQL update query
                JenaEngine.executeUpdate(sparqlUpdate.toString(), model);

                // Save the updated model to a file to persist changes
                JenaEngine.saveModel(model, "data/education.owl");

                return new ResponseEntity<>("Personne updated successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error updating Personne: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }


    // Utility function to sanitize input (if needed, for security reasons)






    @DeleteMapping("/personne/{id}")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> deletePersonne(@PathVariable String id) {
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

    @GetMapping("/pedagogie")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> getPedagodie() {
        if (model != null) {
            String sparqlQuery = """
    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#>
    PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
    SELECT ?pedagogie ?nom 
    WHERE {
        ?pedagogie rdf:type base:Pedagogie .  
        ?pedagogie base:nom ?nom .   
         
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
                    String pedagogie = solution.getResource("pedagogie").toString();
                    String nom = solution.getLiteral("nom").getString();




                    // Create a JSONObject to represent the result
                    JSONObject result = new JSONObject();
                    result.put("pedagodie", pedagogie);
                    result.put("nom", nom);


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

    @PostMapping("/pedagogie")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> addPedagogie(@RequestBody Map<String, Object> payload) {
        if (model != null) {
            try {
                String pedagogieId = "" + UUID.randomUUID();


                String updateQuery =
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" +
                                "PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#>" +
                                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                                "INSERT DATA {" +
                                "base:" + pedagogieId + " rdf:type base:Pedagogie ;" +
                                "base:nom \"" + payload.get("nom") + "\" ;" +
                                "}";

                UpdateRequest updateRequest = UpdateFactory.create(updateQuery);
                Dataset dataset = DatasetFactory.create(model);
                UpdateProcessor processor = UpdateExecutionFactory.create(updateRequest, dataset);
                processor.execute();
                JenaEngine.saveModel(model, "data/education.owl");

                return new ResponseEntity<>("Pedagogie added successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error adding Pedagogie: " + e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PutMapping("/pedagogie/{id}")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> updatePedagogie(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        if (model != null) {
            try {
                String newName = payload.get("nom").toString();
                // Ensure age is parsed correctly as an integer

                // Ensure ID is properly formatted with a prefix (e.g., "base:")
                String fullId = "base:" + id;

                // Construct SPARQL Update query using plain string concatenation
                String sparqlUpdate =
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                                "DELETE { " +
                                "   " + fullId + " base:nom ?oldNom . " +

                                "} " +
                                "INSERT { " +
                                "   " + fullId + " base:nom \"" + newName + "\" . " +
                                "} " +
                                "WHERE { " +
                                "   " + fullId + " base:nom ?oldNom . " +
                                "}";

                // Execute the SPARQL update query
                JenaEngine.executeUpdate(sparqlUpdate, model);

                // Save the updated model to a file to persist changes
                JenaEngine.saveModel(model, "data/education.owl");

                return new ResponseEntity<>("Pedagogie updated successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error updating Pedagogie: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @DeleteMapping("/pedagogie/{id}")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> deletePedagogie(@PathVariable String id) {
        if (model != null) {
            try {
                // Ensure ID is properly formatted with a prefix (e.g., "base:")
                String fullId = "base:" + id;

                // Construct SPARQL Delete query using string concatenation
                String sparqlDelete =
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "DELETE { " +
                                "   " + fullId + " rdf:type base:Pedagogie ; " +
                                "               base:nom ?oldNom . " +
                                "} " +
                                "WHERE { " +
                                "   " + fullId + " rdf:type base:Pedagogie ; " +
                                "               base:nom ?oldNom . " +
                                "}";

                // Execute the SPARQL delete query
                JenaEngine.executeUpdate(sparqlDelete, model);

                // Save the updated model to a file to persist changes
                JenaEngine.saveModel(model, "data/education.owl");

                return new ResponseEntity<>("Pedagogie deleted successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error deleting Pedagogie: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /*activite_educatif*/
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
                JenaEngine.saveModel(model, "data/education.owl");

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
                JenaEngine.saveModel(model, "data/education.owl");

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
                JenaEngine.saveModel(model, "data/education.owl");

                return new ResponseEntity<>("activite educative updated successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error updating activite educative: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    /*Cours*/
    @GetMapping("/Cours")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> getCours() {
        if (model != null) {
            String sparqlQuery = """
    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#>
    PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
    SELECT ?Cours ?credits ?dureeDuCours ?descriptionCours ?heuresEnseignement ?nomDuCours ?objectifDuCours ?prerequis
    WHERE {
        ?Cours rdf:type base:Cours .  
        ?Cours base:credits ?credits . 
        ?Cours base:dureeDuCours ?dureeDuCours . 
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
                    String Cours = solution.getResource("Cours").toString();
                    Literal creditsLiteral = solution.getLiteral("credits");

                    Literal dureeDuCoursLiteral = solution.getLiteral("dureeDuCours");
                    String descriptionCours = solution.getLiteral("descriptionCours").getString();
                    Literal heuresEnseignementLiteral = solution.getLiteral("heuresEnseignement");
                    String nomDuCours = solution.getLiteral("nomDuCours").getString();
                    String objectifDuCours = solution.getLiteral("objectifDuCours").getString();
                    String prerequis = solution.getLiteral("prerequis").getString();

                    int credits = creditsLiteral.getInt();
                    int dureeDuCours = dureeDuCoursLiteral.getInt();
                    int heuresEnseignement = heuresEnseignementLiteral.getInt();


                    JSONObject result = new JSONObject();
                    result.put("Cours", Cours);
                    result.put("credits", credits);
                    result.put("dureeDuCours", dureeDuCours);
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

    @PostMapping("/Cours")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> addCours(@RequestBody Map<String, Object> payload) {
        if (model != null) {
            try {
                String coursId = "Cours_" + UUID.randomUUID().toString().replace("-", "_");

                // Start building the SPARQL update query
                StringBuilder updateQuery = new StringBuilder(
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "PREFIX base: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                                "INSERT DATA { " +
                                "  base:" + coursId + " rdf:type base:Cours ; " +
                                "  base:credits \"" + payload.get("credits") + "\"^^xsd:int ; " +
                                "  base:dureeDuCours \"" + payload.get("dureeDuCours") + "\"^^xsd:int ; " +
                                "  base:descriptionCours \"" + payload.get("descriptionCours") + "\" ; " +
                                "  base:heuresEnseignement \"" + payload.get("heuresEnseignement") + "\"^^xsd:int ; " +
                                "  base:nomDuCours \"" + payload.get("nomDuCours") + "\" ; " +
                                "  base:objectifDuCours \"" + payload.get("objectifDuCours") + "\" ; " +
                                "  base:prerequis \"" + payload.get("prerequis") + "\" ; "
                );

                // Conditionally add the subclass (Magistral or Pratique) to the query
                if (payload.containsKey("magistral") && Boolean.parseBoolean(payload.get("magistral").toString())) {
                    updateQuery.append("  base:" + coursId + " rdf:type base:Magistral . ");
                } else if (payload.containsKey("pratique") && Boolean.parseBoolean(payload.get("pratique").toString())) {
                    updateQuery.append("  base:" + coursId + " rdf:type base:Pratique . ");
                }

                // Close the SPARQL query
                updateQuery.append("}");

                // Execute the SPARQL update query
                JenaEngine.executeUpdate(updateQuery.toString(), model);

                // Save and reload the model to ensure persistence
                JenaEngine.saveModel(model, "data/education.owl");

                return new ResponseEntity<>("Cours added successfully", HttpStatus.OK);
            } catch (Exception e) {
                e.printStackTrace();
                return new ResponseEntity<>("Error adding Cours: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @DeleteMapping("/Cours/{id}")
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
                                "   " + fullId + " base:dureeDuCours ?olddureeDuCours . " +
                                "   " + fullId + " base:descriptionCours ?olddescriptionCours . " +
                                "   " + fullId + " base:heuresEnseignement ?oldheuresEnseignement . " +
                                "   " + fullId + " base:nomDuCours ?oldnomDuCours . " +
                                "   " + fullId + " base:objectifDuCours ?oldobjectifDuCours . " +
                                "   " + fullId + " base:prerequis ?oldprerequis . " +
                                "} " +
                                "WHERE { " +
                                "   " + fullId + " rdf:type base:Cours . " +
                                "   " + fullId + " base:credits ?oldcredits . " +
                                "   " + fullId + " base:dureeDuCours ?olddureeDuCours . " +
                                "   " + fullId + " base:descriptionCours ?olddescriptionCours . " +
                                "   " + fullId + " base:heuresEnseignement ?oldheuresEnseignement . " +
                                "   " + fullId + " base:nomDuCours ?oldnomDuCours . " +
                                "   " + fullId + " base:objectifDuCours ?oldobjectifDuCours . " +
                                "   " + fullId + " base:prerequis ?oldprerequis . " +
                                "}";

                System.out.println("SPARQL Delete Query: " + sparqlDelete);  // Log the query

                JenaEngine.executeUpdate(sparqlDelete, model);

                // Save and reload model to ensure persistence
                JenaEngine.saveModel(model, "data/education.owl");
                //model = JenaEngine.loadModel("data/education  .owl"); // Reload model

                return new ResponseEntity<>("Cours deleted successfully", HttpStatus.OK);
            } catch (Exception e) {
                e.printStackTrace();
                return new ResponseEntity<>("Error deleting Cours: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PutMapping("/Cours/{id}")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> updatecours(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        if (model != null) {
            try {
                int newCredits = Integer.parseInt(payload.get("credits").toString());
                int newDuréeDuCours = Integer.parseInt(payload.get("dureeDuCours").toString());
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
                                "               base:dureeDuCours ?oldDuréeDuCours ; " +
                                "               base:descriptionCours ?oldDescriptionCours ; " +
                                "               base:heuresEnseignement ?oldHeuresEnseignement ; " +
                                "               base:nomDuCours ?oldNomDuCours ; " +
                                "               base:prerequis ?oldPrerequis . " +
                                "} " +
                                "INSERT { " +
                                "   " + fullId + " base:credits \"" + newCredits + "\"^^xsd:int ; " +
                                "               base:dureeDuCours \"" + newDuréeDuCours + "\"^^xsd:int ; " +
                                "               base:descriptionCours \"" + newDescriptionCours + "\" ; " +
                                "               base:heuresEnseignement \"" + newHeuresEnseignement + "\"^^xsd:int ; " +
                                "               base:nomDuCours \"" + newNomDuCours + "\" ; " +
                                "               base:prerequis \"" + newPrerequis + "\" . " +
                                "} " +
                                "WHERE { " +
                                "   " + fullId + " base:credits ?oldCredits ; " +
                                "               base:dureeDuCours ?oldDuréeDuCours ; " +
                                "               base:descriptionCours ?oldDescriptionCours ; " +
                                "               base:heuresEnseignement ?oldHeuresEnseignement ; " +
                                "               base:nomDuCours ?oldNomDuCours ; " +
                                "               base:prerequis ?oldPrerequis . " +
                                "}";

                // Execute the SPARQL update query
                JenaEngine.executeUpdate(sparqlUpdate, model);

                // Save the updated model to a file to persist changes
                JenaEngine.saveModel(model, "data/education.owl");

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
                JenaEngine.saveModel(model, "data/education.owl");

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
                JenaEngine.saveModel(model, "data/education.owl");

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
                JenaEngine.saveModel(model, "data/education.owl");

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
                JenaEngine.saveModel(model, "data/education.owl");

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
                JenaEngine.saveModel(model, "data/education.owl");

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
                JenaEngine.saveModel(model, "data/education.owl");

                return new ResponseEntity<>("activite educative updated successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error updating activite educative: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}





