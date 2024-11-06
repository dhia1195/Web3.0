package com.infinity.education.RestControllers;
import com.infinity.education.DTO.PlateformeDTO;
import org.apache.jena.query.*;
import org.apache.jena.update.*;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/plateformes")
public class PlateformeController {

    private static final String SPARQL_ENDPOINT = "http://localhost:3030/Education/sparql"; // Replace with your endpoint

    // SHOW: Get all plateformes
    @GetMapping
    public List<PlateformeDTO> getAllPlateformes() {
        List<PlateformeDTO> plateformes = new ArrayList<>();

        String queryStr = """
        PREFIX myOnto: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#>
        SELECT ?platform ?name ?subscriptionType
        WHERE {
            ?platform rdf:type myOnto:plateforme .
            ?platform myOnto:nomPlateform ?name .
            ?platform myOnto:typeAbonnement ?subscriptionType .
        }
        """;

        Query query = QueryFactory.create(queryStr);

        // Execute query without authentication
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(SPARQL_ENDPOINT, query)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution sol = results.nextSolution();
                PlateformeDTO plateforme = new PlateformeDTO();
                plateforme.setId(sol.getResource("platform").getURI());
                plateforme.setName(sol.getLiteral("name").getString());
                plateforme.setSubscriptionType(sol.getLiteral("subscriptionType").getString());
                plateformes.add(plateforme);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return plateformes;
    }


    // CREATE: Add a new plateforme
    @PostMapping
    public String addPlateforme(@RequestBody PlateformeDTO plateformeDTO) {
        String insertStr = String.format("""
            PREFIX myOnto: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#>
            INSERT DATA {
                <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#%s> rdf:type myOnto:plateforme .
                <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#%s> myOnto:nomPlateform "%s" .
                <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#%s> myOnto:typeAbonnement "%s" .
            }
            """, plateformeDTO.getId(), plateformeDTO.getId(), plateformeDTO.getName(), plateformeDTO.getId(), plateformeDTO.getSubscriptionType());

        UpdateRequest updateRequest = UpdateFactory.create(insertStr);
        UpdateProcessor processor = UpdateExecutionFactory.createRemote(updateRequest, SPARQL_ENDPOINT + "/update");
        processor.execute();
        return "Plateforme added successfully";
    }

    // UPDATE: Update an existing plateforme by ID
    @PutMapping("/{id}")
    public String updatePlateforme(@PathVariable String id, @RequestBody PlateformeDTO plateformeDTO) {
        String deleteStr = String.format("""
            PREFIX myOnto: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#>
            DELETE {
                <%1$s> myOnto:nomPlateform ?name .
                <%1$s> myOnto:typeAbonnement ?subscriptionType .
            } WHERE {
                <%1$s> myOnto:nomPlateform ?name .
                <%1$s> myOnto:typeAbonnement ?subscriptionType .
            };
        """, id);

        String insertStr = String.format("""
            PREFIX myOnto: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#>
            INSERT DATA {
                <%1$s> myOnto:nomPlateform "%2$s" .
                <%1$s> myOnto:typeAbonnement "%3$s" .
            }
        """, id, plateformeDTO.getName(), plateformeDTO.getSubscriptionType());

        UpdateRequest updateRequest = UpdateFactory.create(deleteStr + insertStr);
        UpdateProcessor processor = UpdateExecutionFactory.createRemote(updateRequest, SPARQL_ENDPOINT + "/update");
        processor.execute();
        return "Plateforme updated successfully";
    }

    // DELETE: Delete a plateforme by ID
    @DeleteMapping("/{id}")
    public String deletePlateforme(@PathVariable String id) {
        String deleteStr = String.format("""
            PREFIX myOnto: <http://www.semanticweb.org/emnar/ontologies/2024/9/untitled-ontology-7#>
            DELETE WHERE {
                <%1$s> ?p ?o .
            }
        """, id);

        UpdateRequest updateRequest = UpdateFactory.create(deleteStr);
        UpdateProcessor processor = UpdateExecutionFactory.createRemote(updateRequest, SPARQL_ENDPOINT + "/update");
        processor.execute();
        return "Plateforme deleted successfully";
    }
}

