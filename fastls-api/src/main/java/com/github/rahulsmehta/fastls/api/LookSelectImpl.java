package com.github.rahulsmehta.fastls.api;

import org.jheaps.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class LookSelectImpl {

    private final Logger LOG = LoggerFactory.getLogger(LookSelectImpl.class);
    private final StreamingGraph graph;

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
        LOG.warn("Starting phase {}", currentPhase);
        this.tree.startPhase();
        ArrayList<Edge> newEdges = new ArrayList<>();
        int nextStreamSize = 0;

        for (Edge edge : this.currentStream) {
            Optional<Edge> maybeEdge = this.tree.processEdge(edge);
            if (maybeEdge.isPresent()) {
                newEdges.add(edge);
                nextStreamSize++;
            }
        }

        LOG.warn("Edges in next phase: {}", newEdges);

        this.currentStream = new EdgeStream(newEdges);
        this.currentPhase++;
        return nextStreamSize == 0 || this.tree.isComplete();
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
