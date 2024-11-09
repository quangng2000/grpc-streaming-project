package com.quang.service.handler;

import com.quang.proto.VideoChunk;
import com.quang.proto.VideoRequest;
import com.quang.proto.VideoServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class GrpcWatchRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(GrpcWatchRequestHandler.class);

    public Flux<byte[]> handleWatch(VideoServiceGrpc.VideoServiceStub videoClient, String fileName) {
        // Create a Sinks.Many to manage and emit a stream of byte arrays.
        Sinks.Many<byte[]> sink = Sinks.many().multicast().onBackpressureBuffer();

        // Make the gRPC call to stream video chunks from the server.
        videoClient.watch(VideoRequest.newBuilder().setFileName(fileName).build(), new StreamObserver<VideoChunk>() {
            @Override
            public void onNext(VideoChunk value) {
                // Emit each chunk to the Flux.
                logger.info("Received video chunk: sequence number {}", value.getSequenceNumber());
                sink.tryEmitNext(value.getContent().toByteArray());
            }

            @Override
            public void onError(Throwable t) {
                // Signal an error if one occurs.
                logger.error("Error occurred while streaming video: {}", t.getMessage());
                sink.tryEmitError(t);
            }

            @Override
            public void onCompleted() {
                // Complete the Flux once all chunks are received.
                logger.info("Completed streaming video for file: {}", fileName);
                sink.tryEmitComplete();
            }
        });

        // Return the Flux that streams video data.
        return sink.asFlux();
    }
}

