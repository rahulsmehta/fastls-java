package com.github.rahulsmehta.fastls.api;

import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TreeNode {

    private int value;
    private TreeNode parent;
    private HashSet<TreeNode> children;


    public TreeNode(int value, TreeNode parent) {
        this.value = value;
        this.parent = parent;
        this.children = Sets.newHashSet();
    }

    public TreeNode(int value, TreeNode parent, Set<TreeNode> children) {
        this.value = value;
        this.parent = parent;
        this.children = Sets.newHashSet(children);
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public TreeNode getParent() {
        return parent;
    }

    public void setParent(TreeNode parent) {
        this.parent = parent;
    }

    public Set<TreeNode> getChildren() {
        return children;
    }

    public void setChildren(Set<TreeNode> children) {
        this.children = Sets.newHashSet(children);
    }

    public void addChild(TreeNode child) {
        this.children.add(child);
    }

    public void addChildren(Collection<TreeNode> children) {
        this.children.addAll(children);
    }

    public boolean removeChild(TreeNode child) {
        return this.children.remove(child);
    }

    public boolean removeChildren(Collection<TreeNode> children) {
        return this.children.removeAll(children);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TreeNode treeNode = (TreeNode) o;
        return value == treeNode.value;
//        return value == treeNode.value &&
//                parent.value == treeNode.parent.value &&
//                children.equals(treeNode.children);

    }
}
