package com.github.rahulsmehta.fastls.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestStreamingGraph {

    private final Logger LOG = LoggerFactory.getLogger(TestStreamingGraph.class);

    private final Set<Integer> NODE_LIST = ImmutableSet.of(0, 1, 2, 3, 4, 5);

    private final List<Edge> EDGE_LIST = ImmutableList.of(
            new Edge(0, 1),
            new Edge(1, 2),
            new Edge(2, 0),
            new Edge(3, 4),
            new Edge(4, 5),
            new Edge(5, 3));

    private EdgeStream edgeStream;
    private StreamingGraph graph;


    @Before
    public void setupStreamingGraph() {
         edgeStream = new EdgeStream(EDGE_LIST);
         graph = new StreamingGraph(NODE_LIST, edgeStream);
    }

    @Test
    public void testBasicGraph() {
        LookSelectImpl impl = graph.getImpl();
        Map<Integer, Set<Integer>> keyedComponents = impl.stronglyConnectedComponentsKeyed();
        keyedComponents.forEach((id, elts) -> LOG.warn("{}: {}", id, elts));
    }

}
