package com.sidutti.charlie.cloud.google;

import com.google.cloud.documentai.v1.Document;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.RawDocument;
import com.google.protobuf.ByteString;
import com.sidutti.charlie.model.ExtractedDocument;
import com.sidutti.charlie.model.Page;
import com.sidutti.charlie.model.Paragraph;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DocumentService {

    private final DocumentProcessorServiceClient client;
    private final String name;

    public DocumentService(DocumentProcessorServiceClient client,
                           @Qualifier("googleCloudRequestName") String name) {
        this.client = client;
        this.name = name;
    }


    public ExtractedDocument processDocument(String filePath) throws IOException {

        byte[] imageFileData = Files.readAllBytes(Paths.get(filePath));
        ByteString content = ByteString.copyFrom(imageFileData);
        RawDocument document =
                RawDocument.newBuilder().setContent(content).setMimeType("application/pdf").build();

        ProcessRequest request =
                ProcessRequest.newBuilder().setName(name).setRawDocument(document).build();

        Document documentResponse = client.processDocument(request).getDocument();
        String text = documentResponse.getText();

        List<Page> pages = new ArrayList<>();
        for (Document.Page page : documentResponse.getPagesList()) {
            List<Document.Page.Paragraph> paragraphs = page.getParagraphsList();
            List<Paragraph> paras = new ArrayList<>();
            for (Document.Page.Paragraph paragraph : paragraphs) {
                String paragraphText = getText(paragraph.getLayout().getTextAnchor(), text);
                paras.add(new Paragraph(paragraphText));
            }
            Map<String, String> fields = new HashMap<>();
            for (Document.Page.FormField field : page.getFormFieldsList()) {
                String fieldName = getText(field.getFieldName().getTextAnchor(), text);
                String fieldValue = getText(field.getFieldValue().getTextAnchor(), text);
                fields.put(fieldName, fieldValue);
            }
            pages.add(new Page(paras, fields));
        }
        return new ExtractedDocument(pages);
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
