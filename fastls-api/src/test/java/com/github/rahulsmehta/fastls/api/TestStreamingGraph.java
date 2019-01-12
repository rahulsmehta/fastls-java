package com.github.rahulsmehta.fastls.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.GabowStrongConnectivityInspector;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.builder.GraphBuilder;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    private final String SMALL_GRAPH_FILE_1 = "small_1.txt";
    private final String SMALL_GRAPH_FILE_4 = "small_2.txt";
    private final String SMALL_GRAPH_FILE_2 = "small_3.txt";
    private final String SMALL_GRAPH_FILE_3 = "small_4.txt";
    private final String MED_GRAPH_FILE = "medium_1.txt";
    private final String BIG_GRAPH_FILE = "large_1.txt";

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
        assertEquals(2, keyedComponents.keySet().size());

        List<Set<Integer>> rawComponents = ImmutableList.<Set<Integer>>builder()
                .addAll(keyedComponents.values())
                .build();
        List<Set<Integer>> components = canonize(rawComponents);

        assertEquals(components.get(0), ImmutableSet.of(0, 1, 2));
        assertEquals(components.get(1), ImmutableSet.of(3, 4, 5));
    }


    @Test
    public void testSmallGraph1() {
        List<Set<Integer>> jgraphtComponents = computeSCCJGraphT(SMALL_GRAPH_FILE_1);
        List<Set<Integer>> lsComponents = computeSCCLookSelect(SMALL_GRAPH_FILE_1);
        assertEquals(jgraphtComponents.size(), lsComponents.size());
        assertTrue(checkComponentLists(jgraphtComponents, lsComponents));
    }

    @Test
    public void testSmallGraph2() {
        List<Set<Integer>> jgraphtComponents = computeSCCJGraphT(SMALL_GRAPH_FILE_2);
        List<Set<Integer>> lsComponents = computeSCCLookSelect(SMALL_GRAPH_FILE_2);
        assertEquals(jgraphtComponents.size(), lsComponents.size());
        assertTrue(checkComponentLists(jgraphtComponents, lsComponents));
    }

    @Test
    public void testSmallGraph3() {
        List<Set<Integer>> jgraphtComponents = computeSCCJGraphT(SMALL_GRAPH_FILE_3);
        List<Set<Integer>> lsComponents = computeSCCLookSelect(SMALL_GRAPH_FILE_3);
        assertEquals(jgraphtComponents.size(), lsComponents.size());
        assertTrue(checkComponentLists(jgraphtComponents, lsComponents));
    }

    @Test
    public void testSmallGraph4() {
        List<Set<Integer>> jgraphtComponents = computeSCCJGraphT(SMALL_GRAPH_FILE_4);
        List<Set<Integer>> lsComponents = computeSCCLookSelect(SMALL_GRAPH_FILE_4);
        assertEquals(jgraphtComponents.size(), lsComponents.size());
        assertTrue(checkComponentLists(jgraphtComponents, lsComponents));
    }

    @Test
    public void testMediumGraph() {
        List<Set<Integer>> jgraphtComponents = computeSCCJGraphT(MED_GRAPH_FILE);
        List<Set<Integer>> lsComponents = computeSCCLookSelect(MED_GRAPH_FILE);
        assertEquals(jgraphtComponents.size(), lsComponents.size());
        assertTrue(checkComponentLists(jgraphtComponents, lsComponents));
    }

    @Test
    public void testLargeGraph() {
        List<Set<Integer>> jgraphtComponents = computeSCCJGraphT(BIG_GRAPH_FILE);
        List<Set<Integer>> lsComponents = computeSCCLookSelect(BIG_GRAPH_FILE);
        assertEquals(jgraphtComponents.size(), lsComponents.size());
        assertTrue(checkComponentLists(jgraphtComponents, lsComponents));
    }

    private static boolean checkComponentLists(List<Set<Integer>> c1, List<Set<Integer>> c2) {
        if (c1 == c2 || c1.equals(c2)) {
            return true;
        }
        if (c1.size() != c2.size()) {
            return false;
        }
        List<Set<Integer>> n1 = canonize(c1);
        List<Set<Integer>> n2 = canonize(c2);

        // we know |n1| == |n2| at this point so we can safely assume that
        for (int i = 0; i < n1.size(); i++) {
            Set<Integer> component1 = n1.get(i);
            Set<Integer> component2 = n2.get(i);
            if (!component1.equals(component2)) {
                return false;
            }
        }
        return true;
    }

    private static List<Set<Integer>> canonize(List<Set<Integer>> c) {
        return c.stream()
                .sorted((s1, s2) -> {
                    int m1 = Collections.min(s1);
                    int m2 = Collections.min(s2);
                    return m1 - m2;
                }).collect(Collectors.toList());

    }

    private static List<Set<Integer>> computeSCCJGraphT(String graphName) {
        StreamingGraph fileGraph = loadGraph(graphName);
        GraphBuilder<Integer, DefaultEdge, Graph<Integer, DefaultEdge>> builder = new GraphBuilder<>(buildEmptySimpleGraph());
        for (Integer node : fileGraph.getNodes()) {
            builder.addVertex(node);
        }
        for (Edge e : fileGraph.getEdgeStream()) {
            builder.addEdge(e.i, e.j);
        }

        Graph<Integer, DefaultEdge> dg = builder.build();

        StrongConnectivityAlgorithm ccAlg = new GabowStrongConnectivityInspector(dg);
        return ccAlg.stronglyConnectedSets();
    }

    private static Graph<Integer, DefaultEdge> buildEmptySimpleGraph() {
        return GraphTypeBuilder
                .<Integer, DefaultEdge>directed().allowingMultipleEdges(false)
                .allowingSelfLoops(true).edgeClass(DefaultEdge.class).weighted(false).buildGraph();
    }

    private static List<Set<Integer>> computeSCCLookSelect(String graphName) {
        StreamingGraph fileGraph = loadGraph(graphName);
        LookSelectImpl impl = fileGraph.getImpl();
        Map<Integer, Set<Integer>> keyedComponents = impl.stronglyConnectedComponentsKeyed();
        return ImmutableList.<Set<Integer>>builder()
                .addAll(keyedComponents.values())
                .build();
    }

    private static StreamingGraph loadGraph(String fileName) {
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
