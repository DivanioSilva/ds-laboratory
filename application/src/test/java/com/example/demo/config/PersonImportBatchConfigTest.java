package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.demo.batch.PersonCsvRecord;
import com.example.demo.model.Person;
import com.example.demo.repository.PersonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;

class PersonImportBatchConfigTest {

    private final PersonImportBatchConfig config = new PersonImportBatchConfig();

    @Test
    void readsAllFiftyPeopleFromCsv() throws Exception {
        FlatFileItemReader<PersonCsvRecord> reader = config.personCsvReader("data/persons.csv");
        reader.open(new ExecutionContext());

        int count = 0;
        PersonCsvRecord first = reader.read();
        count++;
        while (reader.read() != null) {
            count++;
        }
        reader.close();

        assertThat(count).isEqualTo(50);
        assertThat(first.getFirstName()).isEqualTo("Matilde");
        assertThat(first.getLastName()).isEqualTo("Silva");
        assertThat(first.getAge()).isEqualTo(34);
    }

    @Test
    void convertsANewCsvRecordToPerson() throws Exception {
        PersonRepository repository = mock(PersonRepository.class);
        PersonCsvRecord record = record("Matilde", "Silva", 34);
        when(repository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndAge(
                "Matilde", "Silva", 34)).thenReturn(false);

        ItemProcessor<PersonCsvRecord, Person> processor = config.personCsvProcessor(repository);
        Person person = processor.process(record);

        assertThat(person).isNotNull();
        assertThat(person.getId()).isNull();
        assertThat(person.getFirstName()).isEqualTo("Matilde");
        assertThat(person.getLastName()).isEqualTo("Silva");
        assertThat(person.getAge()).isEqualTo(34);
    }

    @Test
    void ignoresAPersonThatAlreadyExists() throws Exception {
        PersonRepository repository = mock(PersonRepository.class);
        PersonCsvRecord record = record("Matilde", "Silva", 34);
        when(repository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndAge(
                "Matilde", "Silva", 34)).thenReturn(true);

        ItemProcessor<PersonCsvRecord, Person> processor = config.personCsvProcessor(repository);

        assertThat(processor.process(record)).isNull();
    }

    private PersonCsvRecord record(String firstName, String lastName, int age) {
        PersonCsvRecord record = new PersonCsvRecord();
        record.setFirstName(firstName);
        record.setLastName(lastName);
        record.setAge(age);
        return record;
    }
}
