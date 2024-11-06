package com.infinity.education.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlateformeDTO {
    private String id; // Changed from int to String
    private String name;
    private String subscriptionType;
}
