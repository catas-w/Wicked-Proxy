package com.catas.wicked.proxy.message;

import com.catas.wicked.common.bean.message.RequestMessage;
import com.catas.wicked.common.bean.RequestCell;
import com.catas.wicked.proxy.gui.controller.RequestViewController;
import com.catas.wicked.common.util.WebUtils;
import io.netty.handler.codec.http.HttpMethod;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.application.Platform;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Singleton
public class MessageTree {

    private final TreeNode root = new TreeNode();

    private TreeNode latestNode;

    @Inject
    private RequestViewController requestViewController;

    /**
     * 根据 path 添加节点到树中
     * @param msg request/response entity
     */
    public void add(RequestMessage msg) {
        // create leaf node
        TreeNode node = new TreeNode();
        node.setRequestId(msg.getRequestId());
        node.setMethod(new HttpMethod(msg.getMethod()));
        node.setUrl(msg.getRequestUrl());
        node.setFullPath(msg.getRequestUrl());
        node.setLeaf(true);

        // add node to its position
        List<String> pathSplits = WebUtils.getPathSplits(msg.getUrl().toString());
        node.setPath(pathSplits.get(pathSplits.size() - 1));
        pathSplits.remove(pathSplits.size() - 1);

        TreeNode parent = findAndCreatParentNode(root, pathSplits, 0);
        parent.getLeafChildren().add(node);

        // 创建 UI
        createTreeItemUI(parent, node);
        createListItemUI(node);

        if (latestNode == null) {
            latestNode = new TreeNode();
        }
        latestNode.setNext(node);
        node.setPrev(latestNode);
        latestNode = node;
    }

    /**
     * 创建 tree item
     * @param parent parent node
     * @param node tree node
     */
    private void createTreeItemUI(TreeNode parent, TreeNode node) {
        if (!node.isLeaf() && node.getTreeItem() != null) {
            return;
        }
        if (parent == root && parent.getTreeItem() == null) {
            parent.setTreeItem(requestViewController.getTreeRoot());
        }
        TreeItem<RequestCell> parentTreeItem = parent.getTreeItem();
        TreeItem<RequestCell> treeItem = new TreeItem<>();

        RequestCell requestCell = new RequestCell(node.getPath(),
                node.getMethod() == null ? "": node.getMethod().name());
        requestCell.setFullPath(node.getFullPath());
        requestCell.setLeaf(node.isLeaf());
        requestCell.setRequestId(node.getRequestId());
        treeItem.setValue(requestCell);
        node.setTreeItem(treeItem);

        // define tree item order
        int index;
        if (node.isLeaf()) {
            index = parent.getPathChildren().size() + parent.getLeafChildren().size() - 1;
        } else {
            index = parent.getPathChildren().size() - 1;
        }
        Platform.runLater(() -> {
           parentTreeItem.getChildren().add(index, treeItem);
        });
    }

    /**
     * 根据 path 查找并创建需要添加的父节点
     * @param parent parent node
     * @param index index
     */
    private TreeNode findAndCreatParentNode(TreeNode parent, List<String> path, int index) {
        if (index >= path.size()) {
            return parent;
        }
        String curPath = path.get(index);
        TreeNode node = parent.getPathChildren().get(curPath);
        if (node == null) {
            node = new TreeNode();
            node.setPath(curPath);
            node.setFullPath(parent == root ? curPath : parent.getFullPath() + '/' + curPath);
            parent.getPathChildren().put(curPath, node);
        }
        createTreeItemUI(parent, node);
        RequestCell cell = node.getTreeItem().getValue();
        cell.setCreatedTime(System.currentTimeMillis());
        return findAndCreatParentNode(node, path, ++index);
    }

    /**
     * 创建 list-item ui
     * @param node
     */
    private void createListItemUI(TreeNode node) {
        if (node == null || !node.isLeaf()) {
            return;
        }
        RequestCell requestCell = new RequestCell(node.getUrl(), node.getMethod() == null ? "" : node.getMethod().name());
        requestCell.setRequestId(node.getRequestId());
        requestCell.setFullPath(node.getFullPath());
        ListView<RequestCell> reqListView = requestViewController.getReqListView();
        Platform.runLater(() -> {
            reqListView.getItems().add(requestCell);
        });
    }

    /**
     * delete request by path and id
     * @param requestCell requestCell hold by tree-view or list-view
     */
    public void delete(RequestCell requestCell) {
        if (requestCell == null || StringUtils.isBlank(requestCell.getFullPath())) {
            return;
        }
        // find node to delete
        String requestId = requestCell.isLeaf() ? requestCell.getRequestId() : null;
        TreeNode nodeToDelete = findNodeByPath(requestCell.getFullPath(), requestId);
        System.out.println("Node to delete: " + nodeToDelete.getFullPath());

        ArrayList<String> list = new ArrayList<>();
        travel(nodeToDelete, treeNode -> list.add(treeNode.getRequestId()));
    }

    /**
     * travel tree from specified treeNode
     * @param node start point
     */
    public void travel(TreeNode node, Consumer<? super TreeNode> action) {
        if (node == null) {
            return;
        }


    }

    /**
     * find TreeNode by full path and request id
     * @param fullPath full path
     * @param requestId requestId
     */
    public TreeNode findNodeByPath(String fullPath, String requestId) {
        List<String> pathSplits = WebUtils.getPathSplits(fullPath, false);
        return findNodeByPath(root, requestId, pathSplits, 0);
    }

    private TreeNode findNodeByPath(TreeNode parent, String requestId, List<String> pathSplits, int index) {
        if (index >= pathSplits.size() || parent == null) {
            return parent;
        }
        if (requestId != null && index == pathSplits.size() - 1) {
            // last path, find in leavesChildren
            for (TreeNode leafChild : parent.getLeafChildren()) {
                if (StringUtils.equals(requestId, leafChild.getRequestId())) {
                    return leafChild;
                }
            }
            // not find
            return null;
        }

        // find in pathChildren
        String curPath = pathSplits.get(index);
        TreeNode node = parent.getPathChildren().getOrDefault(curPath, null);
        return findNodeByPath(node, requestId, pathSplits, ++index);
    }
}
