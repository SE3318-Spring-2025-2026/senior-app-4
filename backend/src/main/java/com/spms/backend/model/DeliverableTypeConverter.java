package com.spms.backend.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

@Converter(autoApply = false)
public class DeliverableTypeConverter implements AttributeConverter<DeliverableType, String> {

    @Override
    public String convertToDatabaseColumn(DeliverableType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public DeliverableType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : DeliverableType.valueOf(dbData.toUpperCase(Locale.ROOT));
    }
}
