package com.sidutti.charlie.service;

import com.sidutti.charlie.model.Basics;
import org.bson.types.ObjectId;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Component
public class FileReaderService {

    public Flux<String> readFile(String fileName) {
        StringDecoder stringDecoder = StringDecoder.textPlainOnly();
        return DataBufferUtils.readAsynchronousFileChannel(() -> AsynchronousFileChannel.open(Path.of(fileName),
                        StandardOpenOption.READ), DefaultDataBufferFactory.sharedInstance, 4096)
                .transform(dataBufferFlux -> stringDecoder.decode(dataBufferFlux, null, null, null));
    }

    public List<String> splitLine(String line) {

        return List.of(line.split("\t"));
    }

    public Basics getBasics(List<String> lines) {
        return new Basics(new ObjectId(), lines.get(0), lines.get(1), lines.get(2), lines.get(3), lines.get(4), parseInt(lines.get(5)), parseInt(lines.get(6)), parseInt(lines.get(7)), List.of(lines.get(8).split(",")));
    }

    private int parseInt(String input) {
        try {
            return Integer.parseInt(input);
        } catch (Exception e) {
            return 0;
        }
    }
}
