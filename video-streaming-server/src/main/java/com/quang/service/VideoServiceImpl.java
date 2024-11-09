package com.quang.service;



import com.quang.proto.*;
import com.google.protobuf.ByteString;
import com.quang.service.handler.UploadRequestHandler;
import com.quang.service.handler.WatchRequestHandler;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


@GrpcService
public class VideoServiceImpl extends VideoServiceGrpc.VideoServiceImplBase {

    private final UploadRequestHandler uploadHandler;
    private final WatchRequestHandler watchHandler;

    public VideoServiceImpl() {
        this.uploadHandler = new UploadRequestHandler();
        this.watchHandler = new WatchRequestHandler();
    }

    @Override
    public StreamObserver<VideoChunk> upload(StreamObserver<UploadStatus> responseObserver) {
        return uploadHandler.handleUpload(responseObserver);
    }

    @Override
    public void watch(VideoRequest request, StreamObserver<VideoChunk> responseObserver) {
        watchHandler.handleWatch(request, responseObserver);
    }
}


