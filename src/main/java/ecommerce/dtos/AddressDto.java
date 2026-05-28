package ecommerce.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AddressDto {

    private Long id;
    private String fullName;
    private String mobile;
    private String street;
    private String city;
    private String state;
    private String pincode;
}
