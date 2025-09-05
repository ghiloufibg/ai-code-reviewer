
package com.ghiloufi.aicode.util;

import com.fasterxml.jackson.databind.*;
import com.networknt.schema.*;
import java.util.Set;

public class JsonValidator {
    private final ObjectMapper om = new ObjectMapper();

    public boolean isValid(String schemaJson, String data) {
        try {
            JsonSchemaFactory f = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            JsonSchema schema = f.getSchema(schemaJson);
            JsonNode node = om.readTree(data);
            Set<ValidationMessage> errors = schema.validate(node);
            return errors.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
