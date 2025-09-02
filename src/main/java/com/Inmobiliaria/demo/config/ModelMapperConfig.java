package com.Inmobiliaria.demo.config;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.modelmapper.ModelMapper;
import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // ✅ Converter: Date → LocalDate
        Converter<Date, LocalDate> toLocalDate = new Converter<Date, LocalDate>() {
            public LocalDate convert(MappingContext<Date, LocalDate> context) {
                Date source = context.getSource();
                return (source == null) ? null : source.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
        };

        //Converter: LocalDate → Date
        Converter<LocalDate, Date> toDate = new Converter<LocalDate, Date>() {
            public Date convert(MappingContext<LocalDate, Date> context) {
                LocalDate source = context.getSource();
                return (source == null) ? null : Date.from(source.atStartOfDay(ZoneId.systemDefault()).toInstant());
            }
        };

        modelMapper.addConverter(toLocalDate);
        modelMapper.addConverter(toDate);

        return modelMapper;
    }
}
