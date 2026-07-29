package com.lanyuan.starter.training;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class TrainingRecordImagesConverter implements AttributeConverter<List<TrainingRecordImage>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<TrainingRecordImage>> IMAGE_LIST = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<TrainingRecordImage> images) {
        try {
            return OBJECT_MAPPER.writeValueAsString(images == null ? List.of() : images);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("无法保存实训记录图片信息", ex);
        }
    }

    @Override
    public List<TrainingRecordImage> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) return new ArrayList<>();
        try {
            return new ArrayList<>(OBJECT_MAPPER.readValue(value, IMAGE_LIST));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("实训记录图片信息格式错误", ex);
        }
    }
}
