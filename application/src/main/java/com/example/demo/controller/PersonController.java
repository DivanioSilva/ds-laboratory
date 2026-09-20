package com.example.demo.controller;

import java.util.List;

import com.example.demo.dto.PersonDto;
import com.example.demo.exception.AddressNotFoundException;
import com.example.demo.exception.PersonNotFoundException;
import com.example.demo.mapper.PersonMapper;
import com.example.demo.model.Person;
import com.example.demo.repository.PersonRepository;
import com.example.demo.repository.AddressRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/persons")
@Tag(name = "Pessoas", description = "Operações CRUD de pessoas")
public class PersonController {

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;
    private final AddressRepository addressRepository;

    public PersonController(
            PersonRepository personRepository,
            PersonMapper personMapper,
            AddressRepository addressRepository) {
        this.personRepository = personRepository;
        this.personMapper = personMapper;
        this.addressRepository = addressRepository;
    }

    @GetMapping
    @Operation(summary = "Listar todas as pessoas")
    public List<PersonDto> findAll() {
        return personMapper.toDtoList(personRepository.findAll());
    }

    @GetMapping("/search")
    @Operation(summary = "Pesquisar pessoas pelo primeiro nome, apelido ou idade")
    public List<PersonDto> search(
            @Parameter(description = "Parte do primeiro nome, apelido ou idade", example = "silva")
            @RequestParam(required = false) String query,
            @Parameter(description = "Parâmetro antigo, mantido para compatibilidade", deprecated = true)
            @RequestParam(required = false) String firstName) {
        String term = query != null ? query.trim() : firstName == null ? "" : firstName.trim();
        return personMapper.toDtoList(term.isBlank()
                ? personRepository.findAll()
                : personRepository.searchByNameOrAge(term));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Procurar uma pessoa pelo ID")
    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada")
    public PersonDto findById(@PathVariable Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new PersonNotFoundException(id));
        return personMapper.toDto(person);
    }

    @PostMapping
    @Operation(summary = "Criar uma pessoa")
    @ApiResponse(responseCode = "201", description = "Pessoa criada")
    public ResponseEntity<PersonDto> create(@Valid @RequestBody PersonDto personDto) {
        Person person = personMapper.toEntity(personDto);
        assignAddress(person, personDto.getAddressId());
        Person savedPerson = personRepository.save(person);
        return ResponseEntity.status(HttpStatus.CREATED).body(personMapper.toDto(savedPerson));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar uma pessoa")
    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada")
    public PersonDto update(@PathVariable Long id, @Valid @RequestBody PersonDto personDto) {
        Person existingPerson = personRepository.findById(id)
                .orElseThrow(() -> new PersonNotFoundException(id));

        personMapper.updateEntity(personDto, existingPerson);
        assignAddress(existingPerson, personDto.getAddressId());
        Person savedPerson = personRepository.save(existingPerson);
        return personMapper.toDto(savedPerson);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar uma pessoa")
    @ApiResponse(responseCode = "204", description = "Pessoa eliminada")
    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!personRepository.existsById(id)) {
            throw new PersonNotFoundException(id);
        }

        personRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void assignAddress(Person person, Long addressId) {
        if (addressId == null) {
            person.setAddress(null);
            return;
        }

        person.setAddress(addressRepository.findById(addressId)
                .orElseThrow(() -> new AddressNotFoundException(addressId)));
    }
}
