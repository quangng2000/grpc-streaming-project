package com.quang.service;

import com.quang.proto.VideoServiceGrpc;
import com.quang.service.handler.GrpcUploadRequestHandler;
import com.quang.service.handler.GrpcWatchRequestHandler;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class GrpcVideoClientService {
    private static final Logger logger = LoggerFactory.getLogger(GrpcVideoClientService.class);

    @GrpcClient("video-service")
    private VideoServiceGrpc.VideoServiceStub videoClient;

    private final GrpcUploadRequestHandler uploadRequestHandler;
    private final GrpcWatchRequestHandler watchRequestHandler;

    @Autowired
    public GrpcVideoClientService(GrpcUploadRequestHandler uploadRequestHandler, GrpcWatchRequestHandler watchRequestHandler) {
        this.uploadRequestHandler = uploadRequestHandler;
        this.watchRequestHandler = watchRequestHandler;
    }

    public String uploadVideo(byte[] videoContent, String fileName) throws InterruptedException {
        if (videoContent == null || videoContent.length == 0) {
            logger.error("Upload failed: Video content is empty.");
            return "Upload failed: Video content is empty.";
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            logger.error("Upload failed: File name is not provided.");
            return "Upload failed: File name is not provided.";
        }

        // Delegate the upload handling to the request handler
        return uploadRequestHandler.handleUpload(videoClient, videoContent, fileName);
    }

    public Flux<byte[]> watchVideoReactive(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            logger.error("Watch failed: File name is not provided.");
            return Flux.error(new IllegalArgumentException("File name is required"));
        }

        // Delegate the watch handling to the request handler
        return watchRequestHandler.handleWatch(videoClient, fileName);
    }
}


