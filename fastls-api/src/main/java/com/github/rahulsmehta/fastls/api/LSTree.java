package com.github.rahulsmehta.fastls.api;

import com.google.common.collect.*;
import org.jgrapht.alg.util.UnionFind;
import org.jheaps.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class LSTree {

    private final Logger LOG = LoggerFactory.getLogger(LSTree.class);
    private final Integer numNodes;

    private UnionFind<Integer> uf;
    private TreeNode root;
    private Map<Integer, TreeNode> nodeMap;
    private boolean modifiedThisPhase;

    public LSTree(Set<Integer> nodes) {
        this.numNodes = nodes.size();
        this.uf = new UnionFind<>(nodes);
        this.nodeMap = Maps.newHashMap();

        this.root = new TreeNode(-1, null);
        this.nodeMap.put(-1, this.root);

        List<TreeNode> children = nodes.stream()
                .map(value -> new TreeNode(value, this.root))
                .collect(Collectors.toList());
        this.root.addChildren(children);
        for (TreeNode child : children) {
            this.nodeMap.put(child.getValue(), child);
        }
        this.modifiedThisPhase = false;
    }

    /**
     * Processes an edge according to the LS algorithm.
     *
     * @param graphEdge An edge in <code>G</code> corresponding to <code>(i,j)</code>.
     * @return Returns an optional <code>Edge</code>, depending on whether or not the previous update
     * resulted in a new edge being added to the next streaming phase.
     */
    public Optional<Edge> processEdge(Edge graphEdge) {
        Edge treeEdge = translateEdge(graphEdge);

        if (isInit(treeEdge) && !isSelfLoop(treeEdge)) {
            processInit(treeEdge);
            if (isBackward(treeEdge) && !isSelfLoop(treeEdge)) {
                processBackward(treeEdge);
            }
            return Optional.empty();
        } else if (isSelfLoop(treeEdge) || isForward(treeEdge)) {
            return Optional.empty();
        } else if (isBackward(treeEdge)) {
            processBackward(treeEdge);
            return Optional.empty();
        } else if (isCrossForward(treeEdge)) {
            return Optional.of(treeEdge);
        } else if (isCrossNonForward(treeEdge)) {
            return processCrossNonForward(treeEdge);
        } else {
            throw new IllegalStateException("Should never reach here");
        }
    }

    /**
     * Computes the strongly-connected components of <code>G</code>.
     *
     * @return Returns a list of sets, each containing the values of each node in the resepctive connected
     * components.
     */
    List<Set<Integer>> stronglyConnectedComponents() {
        return new ArrayList<>(getKeyedComponents().values());
    }

    /**
     * Returns the <code>TreeNode</code> associated with a particular node value.
     *
     * @param value The value of the node to return.
     * @return The value of the node, it exists
     * @throws throws an <code>IllegalStateException</code> if a caller attempts to
     *                access a non-existent node (since nodes are never explicitly deleted during execution
     *                this is an indicator of corrupted state).
     */
    private TreeNode getNode(int value) {
        if (this.nodeMap.containsKey(value)) {
            return this.nodeMap.get(value);
        } else {
            throw new IllegalStateException("Tried to access dead node");
        }
    }

    /**
     * Translates a graph edge <code>(i,j)</code> into a tree edge <code>(u,v)</code> using the union-find
     * structure.
     *
     * @param e <code>e=(i,j)</code>, a graph edge.
     * @return <code>e' = (u,v),</code> which is an edge that corresponds to the (possibly contracted) nodes
     * in the LS tree.
     */
    private Edge translateEdge(Edge e) {
        return new Edge(this.uf.find(e.i), this.uf.find(e.j));
    }

    @VisibleForTesting
    Map<Integer, Set<Integer>> getKeyedComponents() {
        Map<Integer, Set<Integer>> keyedComponents = Maps.newHashMap();
        for (int i = 0; i < uf.size(); i++) {
            int component = uf.find(i);
            Set<Integer> updatedComponent = keyedComponents.getOrDefault(component, Sets.newHashSet());
            updatedComponent.add(i);
            keyedComponents.put(component, updatedComponent);
        }
        return keyedComponents;
    }

    /**
     * Marks the streaming passs as having begun, so T can detect mutations and report the flag
     * to the caller.
     */
    void startPhase() {
        this.modifiedThisPhase = false;
    }

    /**
     * Checks if the internal tree T changed at all in the past streaming phase - if it did not, then the
     * algorithm has terminated.
     *
     * @return whether or not this is the last phase
     */
    boolean isComplete() {
        return !this.modifiedThisPhase;
    }

    /***
     * Returns the distance of the node <code>u</code> from the root of the tree.
     * The root is considered to be at depth 0.
     * @param   u   the node to calculate the depth.
     * @return the distance from the root to u; this will always be non-negative.
     */
    private int depth(TreeNode u) {
        return depthRecursive(u, 0);
    }

    private int depthRecursive(TreeNode u, int depth) {
        if (u.getParent() == null) {
            return depth;
        }
        return depthRecursive(u.getParent(), depth + 1);
    }

    /**
     * Computes if <code>u</code> is an ancestor of <code>v</code>.
     *
     * @param u the source node
     * @param v the target node
     * @return a boolean indicating whether or not <code>v</code> is a descendent
     * of <code>u</code>.
     */
    private boolean isAncestor(TreeNode u, TreeNode v) {
        if (v == null || v.getParent() == null) {
            return false;
        } else if (v.equals(u)) {
            return true;
        } else {
            return isAncestor(u, v.getParent());
        }
    }

    /**
     * Checks if an edge <code>e=(ukv)</code>, given the current state of the tree <code>T</code>,
     * satisfies the "initial" condition of the LS algorithm, namely that the <code>root</code>
     * is <code>v</code>'s parent.
     *
     * @param e an edge <code>e=(u,v)</code> in the graph <code>G</code>.
     * @return whether or not <code>e</code> satisfies the condition that <code>root</code> is
     * the parent of <code>v</code>.
     */
    private boolean isInit(Edge e) {
        TreeNode v_node = this.getNode(e.j);
        return v_node.getParent().equals(this.root);
    }

    /**
     * Updates the internal state of the LS tree T according to the update rule for the initial state.
     *
     * @param e the current edge to update.
     */
    private void processInit(Edge e) {
        TreeNode u_node = this.getNode(e.i);
        TreeNode v_node = this.getNode(e.j);

        // Remove (r,v) from T
        this.root.removeChild(v_node);

        // Add (u,v) to T
        u_node.addChild(v_node);
        v_node.setParent(u_node);
        this.modifiedThisPhase = true;
    }

    /**
     * Checks if an edge <code>e=(u,v)</code>, given the current state of the tree <code>T</code>,
     * satisfies the "backward" edge condition, namely that <code>v</code> is an ancestor of <code>u</code>.
     *
     * @param e an edge <code>e=(u,v)</code> in the graph <code>G</code>.
     * @return whether or not <code>e</code> satisfies the condition that <code>v</code> is an ancestor
     * of <code>u</code>.
     */
    private boolean isBackward(Edge e) {
        TreeNode u_node = this.getNode(e.i);
        TreeNode v_node = this.getNode(e.j);
        return isAncestor(v_node, u_node);
    }

    /**
     * Updates the internal state of the LS tree T according to the update rule for the back edge state.
     *
     * @param e the current edge to update.
     */
    private void processBackward(Edge e) {
        TreeNode u_node = this.getNode(e.i);
        TreeNode v_node = this.getNode(e.j);


        List<TreeNode> cycle = findCycle(u_node, v_node);
        if (cycle.size() < 2) {
            throw new IllegalStateException("Should have at least 2 vertices");
        }

        // Select a pivot node and contract the cycle around it
        TreeNode pivot = cycle.get(0);
        Set<TreeNode> newChildren = Sets.newHashSet();
        newChildren.addAll(pivot.getChildren());
        for (int i = 1; i < cycle.size(); i++) {
            TreeNode toContract = cycle.get(i);
            // Remove from nodeMap and store children
            this.nodeMap.remove(toContract.getValue());
            newChildren.addAll(toContract.getChildren());
            // Update the UF structure
            uf.union(pivot.getValue(), toContract.getValue());
        }
        newChildren = Sets.difference(newChildren, ImmutableSet.of(cycle));
        newChildren.stream().filter(node -> node != pivot).forEach(child -> child.setParent(pivot));
        pivot.setChildren(newChildren);
        if (cycle.contains(pivot.getParent())) {
            this.root.addChild(pivot);
            pivot.setParent(this.root);
        }
        // Re-label the new component
        int oldValue = pivot.getValue();
        nodeMap.remove(oldValue);

        int newValue = uf.find(oldValue);
        pivot.setValue(newValue);
        nodeMap.put(newValue, pivot);

        this.modifiedThisPhase = true;
    }

    /**
     * Utility method that finds all nodes lying on a cycle between <code>u</code> and <code>v</code>.
     *
     * @param u the source vertex for the cycle-forming edge
     * @param v the target vertex for the cytcle-forming edge
     * @return a list of all <code>TreeNode</code>s that are on the cycle.
     */
    private List<TreeNode> findCycle(TreeNode u, TreeNode v) {
        return findCycleRecursive(u, v, Lists.newLinkedList(ImmutableList.of(v)));
    }

    private List<TreeNode> findCycleRecursive(TreeNode u, TreeNode v, LinkedList<TreeNode> currentCycle) {
        if (u.equals(v)) {
            return currentCycle;
        }
        currentCycle.add(u);
        return findCycleRecursive(u.getParent(), v, currentCycle);
    }

    /**
     * Checks if an edge <code>e=(u,v)</code>, given the current state of the tree <code>T</code>,
     * satisfies the "cross-forward" edge condition, namely that <code>h(u) >= h(v)</code>.
     *
     * @param e an edge <code>e=(u,v)</code> in the graph <code>G</code>.
     * @return whether or not <code>e</code> is a cross-forward edge.
     */
    private boolean isCrossForward(Edge e) {
        TreeNode u_node = this.getNode(e.i);
        TreeNode v_node = this.getNode(e.j);
        return !isForward(e) && !isBackward(e) &&
                depth(u_node) - 1 <= depth(v_node);
    }

    /**
     * Checks if an edge <code>e=(u,v)</code>, given the current state of the tree <code>T</code>,
     * satisfies the "cross-non-forward" edge condition, namely that <code>h(u) < h(v)</code>.
     *
     * @param e an edge <code>e=(u,v)</code> in the graph <code>G</code>.
     * @return whether or not <code>e</code> is a cross-non-forward edge.
     */
    private boolean isCrossNonForward(Edge e) {
        TreeNode u_node = this.getNode(e.i);
        TreeNode v_node = this.getNode(e.j);
        return !isForward(e) && !isBackward(e) &&
                depth(u_node) > depth(v_node) - 1;
    }

    /**
     * Updates the internal state of the tree for a cross non-forward edge. This "depeens" the tree by discarding
     * the existing forward tree edge and re-rooting the sub-tree so that the cross-non-forward edge is now
     * contaiend in T.
     *
     * @param e the current edge to update.
     * @return an optional of type <code>Edge</code>, which indicates if there is another edge to be added
     * to the next stream.
     */
    private Optional<Edge> processCrossNonForward(Edge e) {
        TreeNode u_node = this.getNode(e.i);
        TreeNode v_node = this.getNode(e.j);

        // Remove (v.parent, v) from T
        TreeNode v_parent = v_node.getParent();
        v_parent.removeChild(v_node);

        // Add e=(u,v) to T
        u_node.addChild(v_node);
        v_node.setParent(u_node);

        // Add (v.parent, v) to new stream
        this.modifiedThisPhase = true;
        return Optional.of(new Edge(v_parent.getValue(), v_node.getValue()));
    }


    /**
     * Checks if an edge <code>e=(u,v)</code>, given the current state of the tree <code>T</code>,
     * satisfies the "forward" edge condition, namely that <code>u</code> is an ancestor of <code>v</code>.
     *
     * @param e an edge <code>e=(u,v)</code> in the graph <code>G</code>.
     * @return whether or not <code>e</code> satisfies the condition that <code>u</code> is an ancestor
     * of <code>v</code>.
     */
    private boolean isForward(Edge e) {
        TreeNode u_node = this.getNode(e.i);
        TreeNode v_node = this.getNode(e.j);
        return isAncestor(u_node, v_node);
    }

    /**
     * Checks if an edge <code>e=(u,v)</code> is a self-loop, i.e. that <code>u = v</code>.
     *
     * @param e an edge <code>e=(u,v)</code> in the graph <code>G</code>.
     * @return whether or not <code>e</code> is a self-loop.
     */
    private boolean isSelfLoop(Edge e) {
        return e.i.equals(e.j);
    }

    /*
    Testing & debugging utilities
     */
    private void printNodeMap() {
        nodeMap.entrySet().stream()
                .filter(entry -> entry.getKey() != -1)
                .forEach(e -> LOG.warn("{} -p-> {}", e.getKey(), e.getValue().getParent().getValue()));
    }

    public List<String> treeEdges() {
        Set<TreeNode> leafNodes = this.nodeMap.values().stream()
                .filter(node -> node.getChildren().size() == 0)
                .collect(Collectors.toSet());
        Map<Integer, List<String>> leafEdges = Maps.newHashMap();
        for (TreeNode node : leafNodes) {
            List<String> nodePath = treeEdges(node, Lists.newLinkedList());
            leafEdges.put(node.getValue(), nodePath);
        }

        return leafEdges.values().stream()
                .flatMap(path -> path.stream())
                .distinct().collect(Collectors.toList());

    }

    private List<String> treeEdges(TreeNode node, LinkedList<String> path) {
        if (node.getParent() == null) {
            return path;
        } else {
            path.add(String.format("(%d,%d)", node.getParent().getValue(), node.getValue()));
            return treeEdges(node.getParent(), path);
        }
    }

    public Integer height() {
        Collection<TreeNode> nodes = this.nodeMap.values();
        Integer maxHeight = -1;
        for (TreeNode n : nodes) {
            Integer nHeight = depth(n);
            maxHeight = nHeight > maxHeight ? nHeight : maxHeight;
        }
        return maxHeight;
    }

    private Integer height(TreeNode current, Integer h) {
        if (current.getChildren().size() == 0) {
            return h;
        } else {
            int maxHeight = -1;
            for (TreeNode child : current.getChildren()) {
                int subtreeHeight = height(child, h + 1);
                maxHeight = subtreeHeight > maxHeight ? subtreeHeight : maxHeight;
            }
            return maxHeight;
        }
    }

}
