package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.example.demo.model.Address;
import com.example.demo.repository.AddressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddressDataInitializerTest {

    @Mock
    private AddressRepository addressRepository;

    @Test
    void populatesAddressesWhenTableIsEmpty() {
        when(addressRepository.count()).thenReturn(0L);
        AddressDataInitializer initializer = new AddressDataInitializer(addressRepository);

        initializer.run(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Address>> captor = ArgumentCaptor.forClass(List.class);
        verify(addressRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .hasSize(6)
                .allSatisfy(address -> {
                    assertThat(address.getCountry()).isEqualTo("Portugal");
                    assertThat(address.getState()).isEqualTo("Setúbal");
                    assertThat(address.getCity()).isEqualTo("Costa da Caparica");
                });
    }

    @Test
    void doesNotDuplicateExistingAddresses() {
        when(addressRepository.count()).thenReturn(1L);
        AddressDataInitializer initializer = new AddressDataInitializer(addressRepository);

        initializer.run(null);

        verify(addressRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }
}
