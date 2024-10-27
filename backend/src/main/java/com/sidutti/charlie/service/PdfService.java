package com.sidutti.charlie.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class PdfService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfService.class);


    public Document parseDocument(Path path) {
        TikaDocumentReader parser = new TikaDocumentReader(path.toAbsolutePath().toString());
        LOGGER.debug("Path to File {}", path);
        var parsedDoc = parser.get().getFirst();
        parsedDoc.getMetadata().put("file", path.toAbsolutePath().toString());
        parsedDoc.getMetadata().put("name", path.getFileName().toString());
        return parsedDoc;
    }

}
