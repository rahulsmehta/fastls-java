package com.github.rahulsmehta.fastls.api;

import org.jheaps.annotations.VisibleForTesting;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StreamingGraph {

    private final Set<Integer> nodes;
    private final EdgeStream edgeStream;


    public StreamingGraph(Set<Integer> nodes, EdgeStream edgeStream) {
        this.nodes = nodes;
        this.edgeStream = edgeStream;
    }

    public StreamingGraph(EdgeStream edgeStream) {
        this.nodes = IntStream.range(0, edgeStream.getNumNodes())
                .boxed()
                .collect(Collectors.toSet());
        this.edgeStream = edgeStream;
    }

    public Set<Integer> getNodes() {
        return nodes;
    }

    public EdgeStream getEdgeStream() {
        return edgeStream;
    }

    @VisibleForTesting
    LookSelectImpl getImpl() {
        return new LookSelectImpl(this);
    }

    public List<Set<Integer>> stronglyConnectedComponents() {
        LookSelectImpl lookSelect = new LookSelectImpl(this);
        return lookSelect.stronglyConnectedComponents();
    }
}
