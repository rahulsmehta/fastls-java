package com.github.rahulsmehta.fastls.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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


//    @Before
    public void setupStreamingGraph() {
        edgeStream = new EdgeStream(EDGE_LIST);
        graph = new StreamingGraph(NODE_LIST, edgeStream);
    }

    public void testBasicGraph() {
        LookSelectImpl impl = graph.getImpl();
        Map<Integer, Set<Integer>> keyedComponents = impl.stronglyConnectedComponentsKeyed();
        keyedComponents.forEach((id, elts) -> LOG.warn("{}: {}", id, elts));
    }

    @Test
    public void testSmallGraph3() {

        StreamingGraph fileGraph = loadGraph("test_graph_very_large.txt");

        LookSelectImpl impl = fileGraph.getImpl();
        Map<Integer, Set<Integer>> keyedComponents = impl.stronglyConnectedComponentsKeyed();
        LOG.warn("{} components", keyedComponents.keySet().size());
        keyedComponents.forEach((id, elts) -> LOG.warn("{}: {}", id, elts));

        assertEquals(1, keyedComponents.keySet().size());
    }

    private StreamingGraph loadGraph(String fileName) {
        int bufferSize = 8 * 1024;

        URL url = Thread.currentThread().getContextClassLoader().getResource(fileName);
        File file = new File(url.getPath());
        try {

            BufferedReader bufferedReader = new BufferedReader(
                    new FileReader(file),
                    bufferSize
            );

            String line = bufferedReader.readLine();
            int numNodes = Integer.parseInt(line);
            Set<Integer> nodes = IntStream.range(0, numNodes).boxed()
                    .collect(Collectors.toSet());

            List<Edge> edges = Lists.newLinkedList();
            while (line != null) {
                line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                String[] tokens = line.split(",");
                Edge toAdd = new Edge(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));
                edges.add(toAdd);
            }

            EdgeStream stream = new EdgeStream(edges);
            return new StreamingGraph(nodes, stream);

        } catch (IOException e) {
            throw new IllegalArgumentException("Bad file");
        }

    }
}
