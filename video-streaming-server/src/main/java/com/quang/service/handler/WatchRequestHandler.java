package com.quang.service.handler;


import com.quang.proto.VideoChunk;
import com.quang.proto.VideoRequest;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class WatchRequestHandler {

    private static final String VIDEO_DIRECTORY = "videos/";

    public void handleWatch(VideoRequest request, StreamObserver<VideoChunk> responseObserver) {
        String fileName = request.getFileName();
        String filePath = VIDEO_DIRECTORY + fileName;

        try {
            if (!Files.exists(Paths.get(filePath))) {
                responseObserver.onError(
                        new IOException("File not found: " + fileName)
                );
                return;
            }

            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            int chunkSize = 64 * 1024; // 64KB
            int sequenceNumber = 0;

            for (int i = 0; i < fileBytes.length; i += chunkSize) {
                int end = Math.min(fileBytes.length, i + chunkSize);
                VideoChunk chunk = VideoChunk.newBuilder()
                        .setContent(com.google.protobuf.ByteString.copyFrom(fileBytes, i, end - i))
                        .setFileName(fileName)
                        .setSequenceNumber(sequenceNumber++)
                        .setIsLastChunk(end == fileBytes.length)
                        .build();
                responseObserver.onNext(chunk);
            }
            responseObserver.onCompleted();
        } catch (IOException e) {
            responseObserver.onError(e);
        }
    }
}

