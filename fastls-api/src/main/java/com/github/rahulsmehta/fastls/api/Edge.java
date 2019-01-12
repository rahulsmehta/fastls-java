package com.github.rahulsmehta.fastls.api;

class Edge {
    final Integer i, j;

    Edge(int i, int j) {
        this.i = i;
        this.j = j;
    }

    @Override
    public String toString() {
        return String.format("%d,%d", this.i, this.j);
    }
}