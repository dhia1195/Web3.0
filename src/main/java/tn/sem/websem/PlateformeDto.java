package tn.sem.websem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlateformeDto {
    private String name;
    private String subscriptionType;
    private String type;  // Add this field

}
