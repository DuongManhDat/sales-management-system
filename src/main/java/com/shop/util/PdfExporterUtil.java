package com.shop.util;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.shop.model.Invoice;

import java.io.FileOutputStream;
import java.io.IOException;

public class PdfExporterUtil {
    public static void exportInvoice(Invoice invoice, String filePath) {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("HOA DON BAN HANG", titleFont);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(title);
            
            document.add(new Paragraph(" ")); // empty line

            document.add(new Paragraph("Ma HD: " + invoice.getCode()));
            document.add(new Paragraph("Ngay: " + invoice.getInvoiceDate()));
            document.add(new Paragraph("Khach hang ID: " + invoice.getCustomerId()));
            document.add(new Paragraph(" "));
            
            document.add(new Paragraph("Tong tien hang: " + invoice.getSubtotal()));
            document.add(new Paragraph("Giam gia: " + invoice.getDiscountAmt()));
            document.add(new Paragraph("Thanh toan: " + invoice.getTotal()));
            document.add(new Paragraph("Da tra: " + invoice.getPaid()));
            document.add(new Paragraph("Con no: " + invoice.getDebt()));
            
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Cam on quy khach!"));
            
        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }
}
