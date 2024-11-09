package com.quang.service.handler;

import com.quang.proto.UploadStatus;
import com.quang.proto.VideoChunk;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class UploadRequestHandler {

    private static final String VIDEO_DIRECTORY = "videos/";

    public StreamObserver<VideoChunk> handleUpload(StreamObserver<UploadStatus> responseObserver) {
        return new StreamObserver<VideoChunk>() {
            private FileOutputStream outputStream;
            private String fileName;

            @Override
            public void onNext(VideoChunk chunk) {
                try {
                    if (outputStream == null) {
                        // Initialize the output stream on the first chunk
                        fileName = chunk.getFileName();
                        Files.createDirectories(Paths.get(VIDEO_DIRECTORY)); // Ensure directory exists
                        outputStream = new FileOutputStream(VIDEO_DIRECTORY + fileName);
                    }

                    // Write the content of each chunk to the output stream
                    outputStream.write(chunk.getContent().toByteArray());

                } catch (IOException e) {
                    responseObserver.onError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                // Handle error, close the output stream and log the error message
                closeStream();
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                // Close the output stream once all chunks have been received
                closeStream();

                // Build the success response
                UploadStatus status = UploadStatus.newBuilder()
                        .setSuccess(true)
                        .setMessage("Upload successful for file: " + fileName)
                        .build();

                // Send success response to the client
                responseObserver.onNext(status);
                responseObserver.onCompleted();
            }

            private void closeStream() {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        // Log the error if closing the stream fails
                        System.err.println("Error closing stream for file: " + fileName + " - " + e.getMessage());
                    }
                }
            }
        };
    }
}
