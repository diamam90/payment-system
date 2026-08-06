package com.example.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Named("DateTimeMapper")
@Mapper(componentModel = "spring")
public class DateTimeMapper {

    @Named("toZDT")
    public ZonedDateTime localDateTimeToUTC(LocalDateTime value) {
        if (value == null) return null;
        return ZonedDateTime.of(value, ZoneOffset.UTC);
    }
}
