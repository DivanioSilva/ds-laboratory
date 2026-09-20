package com.example.demo.config;

import java.util.ArrayList;
import java.util.List;

import com.example.demo.model.Address;
import com.example.demo.repository.AddressRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AddressDataInitializer implements ApplicationRunner {

    private final AddressRepository addressRepository;

    public AddressDataInitializer(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (addressRepository.count() > 0) {
            return;
        }

        addressRepository.saveAll(List.of(
                address("Avenida 1.º de Maio", "12", "1.º Esq."),
                address("Rua dos Pescadores", "27", "R/C"),
                address("Avenida General Humberto Delgado", "45A", "3.º Dt.º"),
                address("Rua de Almada", "8", "2.º"),
                address("Praça da Liberdade", "3", "1.º"),
                address("Rua Mestre Manuel", "19B", "4.º Esq.")));
    }

    private Address address(String street, String door, String floor) {
        return new Address(
                null,
                "Portugal",
                "Setúbal",
                "Costa da Caparica",
                street,
                door,
                floor,
                new ArrayList<>());
    }
}
