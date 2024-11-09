package com.quang.service.handler;

import com.google.protobuf.ByteString;
import com.quang.proto.UploadStatus;
import com.quang.proto.VideoChunk;
import com.quang.proto.VideoServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Component
public class GrpcUploadRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(GrpcUploadRequestHandler.class);

    public String handleUpload(VideoServiceGrpc.VideoServiceStub videoClient, byte[] videoContent, String fileName) throws InterruptedException {
        CountDownLatch finishLatch = new CountDownLatch(1);
        UploadStatus.Builder status = UploadStatus.newBuilder();

        StreamObserver<UploadStatus> responseObserver = new StreamObserver<UploadStatus>() {
            @Override
            public void onNext(UploadStatus value) {
                status.mergeFrom(value);
            }

            @Override
            public void onError(Throwable t) {
                logger.error("Error received from gRPC server during upload: {}", t.getMessage());
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                finishLatch.countDown();
            }
        };

        StreamObserver<VideoChunk> requestObserver = videoClient.upload(responseObserver);
        try {
            int chunkSize = 2 * 1024 * 1024; // 2MB per chunk
            int totalBytes = videoContent.length;

            // Send video data in chunks
            for (int start = 0; start < totalBytes; start += chunkSize) {
                int end = Math.min(totalBytes, start + chunkSize);
                byte[] chunk = new byte[end - start];
                System.arraycopy(videoContent, start, chunk, 0, end - start);

                // Log chunk details
                logger.info("Sending chunk: {} to {}", start, end);

                requestObserver.onNext(VideoChunk.newBuilder()
                        .setContent(ByteString.copyFrom(chunk))
                        .setFileName(fileName)
                        .setSequenceNumber(start / chunkSize)
                        .setIsLastChunk(end == totalBytes)
                        .build());
            }

            // Notify that all chunks are sent
            requestObserver.onCompleted();
        } catch (RuntimeException e) {
            logger.error("Exception during upload: {}", e.getMessage(), e);
            requestObserver.onError(e);
            throw e;
        }

        finishLatch.await();
        if (status.getSuccess()) {
            logger.info("Upload successful for file: {}", fileName);
            return "Upload successful: " + fileName;
        } else {
            logger.error("Upload failed for file: {}", fileName);
            return "Upload failed: " + fileName;
        }
    }
}

