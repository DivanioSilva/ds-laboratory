package com.example.demo.repository;

import java.util.List;

import com.example.demo.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonRepository extends JpaRepository<Person, Long> {

    List<Person> findByFirstNameContainingIgnoreCase(String firstName);

    @Query("""
            SELECT person FROM Person person
            WHERE LOWER(person.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(person.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
               OR CAST(person.age AS string) LIKE CONCAT('%', :query, '%')
            """)
    List<Person> searchByNameOrAge(@Param("query") String query);

    boolean existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndAge(
            String firstName, String lastName, int age);
}
