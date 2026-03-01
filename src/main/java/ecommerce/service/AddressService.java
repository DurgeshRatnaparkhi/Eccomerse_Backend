package ecommerce.service;

import ecommerce.dtos.AddressDto;
import ecommerce.entity.Address;
import ecommerce.entity.User;
import ecommerce.repo.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressDto addAddress(AddressDto dto, User user) {

        Address address = Address.builder()
                .fullName(dto.getFullName())
                .mobile(dto.getMobile())
                .street(dto.getStreet())
                .city(dto.getCity())
                .state(dto.getState())
                .pincode(dto.getPincode())
                .user(user)
                .build();

        Address saved = addressRepository.save(address);

        return mapToDto(saved);
    }

    public List<AddressDto> getUserAddresses(User user) {
        return addressRepository.findByUser(user)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public void deleteAddress(Long addressId, User user) {

        Address address = addressRepository
                .findByIdAndUser(addressId, user)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        addressRepository.delete(address);
    }

    private AddressDto mapToDto(Address address) {
        return AddressDto.builder()
                .id(address.getId())
                .fullName(address.getFullName())
                .mobile(address.getMobile())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .pincode(address.getPincode())
                .build();
    }
}


