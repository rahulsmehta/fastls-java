package com.github.rahulsmehta.fastls.api;

import org.jheaps.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class LookSelectImpl {

    private final Logger LOG = LoggerFactory.getLogger(LookSelectImpl.class);
    private final int BUFFER_SIZE = 8 * 1024;
    private final String BASE_NAME = UUID.randomUUID().toString();
    private final String FILE_PATTERN = "./build/resources/fastls.%s.%d";

    private StreamingGraph graph;
    private EdgeStream currentStream;
    private LSTree tree;
    private int currentPhase;


    public LookSelectImpl(StreamingGraph graph) {
        this.graph = graph;
        this.currentStream = graph.getEdgeStream();
        this.currentPhase = 0;

        this.tree = new LSTree(graph.getNodes());
    }

    private boolean streamingPhase() {
        this.tree.startPhase();
        int nextStreamSize = 0;

        BufferedWriter writer;
        FileWriter fileWriter;
        String OUT_FILE_NAME = String.format(FILE_PATTERN, BASE_NAME, this.currentPhase);

        try {
            File file = new File(OUT_FILE_NAME);
            fileWriter = new FileWriter(file);
            writer = new BufferedWriter(fileWriter);

            String toWrite = String.format("%d\n", this.graph.getNodes().size());
            writer.write(toWrite);

            for (Edge edge : this.currentStream) {
                Optional<Edge> maybeEdge = this.tree.processEdge(edge);
                if (maybeEdge.isPresent()) {
                    toWrite = maybeEdge.get().toString() + "\n";
                    writer.write(toWrite);
                    nextStreamSize++;
                }
            }
            writer.close();
            fileWriter.close();

            BufferedReader nextStream = new BufferedReader(
                    new FileReader(file),
                    BUFFER_SIZE
            );

            this.currentStream = new EdgeStream(nextStream);
            this.currentPhase++;

            return nextStreamSize == 0 || this.tree.isComplete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @VisibleForTesting
    Map<Integer, Set<Integer>> stronglyConnectedComponentsKeyed() {
        boolean isDone;
        do {
            isDone = this.streamingPhase();
        } while (!isDone);
        return this.tree.getKeyedComponents();
    }

    public List<Set<Integer>> stronglyConnectedComponents() {
        boolean isDone;
        do {
            isDone = this.streamingPhase();
        } while (!isDone);
        return this.tree.stronglyConnectedComponents();
    }
}
