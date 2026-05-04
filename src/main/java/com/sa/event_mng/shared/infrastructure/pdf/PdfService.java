package com.sa.event_mng.shared.infrastructure.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sa.event_mng.modules.ordering.domain.model.Order;
import com.sa.event_mng.modules.ordering.domain.model.OrderItem;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfService {

    public byte[] generateOrderInvoice(Order order) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            
            BaseFont baseFont;
            try {
                // Thử tìm font Arial trên Windows hoặc Linux
                String fontPath = "C:/Windows/Fonts/Arial.ttf";
                if (!new java.io.File(fontPath).exists()) {
                    fontPath = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"; // Linux
                }
                if (!new java.io.File(fontPath).exists()) {
                    // Nếu không thấy font nào, dùng font mặc định (không hỗ trợ tiếng Việt tốt nhưng không gây crash)
                    baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
                } else {
                    baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            } catch (Exception e) {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            }

            Font headerFont = new Font(baseFont, 22, Font.BOLD, java.awt.Color.BLACK);
            Font infoFont = new Font(baseFont, 11, Font.NORMAL, java.awt.Color.BLACK);
            Font headFont = new Font(baseFont, 11, Font.BOLD, java.awt.Color.BLACK);
            Font totalFont = new Font(baseFont, 16, Font.BOLD, java.awt.Color.RED);
            Font footerFont = new Font(baseFont, 10, Font.ITALIC, java.awt.Color.GRAY);

            Paragraph header = new Paragraph("E-INVOICE", headerFont);
            header.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(header);
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Order ID: #" + order.getId(), infoFont));
            document.add(new Paragraph("Customer: " + order.getCustomer().getFullName(), infoFont));
            document.add(new Paragraph("Date: " + (order.getPaidAt() != null ? order.getPaidAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : order.getOrderDate()), infoFont));
            document.add(new Paragraph("Payment: " + order.getPaymentMethod(), infoFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4f, 1f, 2f, 2.5f});

            String[] headers = {"Event & Ticket", "Quantity", "Price", "Total"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
                cell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                cell.setPadding(8);
                cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                table.addCell(cell);
            }

            for (OrderItem item : order.getItems()) {
                table.addCell(new Phrase(item.getTicketType().getEvent().getName() + "\n(" + item.getTicketType().getName() + ")", infoFont));
                PdfPCell qtyCell = new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), infoFont));
                qtyCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                table.addCell(qtyCell);
                
                table.addCell(new Phrase(String.format("%,.0f", item.getUnitPrice()) + " d", infoFont));
                table.addCell(new Phrase(String.format("%,.0f", item.getSubtotal()) + " d", infoFont));
            }
            document.add(table);

            Paragraph totals = new Paragraph();
            totals.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
            java.math.BigDecimal subTotalVal = order.getTotalAmount().add(order.getDiscountAmount() != null ? order.getDiscountAmount() : java.math.BigDecimal.ZERO);
            totals.add(new Phrase("Subtotal: " + String.format("%,.0f", subTotalVal) + " d\n", infoFont));
            
            if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                totals.add(new Phrase("Discount: -" + String.format("%,.0f", order.getDiscountAmount()) + " d (" + order.getVoucherCode() + ")\n", infoFont));
            }
            
            totals.add(new Phrase("\nTOTAL PAID: " + String.format("%,.0f", order.getTotalAmount()) + " VND", totalFont));
            document.add(totals);

            document.add(new Paragraph("\n\n"));
            Paragraph footer = new Paragraph("Thank you for your purchase!", footerFont);
            footer.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }
}
