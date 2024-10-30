package com.sidutti.charlie.cloud.google;

import com.google.cloud.documentai.v1.Document;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.RawDocument;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Component
public class DocumentService {

    private final DocumentProcessorServiceClient client;
    private final String name;

    public DocumentService(DocumentProcessorServiceClient client,
                           @Qualifier("googleCloudRequestName") String name) {
        this.client = client;
        this.name = name;
    }


    public Document processDocument(String filePath) throws IOException {

        byte[] imageFileData = Files.readAllBytes(Paths.get(filePath));
        ByteString content = ByteString.copyFrom(imageFileData);
        RawDocument document =
                RawDocument.newBuilder().setContent(content).setMimeType("application/pdf").build();

        ProcessRequest request =
                ProcessRequest.newBuilder().setName(name).setRawDocument(document).build();

        Document documentResponse = client.processDocument(request).getDocument();
        String text = documentResponse.getText();
        System.out.println("The document contains the following paragraphs:");
        for (Document.Page page : documentResponse.getPagesList()) {
            List<Document.Page.Paragraph> paragraphs = page.getParagraphsList();

            for (Document.Page.Paragraph paragraph : paragraphs) {
                String paragraphText = getText(paragraph.getLayout().getTextAnchor(), text);
                System.out.printf("Paragraph text:\n%s\n", paragraphText);
            }

            // Form parsing provides additional output about
            // form-formatted PDFs. You must create a form
            // processor in the Cloud Console to see full field details.
            System.out.println("The following form key/value pairs were detected:");

            for (Document.Page.FormField field : page.getFormFieldsList()) {
                String fieldName = getText(field.getFieldName().getTextAnchor(), text);
                String fieldValue = getText(field.getFieldValue().getTextAnchor(), text);

                System.out.println("Extracted form fields pair:");
                System.out.printf("\t(%s, %s))\n", fieldName, fieldValue);
            }
        }
        return documentResponse;

    }


    // Extract shards from the text field
    private String getText(Document.TextAnchor textAnchor, String text) {
        if (!textAnchor.getTextSegmentsList().isEmpty()) {
            int startIdx = (int) textAnchor.getTextSegments(0).getStartIndex();
            int endIdx = (int) textAnchor.getTextSegments(0).getEndIndex();
            return text.substring(startIdx, endIdx);
        }
        return "[NO TEXT]";
    }

}
