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

    private TreeNode getNode(int value) {
        if (this.nodeMap.containsKey(value)) {
            return this.nodeMap.get(value);
        } else {
            throw new IllegalStateException("Tried to access dead node");
        }
    }

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

    List<Set<Integer>> stronglyConnectedComponents() {
        return new ArrayList<>(getKeyedComponents().values());
    }

    void startPhase() {
        this.modifiedThisPhase = false;
    }

    boolean isComplete() {
        return !this.modifiedThisPhase;
    }

    // ===============================================================

    /*
    Returns the distance from u to the root
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

    /*
    Returns if u is an ancestor of v (i.e. v is a descendant of u)
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

    // ===============================================================

    private boolean isInit(Edge e) {
        TreeNode v_node = this.getNode(e.j);
        return v_node.getParent().equals(this.root);
    }

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

    private boolean isBackward(Edge e) {
        TreeNode u_node = this.getNode(e.i);
        TreeNode v_node = this.getNode(e.j);
        return isAncestor(v_node, u_node);
    }

    private Optional<Edge> processBackward(Edge e) {
        TreeNode u_node = this.getNode(e.i);
        TreeNode v_node = this.getNode(e.j);


        List<TreeNode> cycle = findCycle(u_node, v_node);
        List<Integer> values = cycle.stream().map(TreeNode::getValue).collect(Collectors.toList());
        if (cycle.size() < 2) {
//            LOG.warn("current edge: {}", e);
//            LOG.warn("cycle: {}, {}", cycle.get(0).getValue(), cycle.get(0).getParent().getValue());
            throw new IllegalStateException("Should have at least 2 vertices");
        }

        // Contract nodes along cycle

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
//        LOG.warn("pivot : {}", pivot.getValue());
//        LOG.warn("pivot parent: {}", pivot.getParent().getValue());
        pivot.setChildren(newChildren);
        if (cycle.contains(pivot.getParent())) {
            this.root.addChild(pivot);
            pivot.setParent(this.root);
        }
        // Re-label the new component
        int oldValue = pivot.getValue();
        nodeMap.remove(oldValue);

        int newValue = uf.find(oldValue);
//        LOG.warn("relabeled to: {}", pivot.getValue());
//        LOG.warn("new pivot parent: {}", pivot.getParent().getValue());
        pivot.setValue(newValue);
        nodeMap.put(newValue, pivot);

        this.modifiedThisPhase = true;
        return Optional.empty();
    }

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

    private boolean isCrossForward(Edge e) {
        TreeNode u_node = this.getNode(e.i);
        TreeNode v_node = this.getNode(e.j);
        return !isForward(e) && !isBackward(e) &&
                depth(u_node) - 1 <= depth(v_node);
    }

    private boolean isCrossNonForward(Edge e) {
        TreeNode u_node = this.getNode(e.i);
        TreeNode v_node = this.getNode(e.j);
        return !isForward(e) && !isBackward(e) &&
                depth(u_node) > depth(v_node) - 1;
    }

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

    private boolean isForward(Edge e) {
        TreeNode u_node = this.getNode(e.i);
        TreeNode v_node = this.getNode(e.j);
        return isAncestor(u_node, v_node);
    }

    private boolean isSelfLoop(Edge e) {
        return e.i.equals(e.j);
    }

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
//            return current.getChildren().stream()
//                    .map(child -> height(child, h + 1))
//                    .max(Integer::compareTo)
//                    .orElseThrow(() -> new IllegalStateException("Malformed LS tree"));

            Integer maxHeight = -1;
            for (TreeNode child : current.getChildren()) {
                Integer subtreeHeight = height(child, h+1);
                maxHeight = subtreeHeight > maxHeight ? subtreeHeight : maxHeight;
            }
            return maxHeight;
        }
    }


    public Optional<Edge> processEdge(Edge graphEdge) {
        Edge treeEdge = translateEdge(graphEdge);
//        Object[] args = {graphEdge.i, graphEdge.j, treeEdge.i, treeEdge.j};
//        LOG.warn("{},{} -> {},{}", args);
//        LOG.warn("(n,h_T): {},{}", this.numNodes, this.height());

        if (isInit(treeEdge) && !isSelfLoop(treeEdge)) {
//            LOG.warn("Initial case");
            processInit(treeEdge);
//            LOG.warn("{}", nodeMap.get(treeEdge.j).getParent().getValue());
            if (isBackward(treeEdge) && !isSelfLoop(treeEdge)) {
//                LOG.warn("Backward");
                return processBackward(treeEdge);
            } else {
                return Optional.empty();
            }
        } else if (isSelfLoop(treeEdge) || isForward(treeEdge)) {
//            LOG.warn("Self-loop or forward");
            return Optional.empty();
        } else if (isBackward(treeEdge)) {
//            LOG.warn("Backward");
            return processBackward(treeEdge);
        } else if (isCrossForward(treeEdge)) {
//            LOG.warn("Cross-forward");
            return Optional.of(treeEdge);
        } else if (isCrossNonForward(treeEdge)) {
//            LOG.warn("Cross-non-forward");
            return processCrossNonForward(treeEdge);
        } else {
            throw new IllegalStateException("Should never reach here");
        }
    }
}
