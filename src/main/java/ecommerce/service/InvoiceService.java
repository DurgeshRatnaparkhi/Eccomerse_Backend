package ecommerce.service;

import ecommerce.entity.Order;

public interface InvoiceService {

    byte[] generateInvoice(Order order);
}
