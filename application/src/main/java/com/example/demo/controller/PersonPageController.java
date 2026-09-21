package com.example.demo.controller;

import java.util.Comparator;
import java.util.List;

import com.example.demo.dto.PersonDto;
import com.example.demo.exception.AddressNotFoundException;
import com.example.demo.exception.PersonNotFoundException;
import com.example.demo.mapper.PersonMapper;
import com.example.demo.mapper.AddressMapper;
import com.example.demo.model.Person;
import com.example.demo.repository.PersonRepository;
import com.example.demo.repository.AddressRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/persons")
public class PersonPageController {

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;

    public PersonPageController(
            PersonRepository personRepository,
            PersonMapper personMapper,
            AddressRepository addressRepository,
            AddressMapper addressMapper) {
        this.personRepository = personRepository;
        this.personMapper = personMapper;
        this.addressRepository = addressRepository;
        this.addressMapper = addressMapper;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false, defaultValue = "") String firstName,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication,
            Model model) {
        String term = query == null ? firstName.trim() : query.trim();
        List<Person> people = term.isBlank()
                ? personRepository.findAll()
                : personRepository.searchByNameOrAge(term);

        Comparator<Person> comparator = switch (sort) {
            case "name" -> Comparator.comparing(Person::getFirstName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Person::getLastName, String.CASE_INSENSITIVE_ORDER);
            case "age" -> Comparator.comparing(Person::getAge);
            default -> Comparator.comparing(Person::getId);
        };
        if ("desc".equalsIgnoreCase(direction)) {
            comparator = comparator.reversed();
            direction = "desc";
        } else {
            direction = "asc";
        }
        people.sort(comparator.thenComparing(Person::getId));

        size = List.of(5, 10, 20, 50).contains(size) ? size : 10;
        int totalPeople = people.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalPeople / size));
        page = Math.min(Math.max(page, 1), totalPages);
        int fromIndex = Math.min((page - 1) * size, totalPeople);
        int toIndex = Math.min(fromIndex + size, totalPeople);

        model.addAttribute("people", personMapper.toDtoList(people.subList(fromIndex, toIndex)));
        model.addAttribute("query", term);
        model.addAttribute("sort", List.of("id", "name", "age").contains(sort) ? sort : "id");
        model.addAttribute("direction", direction);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPeople", totalPeople);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("firstVisible", totalPeople == 0 ? 0 : fromIndex + 1);
        model.addAttribute("lastVisible", toIndex);
        model.addAttribute("username", authentication.getName());
        model.addAttribute("canCreate", hasRole(authentication, "create_users"));
        model.addAttribute("canImport", hasRole(authentication, "import_users"));
        model.addAttribute("canEdit", hasRole(authentication, "edit_users"));
        model.addAttribute("canDelete", hasRole(authentication, "delete_users"));
        return "persons/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("person", new PersonDto());
        addAddresses(model);
        return "persons/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("person") PersonDto personDto,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            addAddresses(model);
            return "persons/form";
        }
        Person person = personMapper.toEntity(personDto);
        assignAddress(person, personDto.getAddressId());
        personRepository.save(person);
        return "redirect:/persons";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Person person = findPerson(id);
        model.addAttribute("person", personMapper.toDto(person));
        addAddresses(model);
        return "persons/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("person") PersonDto personDto,
            BindingResult bindingResult,
            Model model) {
        personDto.setId(id);
        if (bindingResult.hasErrors()) {
            addAddresses(model);
            return "persons/form";
        }
        Person person = findPerson(id);
        personMapper.updateEntity(personDto, person);
        assignAddress(person, personDto.getAddressId());
        personRepository.save(person);
        return "redirect:/persons";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        if (!personRepository.existsById(id)) {
            throw new PersonNotFoundException(id);
        }
        personRepository.deleteById(id);
        return "redirect:/persons";
    }

    private Person findPerson(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new PersonNotFoundException(id));
    }

    private void assignAddress(Person person, Long addressId) {
        if (addressId == null) {
            person.setAddress(null);
            return;
        }
        person.setAddress(addressRepository.findById(addressId)
                .orElseThrow(() -> new AddressNotFoundException(addressId)));
    }

    private void addAddresses(Model model) {
        model.addAttribute("addresses", addressMapper.toDtoList(addressRepository.findAll()));
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

}
