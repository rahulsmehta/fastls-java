package com.github.rahulsmehta.fastls.api;

import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TreeNode {

    private Integer value;
    private TreeNode parent;
    private HashSet<TreeNode> children;


    TreeNode(int value, TreeNode parent) {
        this.value = value;
        this.parent = parent;
        this.children = Sets.newHashSet();
    }


    int getValue() {
        return value;
    }

    void setValue(int value) {
        this.value = value;
    }

    TreeNode getParent() {
        return parent;
    }

    void setParent(TreeNode parent) {
        this.parent = parent;
    }

    Set<TreeNode> getChildren() {
        return children;
    }

    void setChildren(Set<TreeNode> children) {
        this.children = Sets.newHashSet(children);
    }

    void addChild(TreeNode child) {
        this.children.add(child);
    }

    void addChildren(Collection<TreeNode> children) {
        this.children.addAll(children);
    }

    boolean removeChild(TreeNode child) {
        return this.children.remove(child);
    }

    boolean removeChildren(Collection<TreeNode> children) {
        return this.children.removeAll(children);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TreeNode treeNode = (TreeNode) o;
        return value.equals(treeNode.value);
    }
}
