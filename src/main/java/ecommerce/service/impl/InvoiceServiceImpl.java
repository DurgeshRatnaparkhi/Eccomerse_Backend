package ecommerce.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import ecommerce.entity.Order;
import ecommerce.entity.OrderItem;
import ecommerce.service.InvoiceService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    @Override
    public byte[] generateInvoice(Order order) {

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Document document = new Document();
            PdfWriter.getInstance(document, out);

            document.open();

            document.add(new Paragraph("INVOICE"));
            document.add(new Paragraph("Order ID: " + order.getId()));
            document.add(new Paragraph("Customer: " + order.getUser().getName()));
            document.add(new Paragraph(" "));

            for (OrderItem item : order.getItems()) {
                document.add(new Paragraph(
                        item.getProduct().getName() +
                                " - Qty: " + item.getQuantity() +
                                " - ₹" + item.getPrice()
                ));
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total Amount: ₹" + order.getTotalAmount()));

            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating invoice");
        }
    }
}