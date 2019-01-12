package com.github.rahulsmehta.fastls.api;

import java.io.BufferedReader;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;


public class EdgeStream implements Iterable<Edge> {


    private final Optional<List<Edge>> maybeEdgeList;
    private final Optional<BufferedReader> maybeReader;

    public EdgeStream(List<Edge> edgeList) {
        this.maybeEdgeList = Optional.of(edgeList);
        this.maybeReader = Optional.empty();
    }

    public EdgeStream(BufferedReader reader) {
        this.maybeReader = Optional.of(reader);
        this.maybeEdgeList = Optional.empty();
    }

    public Iterator<Edge> iterator() {
        if (maybeEdgeList.isPresent()) {
            return this.getEdgeListIterator();
        } else {
            return this.getReaderIterator();
        }
    }

    private Iterator<Edge> getEdgeListIterator() {
        if (!maybeEdgeList.isPresent()) {
            throw new RuntimeException("Attempted to get edge list iterator without it present");
        }

        List<Edge> edgeList = maybeEdgeList.get();

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
        throw new UnsupportedOperationException("Reader iterator not implemented");
    }



}
