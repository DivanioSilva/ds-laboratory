package com.example.demo.mapper;

import java.util.List;

import com.example.demo.dto.PersonDto;
import com.example.demo.model.Person;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PersonMapper {

    @Mapping(source = "address.id", target = "addressId")
    PersonDto toDto(Person person);

    List<PersonDto> toDtoList(List<Person> persons);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "address", ignore = true)
    Person toEntity(PersonDto personDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "address", ignore = true)
    void updateEntity(PersonDto personDto, @MappingTarget Person person);
}
