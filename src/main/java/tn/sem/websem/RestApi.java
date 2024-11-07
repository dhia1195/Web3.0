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
                                "SELECT ?id ?name ?subscriptionType WHERE { " +
                                "  ?plateforme rdf:type rescue:Plateforme . " +
                                "  ?plateforme rescue:name ?name . " +
                                "  ?plateforme rescue:subscriptionType ?subscriptionType . " +
                                "  BIND(STR(?plateforme) AS ?id) . " + // Include the ID (URI) as ?id
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

                String insertQuery =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "INSERT { " +
                                "  <" + plateformeId + "> rdf:type rescue:Plateforme . " +
                                "  <" + plateformeId + "> rescue:name \"" + plateformeDto.getName() + "\" . " +
                                "  <" + plateformeId + "> rescue:subscriptionType \"" + plateformeDto.getSubscriptionType() + "\" . " +
                                "} WHERE { }"; // No need for a WHERE clause since we are inserting a new resource

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

                String modifyQuery =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "DELETE { " +
                                "  ?plateforme rescue:name ?oldName . " +
                                "  ?plateforme rescue:subscriptionType ?oldSubscriptionType . " +
                                "} " +
                                "INSERT { " +
                                "  ?plateforme rescue:name \"" + escapeSpecialCharacters(plateformeDto.getName()) + "\" . " +
                                "  ?plateforme rescue:subscriptionType \"" + escapeSpecialCharacters(plateformeDto.getSubscriptionType()) + "\" . " +
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

}



