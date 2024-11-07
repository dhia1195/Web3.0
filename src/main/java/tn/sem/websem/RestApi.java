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
                // SPARQL query to retrieve all plateforme details including the ID (URI)
                String queryStr =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "SELECT ?id ?name ?subscriptionType WHERE { " +
                                "  ?plateforme rdf:type rescue:Plateforme . " +
                                "  ?plateforme rescue:name ?name . " +
                                "  ?plateforme rescue:subscriptionType ?subscriptionType . " +
                                "  BIND(STR(?plateforme) AS ?id) . " + // Include the ID (URI) as ?id
                                "}";

                // Execute the query
                Query query = QueryFactory.create(queryStr);
                QueryExecution qExec = QueryExecutionFactory.create(query, model);
                ResultSet results = qExec.execSelect();

                // Convert the results to JSON
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
                // Generate a random UUID for the Plateforme ID
                String plateformeId = "Plateforme_" + UUID.randomUUID().toString();

                // Define the SPARQL INSERT query with the generated ID
                String insertQuery =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "INSERT { " +
                                "  <" + plateformeId + "> rdf:type rescue:Plateforme . " +
                                "  <" + plateformeId + "> rescue:name \"" + plateformeDto.getName() + "\" . " +
                                "  <" + plateformeId + "> rescue:subscriptionType \"" + plateformeDto.getSubscriptionType() + "\" . " +
                                "} WHERE { }"; // No need for a WHERE clause since we are inserting a new resource

                // Create the update request and execute it
                UpdateRequest updateRequest = UpdateFactory.create(insertQuery);
                UpdateAction.execute(updateRequest, model);

                // Save the updated model to the ontology file
                JenaEngine.saveModel(model, "data/education.owl");

                // Return the newly created Plateforme ID along with the success message
                return new ResponseEntity<>("Plateforme added successfully with ID: " + plateformeId, HttpStatus.CREATED);
            } catch (Exception e) {
                return new ResponseEntity<>("Error adding plateforme: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PutMapping("/modifyPlateforme/{id}")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> modifyPlateforme(@PathVariable String id, @RequestBody PlateformeDto plateformeDto) {
        if (model != null) {
            try {
                // Ensure the plateforme resource exists by ID
                Resource plateformeResource = model.getResource("http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#" + id);
                if (plateformeResource == null) {
                    return new ResponseEntity<>("Plateforme not found", HttpStatus.NOT_FOUND);
                }

                // Define the SPARQL DELETE/INSERT query to modify the plateforme details
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
        return input.replace("\"", "\\\"") // Escape double quotes
                .replace("\\", "\\\\");  // Escape backslashes
    }
    @DeleteMapping("/delete/{id}")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> deletePlateforme(@PathVariable String id) {
        if (model != null) {
            try {
                // Ensure the plateforme resource exists by ID
                Resource plateformeResource = model.getResource("http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#" + id);
                if (plateformeResource == null) {
                    return new ResponseEntity<>("Plateforme not found", HttpStatus.NOT_FOUND);
                }

                // Define the SPARQL DELETE/INSERT query to modify the plateforme details
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



    // Method to get all Technologie_Educative instances
    @GetMapping("/getAllTechnologies")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<List<Technologie_EducativeDto>> getAllTechnologies() {
        List<Technologie_EducativeDto> technologies = new ArrayList<>();

        if (model != null) {
            try {
                // Reload the model to ensure it reflects the latest changes
                model = JenaEngine.readModel("data/education.owl");

                // SPARQL query to retrieve all instances of Technologie_Educative
                String queryStr =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "SELECT ?name ?impactEnvironnemental WHERE { " +
                                "  ?tech rdf:type rescue:Technologie_Educative . " +
                                "  ?tech rescue:nom ?name . " +
                                "  ?tech rescue:impactEnvironnemental ?impactEnvironnemental . " +
                                "}";

                Query query = QueryFactory.create(queryStr);
                QueryExecution qexec = QueryExecutionFactory.create(query, model);
                ResultSet results = qexec.execSelect();

                // Iterate through results and add them to the DTO list
                while (results.hasNext()) {
                    QuerySolution solution = results.nextSolution();
                    String nom = solution.getLiteral("name").getString();
                    String impactEnvironnemental = solution.contains("impactEnvironnemental") ?
                            solution.getLiteral("impactEnvironnemental").getString() : "";

                    // Create DTO and add to list
                    Technologie_EducativeDto dto = new Technologie_EducativeDto(nom, impactEnvironnemental);
                    technologies.add(dto);
                }

                return new ResponseEntity<>(technologies, HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    // Method to add a new Technologie_Educative instance
    @PostMapping("/addTechnologieEduc")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> addTechnologieEduc(@RequestBody Technologie_EducativeDto technologieDto) {
        if (model != null) {
            try {
                // Define the SPARQL INSERT query
                String insertQuery =
                        "PREFIX rescue: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#> " +
                                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "INSERT { " +
                                "  ?technologieEduc rdf:type rescue:Technologie_Educative . " +
                                "  ?technologieEduc rescue:nom \"" + technologieDto.getNom() + "\" . " +
                                "  ?technologieEduc rescue:impactEnvironnemental \"" + technologieDto.getImpactEnvironnemental() + "\" . " +
                                "} WHERE { " +
                                "  BIND(IRI(CONCAT(\"http://rescuefood.org/ontology/Technologie_Educative_\", STRUUID())) AS ?technologieEduc) " +
                                "}";

                // Create the update request and execute it
                UpdateRequest updateRequest = UpdateFactory.create(insertQuery);
                UpdateAction.execute(updateRequest, model);

                // Reload the model after insertion to ensure the changes are reflected
                model = JenaEngine.readModel("data/education.owl");

                // Save the updated model to the ontology file
                JenaEngine.saveModel(model, "data/education.owl");

                return new ResponseEntity<>("Technologie Educative added successfully", HttpStatus.CREATED);
            } catch (Exception e) {
                return new ResponseEntity<>("Error adding Technologie Educative: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/restaurant")
    @CrossOrigin(origins = "http://localhost:4200")
    public String afficherRestaurant() {
        String NS = "";
        if (model != null) {
            NS = model.getNsPrefixURI("");

            Model inferedModel = JenaEngine.readInferencedModelFromRuleFile(model, "data/rules.txt");

            OutputStream res = JenaEngine.executeQueryFile(inferedModel, "data/query_Restaurant.txt");

            System.out.println(res);
            return res.toString();

        } else {
            return ("Error when reading model from ontology");
        }
    }
    @PostMapping("/addRestaurant2")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> addRestaurant2(@RequestBody RestaurantDto restaurantDto) {
        if (model != null) {
            try {
                // Define the SPARQL INSERT query
                String insertQuery =
                        "PREFIX rescue: <http://rescuefood.org/ontology#> " +
                                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "INSERT { " +
                                "  ?restaurant rdf:type rescue:Restaurant . " +
                                "  ?restaurant rescue:name \"" + restaurantDto.getName() + "\" . " +
                                "  ?restaurant rescue:contact \"" + restaurantDto.getContact() + "\" . " +
                                "  ?restaurant rescue:address \"" + restaurantDto.getAddress() + "\" . " +
                                "} WHERE { " +
                                "  BIND(IRI(CONCAT(\"http://rescuefood.org/ontology/Restaurant_\", STRUUID())) AS ?restaurant) " +
                                "}";

                // Create the update request and execute it
                UpdateRequest updateRequest = UpdateFactory.create(insertQuery);
                UpdateAction.execute(updateRequest, model);

                // Save the updated model to the ontology file
                JenaEngine.saveModel(model, "data/rescuefood.owl");

                return new ResponseEntity<>("Restaurant added successfully", HttpStatus.CREATED);
            } catch (Exception e) {
                return new ResponseEntity<>("Error adding restaurant: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PutMapping("/modifyRestaurant2")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> modifyRestaurant2(@RequestBody RestaurantDto restaurantDto) {
        if (model != null) {
            try {
                // Ensure the restaurant resource exists
                Resource restaurantResource = model.getResource(restaurantDto.getRestaurant());
                if (restaurantResource == null) {
                    return new ResponseEntity<>("Restaurant not found", HttpStatus.NOT_FOUND);
                }

                // Define the SPARQL DELETE/INSERT query
                String modifyQuery =
                        "PREFIX rescue: <http://rescuefood.org/ontology#> " +
                                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "DELETE { " +
                                "  ?restaurant rescue:name ?oldName . " +
                                "  ?restaurant rescue:contact ?oldContact . " +
                                "  ?restaurant rescue:address ?oldAddress . " +
                                "} " +
                                "INSERT { " +
                                "  ?restaurant rescue:name \"" + restaurantDto.getName() + "\" . " +
                                "  ?restaurant rescue:contact \"" + restaurantDto.getContact() + "\" . " +
                                "  ?restaurant rescue:address \"" + restaurantDto.getAddress() + "\" . " +
                                "} " +
                                "WHERE { " +
                                "  BIND(<" + restaurantDto.getRestaurant() + "> AS ?restaurant) ." +
                                "  OPTIONAL { ?restaurant rescue:name ?oldName } ." +
                                "  OPTIONAL { ?restaurant rescue:contact ?oldContact } ." +
                                "  OPTIONAL { ?restaurant rescue:address ?oldAddress } ." +
                                "}";

                // Create and execute the update request
                UpdateRequest updateRequest = UpdateFactory.create(modifyQuery);
                UpdateAction.execute(updateRequest, model);

                // Save the updated model to the ontology file
                JenaEngine.saveModel(model, "data/rescuefood.owl");

                return new ResponseEntity<>("Restaurant modified successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error modifying restaurant: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @DeleteMapping("/deleteRestaurant2")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> deleteRestaurant2(@RequestBody RestaurantDto restaurantDto) {
        String restaurantUri = restaurantDto.getRestaurant();

        System.out.println("Received request to delete restaurant: " + restaurantUri);

        if (model != null) {
            try {
                // Define the SPARQL DELETE query
                String deleteQuery =
                        "PREFIX rescue: <http://rescuefood.org/ontology#> " +
                                "DELETE WHERE { " +
                                "  <" + restaurantUri + "> ?p ?o ." +
                                "}";

                // Create and execute the update request
                UpdateRequest updateRequest = UpdateFactory.create(deleteQuery);
                UpdateAction.execute(updateRequest, model);

                // Save the updated model to the ontology file
                JenaEngine.saveModel(model, "data/rescuefood.owl");

                return new ResponseEntity<>("Restaurant deleted successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error deleting restaurant: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping("/restaurants/sorted")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> getSortedRestaurants() {
        if (model != null) {
            try {
                // Define the SPARQL query to retrieve sorted restaurants by name
                String queryString =
                        "PREFIX rescue: <http://rescuefood.org/ontology#> " +
                                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                                "SELECT ?restaurant ?name ?contact ?address " +
                                "WHERE { " +
                                "  ?restaurant rdf:type rescue:Restaurant . " +
                                "  ?restaurant rescue:name ?name . " +
                                "} " +
                                "ORDER BY ?name"; // Sort by restaurant name

                // Execute the query on the inferred model
                OutputStream res = JenaEngine.executeQuery(model, queryString);

                // Convert the OutputStream to String for the response
                return new ResponseEntity<>(res.toString(), HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error retrieving sorted restaurants: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/addRestaurant")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> addRestaurant(@RequestBody RestaurantDto restaurantDto) {
        if (model != null) {
            try {
                // Generate a random URL for the restaurant resource using UUID
                String restaurantResourceUri = "http://rescuefood.org/ontology#Restaurant" + UUID.randomUUID().toString();

                // Create the resource with the generated URL
                Resource restaurantResource = model.createResource(restaurantResourceUri);

                Property nameProperty = model.createProperty("http://rescuefood.org/ontology#name");
                Property contactProperty = model.createProperty("http://rescuefood.org/ontology#contact");
                Property addressProperty = model.createProperty("http://rescuefood.org/ontology#address");

                // Add RDF type for the resource
                model.add(restaurantResource, RDF.type, model.createResource("http://rescuefood.org/ontology#Restaurant"));

                // Add properties to the resource
                model.add(restaurantResource, nameProperty, restaurantDto.getName());
                model.add(restaurantResource, contactProperty, restaurantDto.getContact());
                model.add(restaurantResource, addressProperty, restaurantDto.getAddress());

                // Save the model
                JenaEngine.saveModel(model, "data/rescuefood.owl");

                return new ResponseEntity<>("Restaurant added successfully: " + restaurantResourceUri, HttpStatus.CREATED);
            } catch (Exception e) {
                return new ResponseEntity<>("Error adding restaurant: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PutMapping("/modifyRestaurant")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> modifyRestaurant(@RequestBody RestaurantDto restaurantDto) {
        if (model != null) {
            try {
                Resource restaurantResource = model.getResource(restaurantDto.getRestaurant());
                if (restaurantResource == null) {
                    return new ResponseEntity<>("Restaurant not found", HttpStatus.NOT_FOUND);
                }

                Property nameProperty = model.createProperty("http://rescuefood.org/ontology#name");
                Property contactProperty = model.createProperty("http://rescuefood.org/ontology#contact");
                Property addressProperty = model.createProperty("http://rescuefood.org/ontology#address");

                model.removeAll(restaurantResource, nameProperty, null);
                model.removeAll(restaurantResource, contactProperty, null);
                model.removeAll(restaurantResource, addressProperty, null);

                model.add(restaurantResource, nameProperty, restaurantDto.getName());
                model.add(restaurantResource, contactProperty, restaurantDto.getContact());
                model.add(restaurantResource, addressProperty, restaurantDto.getAddress());

                JenaEngine.saveModel(model, "data/rescuefood.owl");

                return new ResponseEntity<>("Restaurant modified successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error modifying restaurant: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @DeleteMapping("/deleteRestaurant")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<String> deleteRestaurant(@RequestBody RestaurantDto restaurantDto) {
        String restaurantUri = restaurantDto.getRestaurant();

        System.out.println("Received request to delete restaurant: " + restaurantUri);

        if (model != null) {
            try {
                Resource restaurantResource = model.getResource(restaurantUri);
                if (restaurantResource == null) {
                    return new ResponseEntity<>("Restaurant not found", HttpStatus.NOT_FOUND);
                }

                model.removeAll(restaurantResource, null, null);

                JenaEngine.saveModel(model, "data/rescuefood.owl");

                return new ResponseEntity<>("Restaurant deleted successfully", HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>("Error deleting restaurant: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>("Error when reading model from ontology", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}