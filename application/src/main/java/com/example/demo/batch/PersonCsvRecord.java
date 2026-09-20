package com.example.demo.batch;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PersonCsvRecord {

    private String firstName;
    private String lastName;
    private Integer age;
}
