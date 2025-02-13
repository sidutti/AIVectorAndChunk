package com.sidutti.charlie.cloud.google;


import com.google.api.gax.grpc.GrpcCallContext;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.paging.Page;
import com.google.api.gax.rpc.ApiCallContext;
import com.google.cloud.documentai.v1.BatchDocumentsInputConfig;
import com.google.cloud.documentai.v1.BatchProcessMetadata;
import com.google.cloud.documentai.v1.BatchProcessRequest;
import com.google.cloud.documentai.v1.BatchProcessResponse;
import com.google.cloud.documentai.v1.Document;
import com.google.cloud.documentai.v1.DocumentOutputConfig;
import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.GcsDocument;
import com.google.cloud.documentai.v1.GcsDocuments;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.protobuf.util.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class BatchDocumentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BatchDocumentService.class);
    private final String bucketName;
    private final Storage storage;
    private final DocumentProcessorServiceClient client;
    private final String name;

    public BatchDocumentService(@Value("${cloud.google.bucket}") String bucketName,
                                Storage storage,
                                DocumentProcessorServiceClient client,
                                @Qualifier("googleCloudRequestName") String name) {
        this.bucketName = bucketName;
        this.storage = storage;
        this.client = client;
        this.name = name;
    }

    public String uploadFileToGCS(InputStream input, String fileName) {
        try {
            BlobId blobId = BlobId.of(bucketName, fileName + UUID.randomUUID());
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
            Blob data = storage.createFrom(blobInfo, input, Storage.BlobWriteOption.crc32cMatch());
            return data.getName();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Mono<BatchProcessResponse> processFile(String mimeType,
                                                  String fileName,
                                                  String parser,
                                                  String uuid) {
        String outputGuid = "Output" + uuid;
        BatchDocumentsInputConfig inputConfig = createInputConfig(mimeType, fileName);
        DocumentOutputConfig outputConfig = createOutputConfig(outputGuid);
        BatchProcessRequest request = createBatchProcessRequest(parser, inputConfig, outputConfig);

        ApiCallContext timeContex = GrpcCallContext.createDefault()
                .withTimeoutDuration(Duration.ofMinutes(10L));
        OperationFuture<BatchProcessResponse, BatchProcessMetadata> process = client
                .batchProcessDocumentsOperationCallable()
                .withDefaultCallContext(timeContex)
                .futureCall(request);
        return Mono.fromCallable(() -> {
            try {
                return process.get(10, TimeUnit.MINUTES);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public List<Document> processOutput(String uuid, String fileName) {
        String outputGuid = "Output" + uuid + "/";
        Bucket bucket = storage.get(bucketName);
        Page<Blob> blobs = bucket.list(Storage.BlobListOption.prefix(outputGuid));
        List<Document> documents = new ArrayList<>();
        blobs.iterateAll().forEach(blob -> {
            if (!blob.isDirectory()) {
                try {
                    Blob outputBlob = storage.get(BlobId.of(bucketName, blob.getName()));
                    Document.Builder document = Document.newBuilder();
                    JsonFormat.parser().merge(Channels.newReader(outputBlob.reader(), StandardCharsets.UTF_8), document);
                    documents.add(document.build());
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
                storage.delete(BlobId.of(bucketName,  blob.getName()));
                storage.delete(fileName+uuid);
            }
        });
        return documents;
    }

    private BatchProcessRequest createBatchProcessRequest(String parser, BatchDocumentsInputConfig inputConfig, DocumentOutputConfig outputConfig) {
        return BatchProcessRequest.newBuilder()
                .setName(name)
                .setInputDocuments(inputConfig)
                .setDocumentOutputConfig(outputConfig)
                .build();
    }

    private DocumentOutputConfig createOutputConfig(String outputGuid) {
        String fullPath = String.format("gs://%s/%s", bucketName, outputGuid);
        DocumentOutputConfig.GcsOutputConfig config = DocumentOutputConfig.GcsOutputConfig
                .newBuilder()
                .setGcsUri(fullPath)
                .build();
        return DocumentOutputConfig.newBuilder().setGcsOutputConfig(config).build();
    }

    private BatchDocumentsInputConfig createInputConfig(String mimeType, String fileName) {
        GcsDocument doc = GcsDocument.newBuilder()
                .setGcsUri("gs://" + bucketName + "/" + fileName)
                .setMimeType(mimeType)
                .build();
        GcsDocuments docs = GcsDocuments.newBuilder()
                .addDocuments(doc)
                .build();
        return BatchDocumentsInputConfig.newBuilder()
                .setGcsDocuments(docs)
                .build();
    }
}
