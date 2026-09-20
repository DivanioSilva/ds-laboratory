package com.example.demo.mapper;

import java.util.List;

import com.example.demo.dto.AddressDto;
import com.example.demo.model.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    AddressDto toDto(Address address);

    List<AddressDto> toDtoList(List<Address> addresses);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "persons", ignore = true)
    Address toEntity(AddressDto addressDto);
}
