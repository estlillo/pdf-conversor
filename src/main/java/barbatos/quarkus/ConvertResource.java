package barbatos.quarkus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/pdf")
public class ConvertResource {

    public static class ConvertPdfRequest {
        public String html;
    }

    @POST
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Consumes(MediaType.TEXT_HTML)
    @Path("/convert")
    public Response convertHtmlToPdf(String htmlContent) {

        try {
            String inputHTML = htmlContent;

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
                    inputHTML.getBytes(StandardCharsets.UTF_8));

            File tempFile = File.createTempFile("temp", ".html");
            java.nio.file.Files.copy(byteArrayInputStream, tempFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            FileInputStream fileInputStream = new FileInputStream(tempFile);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfDocument pdfDocument = new PdfDocument(new PdfWriter(outputStream));

            pdfDocument.setDefaultPageSize(PageSize.A3);

            ConverterProperties converterProperties = new ConverterProperties();
            converterProperties.setCharset("UTF-8");

            HtmlConverter.convertToPdf(fileInputStream, pdfDocument, converterProperties);

            ByteArrayOutputStream outputStreamResult = new ByteArrayOutputStream();

            PdfDocument resultantDocument = new PdfDocument(new PdfWriter(outputStreamResult));
            resultantDocument.setDefaultPageSize(PageSize.A4);
            pdfDocument = new PdfDocument(new PdfReader(new ByteArrayInputStream(outputStream.toByteArray())));
            for (int i = 1; i <= pdfDocument.getNumberOfPages(); i++) {
                PdfPage page = pdfDocument.getPage(i);
                PdfFormXObject formXObject = page.copyAsFormXObject(resultantDocument);
                PdfCanvas pdfCanvas = new PdfCanvas(resultantDocument.addNewPage());
                pdfCanvas.addXObjectWithTransformationMatrix(formXObject, 0.7f, 0, 0, 0.7f,
                        6, 0);
            }

            fileInputStream.close();
            tempFile.delete();

            pdfDocument.close();
            resultantDocument.close();

            // Descargar el archivo PDF
            InputStream fileInputStream2 = new ByteArrayInputStream(outputStreamResult.toByteArray());
            Response.ResponseBuilder response = Response.ok(fileInputStream2);
            response.header("Content-Disposition", "attachment; filename=output.pdf");

            return response.build();
        } catch (IOException e) {
            return Response.serverError().entity("Error converting HTML to PDF: " + e.getMessage()).build();
        }
    }
}
