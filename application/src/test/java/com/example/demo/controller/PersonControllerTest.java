package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.example.demo.dto.PersonDto;
import com.example.demo.exception.PersonNotFoundException;
import com.example.demo.mapper.PersonMapper;
import com.example.demo.model.Person;
import com.example.demo.repository.PersonRepository;
import com.example.demo.repository.AddressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class PersonControllerTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PersonMapper personMapper;

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private PersonController personController;

    @Test
    void listsAllPeopleAsDtos() {
        List<Person> people = List.of(person(1L, "Ana", "Silva", 30));
        List<PersonDto> expected = List.of(dto(1L, "Ana", "Silva", 30));
        when(personRepository.findAll()).thenReturn(people);
        when(personMapper.toDtoList(people)).thenReturn(expected);

        List<PersonDto> result = personController.findAll();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void searchesPeopleByFirstNameLastNameOrAge() {
        List<Person> people = List.of(person(1L, "Ana", "Silva", 30));
        List<PersonDto> expected = List.of(dto(1L, "Ana", "Silva", 30));
        when(personRepository.searchByNameOrAge("an")).thenReturn(people);
        when(personMapper.toDtoList(people)).thenReturn(expected);

        List<PersonDto> result = personController.search("an", null);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void findsPersonById() {
        Person person = person(1L, "Ana", "Silva", 30);
        PersonDto expected = dto(1L, "Ana", "Silva", 30);
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));
        when(personMapper.toDto(person)).thenReturn(expected);

        PersonDto result = personController.findById(1L);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void throwsWhenPersonByIdDoesNotExist() {
        when(personRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personController.findById(99L))
                .isInstanceOf(PersonNotFoundException.class)
                .hasMessage("Pessoa com o ID 99 não foi encontrada");
    }

    @Test
    void createsPersonAndReturnsCreatedStatus() {
        PersonDto request = dto(null, "Ana", "Silva", 30);
        Person unsavedPerson = person(null, "Ana", "Silva", 30);
        Person savedPerson = person(1L, "Ana", "Silva", 30);
        PersonDto responseDto = dto(1L, "Ana", "Silva", 30);
        when(personMapper.toEntity(request)).thenReturn(unsavedPerson);
        when(personRepository.save(unsavedPerson)).thenReturn(savedPerson);
        when(personMapper.toDto(savedPerson)).thenReturn(responseDto);

        ResponseEntity<PersonDto> response = personController.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(responseDto);
    }

    @Test
    void updatesExistingPerson() {
        PersonDto request = dto(null, "Maria", "Santos", 35);
        Person existingPerson = person(1L, "Ana", "Silva", 30);
        PersonDto expected = dto(1L, "Maria", "Santos", 35);
        when(personRepository.findById(1L)).thenReturn(Optional.of(existingPerson));
        when(personRepository.save(existingPerson)).thenReturn(existingPerson);
        when(personMapper.toDto(existingPerson)).thenReturn(expected);

        PersonDto result = personController.update(1L, request);

        verify(personMapper).updateEntity(request, existingPerson);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void throwsWhenUpdatingUnknownPerson() {
        PersonDto request = dto(null, "Maria", "Santos", 35);
        when(personRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personController.update(99L, request))
                .isInstanceOf(PersonNotFoundException.class);
        verify(personRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deletesExistingPerson() {
        when(personRepository.existsById(1L)).thenReturn(true);

        ResponseEntity<Void> response = personController.delete(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(personRepository).deleteById(1L);
    }

    @Test
    void throwsWhenDeletingUnknownPerson() {
        when(personRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> personController.delete(99L))
                .isInstanceOf(PersonNotFoundException.class);
        verify(personRepository, never()).deleteById(99L);
    }

    private Person person(Long id, String firstName, String lastName, int age) {
        return new Person(id, firstName, lastName, age);
    }

    private PersonDto dto(Long id, String firstName, String lastName, int age) {
        return new PersonDto(id, firstName, lastName, age);
    }
}
