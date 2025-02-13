package com.sidutti.charlie.cloud.google;

import com.google.cloud.documentai.v1.Document;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.RawDocument;
import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class DocumentService {

    private final DocumentProcessorServiceClient client;
    private final String name;

    public DocumentService(DocumentProcessorServiceClient client,
                           @Qualifier("googleCloudRequestName") String name) {
        this.client = client;
        this.name = name;
    }

    public Document processDocument(InputStream is, String mime){
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
    private String getTextFromBlock(Document.DocumentLayout.DocumentLayoutBlock block, String text) {

        StringBuilder blockTextBuilder = new StringBuilder();
        for(Document.DocumentLayout.DocumentLayoutBlock segment : block.getTextBlock().getBlocksList()){
           if(segment.hasTextBlock()){
               blockTextBuilder.append(segment.getTextBlock().getText());
           }
           getTextFromBlock(segment, blockTextBuilder.toString());
        }
        return blockTextBuilder.toString();

    }
    private Document callGoogleAndExtract(RawDocument document) {
        ProcessRequest request =
                ProcessRequest.newBuilder().setName(name).setRawDocument(document).build();

        return client.processDocument(request).getDocument();

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
