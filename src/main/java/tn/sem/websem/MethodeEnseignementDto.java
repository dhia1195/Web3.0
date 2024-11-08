package tn.sem.websem;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MethodeEnseignementDto {
    private String nom;
    private String duree;
    private String type;
}
