package ecommerce.exception;

public class AddressNotFoundException extends RuntimeException{

    public AddressNotFoundException(String msg){
        super(msg);
    }
}
