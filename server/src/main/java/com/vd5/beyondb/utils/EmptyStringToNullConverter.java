package com.vd5.beyondb.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EmptyStringToNullConverter implements AttributeConverter<String, String> {
    @Override
    public String convertToDatabaseColumn(String string) {
        return "".equals(string) ? null : string;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData;
    }
}
