package com.example.demo.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PersonTest {

    @Test
    void lombokGeneratesConstructorAndAccessors() {
        Person person = new Person(null, "Ana", "Silva", 30);

        assertThat(person.getFirstName()).isEqualTo("Ana");
        assertThat(person.getLastName()).isEqualTo("Silva");
        assertThat(person.getAge()).isEqualTo(30);

        person.setAge(31);
        assertThat(person.getAge()).isEqualTo(31);
    }
}
