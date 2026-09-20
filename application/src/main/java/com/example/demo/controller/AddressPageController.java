package com.example.demo.controller;

import com.example.demo.dto.AddressDto;
import com.example.demo.mapper.AddressMapper;
import com.example.demo.repository.AddressRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/addresses")
public class AddressPageController {

    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;

    public AddressPageController(AddressRepository addressRepository, AddressMapper addressMapper) {
        this.addressRepository = addressRepository;
        this.addressMapper = addressMapper;
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("address", new AddressDto());
        return "addresses/form";
    }

    @PostMapping
    public String create(@ModelAttribute("address") AddressDto addressDto) {
        addressRepository.save(addressMapper.toEntity(addressDto));
        return "redirect:/persons";
    }
}
