package com.example.demo.exception;

public class PersonNotFoundException extends RuntimeException {

    public PersonNotFoundException(Long id) {
        super("Pessoa com o ID " + id + " não foi encontrada");
    }
}
