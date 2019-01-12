package com.github.rahulsmehta.fastls.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;


public class EdgeStream implements Iterable<Edge> {
    private final Integer numNodes;
    private final Optional<List<Edge>> maybeEdgeList;
    private final Optional<BufferedReader> maybeReader;

    public EdgeStream(List<Edge> edgeList) {
        this.numNodes = ((int) edgeList.stream()
                .flatMap(e -> Stream.of(e.i, e.j))
                .distinct()
                .count());
        this.maybeEdgeList = Optional.of(edgeList);
        this.maybeReader = Optional.empty();
    }

    public EdgeStream(BufferedReader reader) {
        this.maybeEdgeList = Optional.empty();

        try {
            String top = reader.readLine();
            if (top == null) {
                throw new IllegalArgumentException("Malformed input graph");
            }
            this.numNodes = Integer.parseInt(top);
            this.maybeReader = Optional.of(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Iterator<Edge> iterator() {
        if (maybeEdgeList.isPresent()) {
            return this.getEdgeListIterator();
        } else {
            return this.getReaderIterator();
        }
    }

    private Iterator<Edge> getEdgeListIterator() {
        List<Edge> edgeList = maybeEdgeList.orElseThrow(() ->
                new RuntimeException("Attempted to get edge list iterator without it present"));

        return new Iterator<Edge>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < edgeList.size();
            }

            @Override
            public Edge next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Edge toReturn = edgeList.get(index);
                index++;
                return toReturn;
            }
        };
    }

    private Iterator<Edge> getReaderIterator() {
        BufferedReader reader = maybeReader.orElseThrow(() ->
                new RuntimeException("Attempted to get reader iterator without it present"));

        return new Iterator<Edge>() {
            private boolean isFlushed = false;
            private String next;

            @Override
            public boolean hasNext() {
                try {
                    if (isFlushed) {
                        next = reader.readLine();
                        isFlushed = false;
                    }
                    return next != null;
                } catch (IOException e) {
                    throw new IllegalArgumentException("error loading iterator");
                }
            }

            @Override
            public Edge next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Edge toReturn = parseEdge(next);
                isFlushed = true;
                return toReturn;
            }

            private Edge parseEdge(String edgeString) {
                String[] e = edgeString.split(",");
                return new Edge(Integer.parseInt(e[0]), Integer.parseInt(e[1]));
            }
        };
    }


    public Integer getNumNodes() {
        return numNodes;
    }
}
