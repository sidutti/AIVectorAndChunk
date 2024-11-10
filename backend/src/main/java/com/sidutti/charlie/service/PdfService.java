package com.sidutti.charlie.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class PdfService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfService.class);


    public Document parseDocument(Path path) {
        FileSystemResource resource = new FileSystemResource(path.toAbsolutePath().toString());
        var parser = new TikaDocumentReader(resource);
        LOGGER.debug("Path to File {}", path);
        var parsedDoc = parser.get().getFirst();
        parsedDoc.getMetadata().put("file", path.toAbsolutePath().toString());
        parsedDoc.getMetadata().put("name", path.getFileName().toString());
        return parsedDoc;
    }

    public Document parseDocument(Resource resource) {
        return (new TikaDocumentReader(resource)).get().getFirst();
    }

}
