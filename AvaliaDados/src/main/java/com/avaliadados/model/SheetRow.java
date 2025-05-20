package com.avaliadados.model;

import com.avaliadados.model.enums.TypeAv;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "project_spreadsheet_rows")
public class SheetRow {
    @Id
    private String id;
    private String projectId;
    private String collaboratorId;
    private TypeAv type;
    @Builder.Default
    private Map<String, String> data = new HashMap<>();

}
