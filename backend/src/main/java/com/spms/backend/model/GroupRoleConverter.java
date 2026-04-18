package com.spms.backend.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Locale;

@Converter(autoApply = false)
public class GroupRoleConverter implements AttributeConverter<GroupRole, String> {

    @Override
    public String convertToDatabaseColumn(GroupRole attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public GroupRole convertToEntityAttribute(String dbData) {
        return dbData == null ? null : GroupRole.valueOf(dbData.toUpperCase(Locale.ROOT));
    }
}