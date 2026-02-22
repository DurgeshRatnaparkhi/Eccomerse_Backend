package ecommerce.repo;

import ecommerce.entity.Address;
import ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address,Long> {


    Optional<Address> findByIdAndUser(Long addressId, User user);
}

