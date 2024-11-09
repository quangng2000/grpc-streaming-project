package com.quang.controller;




import com.quang.service.GrpcVideoClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

@RestController
@RequestMapping("/api/client/videos")
public class VideoClientRestController {

    private final GrpcVideoClientService grpcVideoClientService;

    @Autowired
    public VideoClientRestController(GrpcVideoClientService grpcVideoClientService) {
        this.grpcVideoClientService = grpcVideoClientService;
    }

    @PostMapping("/upload")
    public Mono<ResponseEntity<String>> uploadVideo(@RequestPart("file") Mono<FilePart> filePartMono) {
        return filePartMono
                .flatMap(filePart -> {
                    return filePart.content()
                            .collectList()
                            .flatMap(dataBuffers -> {
                                // Combine all DataBuffers into one byte array
                                byte[] content = dataBuffers.stream()
                                        .map(dataBuffer -> {
                                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                            dataBuffer.read(bytes);
                                            return bytes;
                                        })
                                        .reduce(new byte[0], this::combineBytes);

                                try {
                                    return Mono.just(grpcVideoClientService.uploadVideo(content, filePart.filename()));
                                } catch (Exception e) {
                                    return Mono.error(e);
                                }
                            });
                })
                .map(response -> ResponseEntity.ok(response))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(e.getMessage())));
    }

    private byte[] combineBytes(byte[] a, byte[] b) {
        byte[] combined = new byte[a.length + b.length];
        System.arraycopy(a, 0, combined, 0, a.length);
        System.arraycopy(b, 0, combined, a.length, b.length);
        return combined;
    }

    @GetMapping(value = "/watch/{fileName}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public Flux<byte[]> watchVideo(@PathVariable("fileName") String fileName) throws IOException, InterruptedException {
        return grpcVideoClientService.watchVideoReactive(fileName);
    }
}


