package com.example.demo.exception;

public class AddressNotFoundException extends RuntimeException {

    public AddressNotFoundException(Long id) {
        super("Endereço com o ID " + id + " não foi encontrado");
    }
}
