package com.spms.backend.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

@Converter(autoApply = false)
public class GroupStatusConverter implements AttributeConverter<GroupStatus, String> {

    @Override
    public String convertToDatabaseColumn(GroupStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public GroupStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : GroupStatus.valueOf(dbData.toUpperCase(Locale.ROOT));
    }
}