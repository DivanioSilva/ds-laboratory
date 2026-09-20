package com.example.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.example.demo.model.Person;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PersonRepositoryTest {

    @Autowired
    private PersonRepository personRepository;

    @Test
    void savesAndReadsPersonFromDatabase() {
        Person person = new Person(null, "Ana", "Silva", 30);

        Person savedPerson = personRepository.saveAndFlush(person);

        assertThat(savedPerson.getId()).isNotNull();
        assertThat(personRepository.findById(savedPerson.getId()))
                .contains(savedPerson);
    }

    @Test
    void findsPeopleContainingFirstNameIgnoringCase() {
        personRepository.save(new Person(null, "Ana", "Silva", 30));
        personRepository.save(new Person(null, "ANA", "Santos", 25));
        personRepository.save(new Person(null, "João", "Costa", 40));

        List<Person> people = personRepository.findByFirstNameContainingIgnoreCase("an");

        assertThat(people)
                .hasSize(2)
                .extracting(Person::getLastName)
                .containsExactlyInAnyOrder("Silva", "Santos");
    }

    @Test
    void searchesByFirstNameLastNameOrAge() {
        Person person = personRepository.save(new Person(null, "Beatriz", "Monteiro", 47));

        assertThat(personRepository.searchByNameOrAge("atriz")).containsExactly(person);
        assertThat(personRepository.searchByNameOrAge("MONTEI")).containsExactly(person);
        assertThat(personRepository.searchByNameOrAge("47")).containsExactly(person);
    }
}
