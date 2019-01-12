package com.github.rahulsmehta.fastls.api;

import com.google.common.collect.*;
import org.jgrapht.alg.util.UnionFind;
import org.jheaps.annotations.VisibleForTesting;

import java.util.*;
import java.util.stream.Collectors;

public class LSTree {

    private UnionFind<Integer> uf;
    private TreeNode root;
    private Map<Integer, TreeNode> nodeMap;
    private boolean modifiedThisPhase;

    public LSTree(Set<Integer> nodes) {
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
        for (int i=0; i < uf.size(); i++) {
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
        return depthRecursive(u.getParent(), depth+1);
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
        if (cycle.size() < 2) {
            throw new IllegalStateException("Should have at least 2 vertices");
        }

        // Contract nodes along cycle

        TreeNode pivot = cycle.get(0);
        Set<TreeNode> newChildren = Sets.newHashSet();
        newChildren.addAll(pivot.getChildren());
        for (int i=1; i < cycle.size(); i++) {
            TreeNode toContract = cycle.get(i);
            // Remove from nodeMap and store children
            this.nodeMap.remove(toContract.getValue());
            newChildren.addAll(toContract.getChildren());
            // Update the UF structure
            uf.union(pivot.getValue(), toContract.getValue());
        }
        newChildren = Sets.difference(newChildren, ImmutableSet.of(cycle));
        newChildren.forEach(child -> child.setParent(pivot));
        pivot.setChildren(newChildren);
        // Re-label the new component
        int oldValue = pivot.getValue();
        nodeMap.remove(oldValue);

        int newValue = uf.find(oldValue);
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
                depth(u_node) < depth(v_node);
    }

    private boolean isCrossNonForward(Edge e) {
        TreeNode u_node = this.getNode(e.i);
        TreeNode v_node = this.getNode(e.j);
        return !isForward(e) && !isBackward(e) &&
                depth(u_node) >= depth(v_node);
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
        return e.i == e.j;
    }


    public Optional<Edge> processEdge(Edge graphEdge) {
        Edge treeEdge = translateEdge(graphEdge);

        if (isInit(treeEdge)) {
            processInit(treeEdge);
            if (isBackward(treeEdge)) {
                return processBackward(treeEdge);
            } else {
                return Optional.empty();
            }
        } else if (isSelfLoop(treeEdge) || isForward(treeEdge)) {
            return Optional.empty();
        } else if (isBackward(treeEdge)) {
            return processBackward(treeEdge);
        } else if (isCrossForward(treeEdge)) {
            return Optional.of(treeEdge);
        } else if (isCrossNonForward(treeEdge)) {
            return processCrossNonForward(treeEdge);
        } else {
            throw new IllegalStateException("Should never reach here");
        }
    }
}
