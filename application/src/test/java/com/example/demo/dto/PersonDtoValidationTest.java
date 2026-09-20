package com.example.demo.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PersonDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void rejectsNullAge() {
        PersonDto person = new PersonDto(null, "Ana", "Silva", null);

        Set<ConstraintViolation<PersonDto>> violations = validator.validate(person);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("age");
    }

    @Test
    void rejectsZeroAge() {
        PersonDto person = new PersonDto(null, "Ana", "Silva", 0);

        Set<ConstraintViolation<PersonDto>> violations = validator.validate(person);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessageTemplate)
                .contains("{person.age.minimum}");
    }

    @Test
    void rejectsNegativeAge() {
        PersonDto person = new PersonDto(null, "Ana", "Silva", -1);

        Set<ConstraintViolation<PersonDto>> violations = validator.validate(person);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessageTemplate)
                .contains("{person.age.minimum}");
    }

    @Test
    void acceptsPositiveAge() {
        PersonDto person = new PersonDto(null, "Ana", "Silva", 1);

        assertThat(validator.validate(person)).isEmpty();
    }
}
