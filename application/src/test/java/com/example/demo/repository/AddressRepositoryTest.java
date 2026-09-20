package com.example.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import com.example.demo.model.Address;
import com.example.demo.model.Person;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AddressRepositoryTest {

    @Autowired
    private AddressRepository addressRepository;

    @Test
    void persistsAddressWithMultiplePeople() {
        Address address = new Address(
                null,
                "Portugal",
                "Lisboa",
                "Lisboa",
                "Avenida da Liberdade",
                "10A",
                "2.º Esq.",
                new ArrayList<>());
        address.addPerson(new Person(null, "Ana", "Silva", 30));
        address.addPerson(new Person(null, "João", "Santos", 35));

        Address savedAddress = addressRepository.saveAndFlush(address);

        assertThat(savedAddress.getId()).isNotNull();
        assertThat(savedAddress.getPersons())
                .hasSize(2)
                .allMatch(person -> person.getAddress().equals(savedAddress));
        assertThat(savedAddress.getPersons())
                .extracting(Person::getId)
                .doesNotContainNull();
    }
}
