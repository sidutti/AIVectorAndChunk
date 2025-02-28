package com.sidutti.charlie.cloud.google;

import com.azure.ai.documentintelligence.DocumentIntelligenceAsyncClient;
import com.azure.ai.documentintelligence.models.AnalyzeDocumentOptions;
import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.DocumentTable;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.AsyncPollResponse;
import com.google.cloud.documentai.v1.Document;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.RawDocument;
import com.google.protobuf.ByteString;
import com.sidutti.charlie.model.ExtractedDocument;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentService {

    private final DocumentProcessorServiceClient client;
    private final String name;
    private final DocumentIntelligenceAsyncClient azureDocumentClient;

    public DocumentService(DocumentProcessorServiceClient client,
                           @Qualifier("googleCloudRequestName") String name, DocumentIntelligenceAsyncClient azureDocumentClient) {
        this.client = client;
        this.name = name;
        this.azureDocumentClient = azureDocumentClient;
    }

    public Document processDocument(InputStream is, String mime) {
        ByteString content;
        try {
            content = ByteString.copyFrom(is.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        RawDocument document =
                RawDocument.newBuilder().setContent(content).setMimeType(mime).build();
        return callGoogleAndExtract(document);
    }

    private Document callGoogleAndExtract(RawDocument document) {
        ProcessRequest request =
                ProcessRequest.newBuilder().setName(name).setRawDocument(document).build();

        return client.processDocument(request).getDocument();

    }

    private Flux<ExtractedDocument> callAzureAndExtract(InputStream is, String mime) {

        BinaryData docData = BinaryData.fromStream(is);
        AnalyzeDocumentOptions input = new AnalyzeDocumentOptions(docData);
        return azureDocumentClient.beginAnalyzeDocument("prebuilt-layout", input)
                .flatMap(AsyncPollResponse::getFinalResult)
                .map(this::processAzureDocument);
    }

    private ExtractedDocument processAzureDocument(AnalyzeResult result) {
        result.getDocuments().forEach(document -> {
            document.getFields().forEach((s, documentField) -> System.out.println(s + ": " + documentField));
        });


        result.getPages().forEach(documentPage -> {
            System.out.printf("Page has width: %.2f and height: %.2f, measured with unit: %s%n",
                    documentPage.getWidth(),
                    documentPage.getHeight(),
                    documentPage.getUnit());

            // lines
            documentPage.getLines().forEach(documentLine ->
                    System.out.printf("Line '%s' is within a bounding box %s.%n",
                            documentLine.getContent(),
                            documentLine.getPolygon().toString()));

            // selection marks
            documentPage.getSelectionMarks().forEach(documentSelectionMark ->
                    System.out.printf("Selection mark is '%s' and is within a bounding box %s with confidence %.2f.%n",
                            documentSelectionMark.getState().toString(),
                            documentSelectionMark.getPolygon().toString(),
                            documentSelectionMark.getConfidence()));
        });

// tables
        List<DocumentTable> tables = result.getTables();
        for (int i = 0; i < tables.size(); i++) {
            DocumentTable documentTable = tables.get(i);
            System.out.printf("Table %d has %d rows and %d columns.%n", i, documentTable.getRowCount(),
                    documentTable.getColumnCount());
            documentTable.getCells().forEach(documentTableCell -> {
                System.out.printf("Cell '%s', has row index %d and column index %d.%n", documentTableCell.getContent(),
                        documentTableCell.getRowIndex(), documentTableCell.getColumnIndex());
            });
            System.out.println();
        }
        return new ExtractedDocument(new ArrayList<>());
    }


}
