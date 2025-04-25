package com.avaliadados.controller;

// 1) o AttributeConverter
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

@Converter(autoApply = true)
public class JsonMapConverter
        implements AttributeConverter<Map<String,Object>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao converter Map→JSON", e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        try {
            return MAPPER.readValue(dbData, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao converter JSON→Map", e);
        }
    }
}
