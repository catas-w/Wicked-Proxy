package com.catas.wicked.proxy.gui.controller;

import com.catas.wicked.common.bean.HeaderEntry;
import com.catas.wicked.common.bean.PairEntry;
import com.catas.wicked.common.constant.CodeStyle;
import com.catas.wicked.common.util.TableUtils;
import com.catas.wicked.proxy.gui.componet.MessageLabel;
import com.catas.wicked.proxy.gui.componet.SelectableNodeBuilder;
import com.catas.wicked.proxy.gui.componet.SideBar;
import com.catas.wicked.proxy.gui.componet.ZoomImageView;
import com.catas.wicked.proxy.gui.componet.highlight.CodeStyleLabel;
import com.catas.wicked.proxy.gui.componet.richtext.DisplayCodeArea;
import com.catas.wicked.proxy.render.ContextMenuFactory;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXTreeTableColumn;
import com.jfoenix.controls.JFXTreeTableView;
import com.jfoenix.controls.RecursiveTreeItem;
import com.jfoenix.controls.cells.editors.TextFieldEditorBuilder;
import com.jfoenix.controls.cells.editors.base.GenericEditableTreeTableCell;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import jakarta.inject.Singleton;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Labeled;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.skin.TableHeaderRow;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

@Slf4j
@Getter
@Singleton
public class DetailTabController implements Initializable {

    @FXML
    public JFXTabPane mainTabPane;
    @FXML
    public DisplayCodeArea testCodeArea;
    @FXML
    public TableView<HeaderEntry> reqContentTable;
    @FXML
    public ZoomImageView reqImageView;
    @FXML
    public MessageLabel respHeaderMsgLabel;
    @FXML
    public MessageLabel reqHeaderMsgLabel;
    @FXML
    public MessageLabel reqContentMsgLabel;
    @FXML
    public MessageLabel respContentMsgLabel;
    @FXML
    public MessageLabel timingMsgLabel;
    @FXML
    public JFXComboBox<Labeled> reqComboBox;
    @FXML
    public SideBar respSideBar;
    @FXML
    public SideBar reqContentSideBar;
    @FXML
    public SideBar reqQuerySideBar;
    @FXML
    public TreeTableView<PairEntry> timingTableView;
    @FXML
    private JFXComboBox<Labeled> respComboBox;
    @FXML
    private ZoomImageView respImageView;
    @FXML
    private MessageLabel overViewMsgLabel;
    @FXML
    private SplitPane respSplitPane;
    @FXML
    private SplitPane reqSplitPane;
    @FXML
    private DisplayCodeArea overviewArea;
    @FXML
    private TableView<HeaderEntry> overviewTable;
    @FXML
    private JFXTreeTableView<PairEntry> testTable;
    @FXML
    private TitledPane reqPayloadTitlePane;
    @FXML
    private JFXTabPane reqPayloadTabPane;
    @FXML
    private TitledPane respHeaderPane;
    @FXML
    private TitledPane reqParamPane;
    @FXML
    private TitledPane respDataPane;
    @FXML
    private TitledPane reqHeaderPane;
    @FXML
    private TableView<HeaderEntry> reqHeaderTable;
    @FXML
    private DisplayCodeArea reqHeaderArea;
    @FXML
    private DisplayCodeArea reqParamArea;
    @FXML
    private DisplayCodeArea reqPayloadCodeArea;
    @FXML
    private JFXTextArea reqTimingArea;
    @FXML
    private DisplayCodeArea respHeaderArea;
    @FXML
    private DisplayCodeArea respContentArea;
    @FXML
    private TableView<HeaderEntry> respHeaderTable;

    private final Map<SplitPane, double[]> dividerPositionMap =new HashMap<>();

    private boolean dividerUpdating;

    private boolean midTitleCollapse;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dividerPositionMap.put(reqSplitPane, reqSplitPane.getDividerPositions().clone());
        dividerPositionMap.put(respSplitPane, respSplitPane.getDividerPositions().clone());

        // init titlePane collapse
        addTitleListener(reqHeaderPane, reqSplitPane);
        addTitleListener(reqPayloadTitlePane, reqSplitPane);
        addTitleListener(respHeaderPane, respSplitPane);
        addTitleListener(respDataPane, respSplitPane);

        // init sideBar
        reqQuerySideBar.setStrategy(SideBar.Strategy.URLENCODED_FORM_DATA);
        reqQuerySideBar.setTargetCodeArea(reqParamArea);
        reqContentSideBar.setTargetCodeArea(reqPayloadCodeArea);
        respSideBar.setTargetCodeArea(respContentArea);
        initComboBox(respComboBox, respSideBar);
        initComboBox(reqComboBox, reqContentSideBar);

        // init tableView
        initTableView(overviewTable);
        initTableView(reqHeaderTable);
        initTableView(respHeaderTable);

        initTreeTable(testTable);
        initTreeTable(timingTableView);
    }

    private void initTreeTable(TreeTableView<PairEntry> tableView) {
        TreeTableColumn<PairEntry, String> nameColumn = new TreeTableColumn<>("Name..");
        nameColumn.setPrefWidth(120);
        nameColumn.setMaxWidth(120);
        nameColumn.setMinWidth(100);
        nameColumn.setSortable(false);
        nameColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<PairEntry, String> param) ->
                new ReadOnlyStringWrapper(param.getValue().getValue().getKey()));

        TreeTableColumn<PairEntry, String> valueColumn = new TreeTableColumn<>("Value");
        valueColumn.setSortable(false );
        valueColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<PairEntry, String> param) ->
                new ReadOnlyStringWrapper(param.getValue().getValue().getVal()));
        valueColumn.setCellFactory((TreeTableColumn<PairEntry, String> param) ->
                new GenericEditableTreeTableCell<>(new SelectableNodeBuilder()));

        TreeItem<PairEntry> root = new TreeItem<>();
        TreeItem<PairEntry> reqNode = new TreeItem<>(new PairEntry("Request", null));
        TreeItem<PairEntry> sizeNode = new TreeItem<>(new PairEntry("Size", ""));
        TreeItem<PairEntry> timingNode = new TreeItem<>(new PairEntry("Timing", ""));

        reqNode.getChildren().add(new TreeItem<>(new PairEntry("Url", "https://docs.orcale.com")));
        reqNode.getChildren().add(new TreeItem<>(new PairEntry("Name", "IntelliJ IDEA")));
        reqNode.getChildren().add(new TreeItem<>(new PairEntry("Url", "https://chat.openai.com/c/805fef18-1a46-47fa-b739-152c9ddebf3d")));
        reqNode.getChildren().add(new TreeItem<>(new PairEntry("Method", "POST")));
        sizeNode.getChildren().add(new TreeItem<>(new PairEntry("Request size", "102 Kb")));
        sizeNode.getChildren().add(new TreeItem<>(new PairEntry("Response size", "10,223 Kb")));
        timingNode.getChildren().add(new TreeItem<>(new PairEntry("Time", "2024-01-01 01:01:02")));

        root.setExpanded(true);
        reqNode.setExpanded(true);
        sizeNode.setExpanded(true);
        timingNode.setExpanded(true);
        root.getChildren().add(reqNode);
        root.getChildren().add(sizeNode);
        root.getChildren().add(timingNode);

        tableView.setRoot(root);
        tableView.setShowRoot(false);
        tableView.setEditable(true);
        tableView.getColumns().addAll(nameColumn, valueColumn);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        tableView.widthProperty().addListener((source, oldWidth, newWidth) -> {
            TableHeaderRow header = (TableHeaderRow) tableView.lookup("TableHeaderRow");
            header.reorderingProperty().addListener((observable, oldValue, newValue) -> header.setReordering(false));
        });
        tableView.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                tableView.getSelectionModel().clearSelection();
            }
        });
    }

    /**
     * initialize treeTableView
     * @param tableView treeTable
     */
    private void initTreeTable(JFXTreeTableView<PairEntry> tableView) {
        JFXTreeTableColumn<PairEntry, String> nameColumn = new JFXTreeTableColumn<>("Name");
        nameColumn.setPrefWidth(120);
        // nameColumn.setMaxWidth(200);
        nameColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<PairEntry, String> param) -> {
            if (nameColumn.validateValue(param)) {
                return param.getValue().getValue().keyProperty();
            } else {
                return nameColumn.getComputedValue(param);
            }
        });

        JFXTreeTableColumn<PairEntry, String> valueColumn = new JFXTreeTableColumn<>("Value");
        valueColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<PairEntry, String> param) -> {
            if (valueColumn.validateValue(param)) {
                return param.getValue().getValue().valProperty();
            } else {
                return valueColumn.getComputedValue(param);
            }
        });
        valueColumn.setCellFactory((TreeTableColumn<PairEntry, String> param) -> new GenericEditableTreeTableCell<>(
                new TextFieldEditorBuilder()));
        // valueColumn.setOnEditCommit((TreeTableColumn.CellEditEvent<PairEntry, String> t) ->
        //         t.getTreeTableView().getTreeItem(t.getTreeTablePosition()
        //                 .getRow())
        //                 .getValue().valueProperty().set(t.getNewValue()));


        ObservableList<PairEntry> data = FXCollections.observableArrayList();
        data.add(new PairEntry("Name", "IntelliJ IDEA"));
        data.add(new PairEntry("Url", "https://chat.openai.com/c/805fef18-1a46-47fa-b739-152c9ddebf3d"));
        data.add(new PairEntry("Method", "POST"));
        data.add(new PairEntry("Size", "123456KB"));
        data.add(new PairEntry("Time", "2024-01-01 01:01:02"));

        TreeItem<PairEntry> root = new RecursiveTreeItem<>(data, RecursiveTreeObject::getChildren);

        tableView.setRoot(root);
        tableView.setShowRoot(false);
        tableView.setEditable(true);
        tableView.getColumns().addAll(nameColumn, valueColumn);

        JFXTextField textField = new JFXTextField();

        // tableView.group(nameColumn, valueColumn);
        // tableView.group(valueColumn);
    }

    /**
     * initialize tableView for headers
     */
    private void initTableView(TableView<HeaderEntry> tableView) {
        // set key column
        TableColumn<HeaderEntry, String> keyColumn = new TableColumn<>();
        keyColumn.setText("Name");
        keyColumn.getStyleClass().add("table-key");
        keyColumn.setSortable(false);
        keyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
        keyColumn.setPrefWidth(120);
        keyColumn.setMaxWidth(200);
        TableUtils.setTableCellFactory(keyColumn, true);

        // set value column
        TableColumn<HeaderEntry, String> valColumn = new TableColumn<>();
        valColumn.setText("Value");
        valColumn.getStyleClass().add("table-value");
        valColumn.setSortable(false);
        valColumn.setEditable(true);
        valColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        // valColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        TableUtils.setTableCellFactory(valColumn, false);
        tableView.getColumns().setAll(keyColumn, valColumn);

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        // tableView.setFixedCellSize(20);
        tableView.prefHeightProperty()
                .bind(Bindings.size(tableView.getItems()).multiply(tableView.getFixedCellSize()));

        // selection
        // tableView.getSelectionModel().setCellSelectionEnabled(true);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        // tableView.getSelectionModel().clearAndSelect(0);

        tableView.setContextMenu(ContextMenuFactory.getTableViewContextMenu(tableView));

        // clearSelection when lose focus
        tableView.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                tableView.getSelectionModel().clearSelection();
            }
        });
        tableView.widthProperty().addListener((source, oldWidth, newWidth) -> {
            TableHeaderRow header = (TableHeaderRow) tableView.lookup("TableHeaderRow");
            header.reorderingProperty().addListener((observable, oldValue, newValue) -> header.setReordering(false));
        });

        TableUtils.installCopyPasteHandler(tableView);
    }

    private void initComboBox(ComboBox<Labeled> comboBox, SideBar sideBar) {
        if (comboBox.getItems().isEmpty()) {
            // comboBox.setButtonCell(new Gra);
            comboBox.getItems().add(new CodeStyleLabel("Plain", CodeStyle.PLAIN));
            comboBox.getItems().add(new CodeStyleLabel("Json", CodeStyle.JSON));
            comboBox.getItems().add(new CodeStyleLabel("Html", CodeStyle.HTML));
            comboBox.getItems().add(new CodeStyleLabel("Xml", CodeStyle.XML));
            comboBox.getItems().add(new CodeStyleLabel("Javascript", CodeStyle.JAVASCRIPT));

            comboBox.valueProperty().addListener(new ChangeListener<Labeled>() {
                @Override
                public void changed(ObservableValue<? extends Labeled> observable, Labeled oldValue, Labeled newValue) {
                    CodeStyle codeStyle = CodeStyle.valueOf(newValue.getText().toUpperCase());
                    // codeArea.setCodeStyle(codeStyle);
                    sideBar.setCodeStyle(codeStyle, false, true);
                }
            });
        }
        comboBox.getSelectionModel().selectFirst();
    }

    /**
     * synchronized dividers
     * @deprecated
     */
    private void bindDividerPosition(SplitPane splitPane) {
        if (splitPane.getDividers().size() < 2) {
            return;
        }
        ObservableList<SplitPane.Divider> dividers = splitPane.getDividers();
        dividers.get(0).positionProperty().addListener(((observable, oldValue, newValue) -> {
            if (dividerUpdating || splitPane.getDividers().size() < 2 || reqParamPane.isExpanded()) {
                return;
            }
            // System.out.println("Divider-0: " + newValue);
            if (newValue.doubleValue() > 0.95) {
                dividers.get(0).setPosition(0.95);
                dividers.get(1).setPosition(1.0);
                return;
            }
            dividerUpdating = true;
            double delta = newValue.doubleValue() - oldValue.doubleValue();
            dividers.get(1).setPosition(dividers.get(1).positionProperty().doubleValue() + delta);
            dividerUpdating = false;
        }));

        dividers.get(1).positionProperty().addListener(((observable, oldValue, newValue) -> {
            if (dividerUpdating || splitPane.getDividers().size() < 2 || reqParamPane.isExpanded()) {
                return;
            }
            // System.out.println("Divider-1: " + newValue);
            if (!midTitleCollapse) {
                return;
            }
            if (newValue.doubleValue() < 0.05) {
                dividers.get(0).setPosition(0.0);
                dividers.get(1).setPosition(0.05);
                return;
            }
            dividerUpdating = true;
            double delta = newValue.doubleValue() - oldValue.doubleValue();
            dividers.get(0).setPosition(dividers.get(0).positionProperty().doubleValue() + delta);
            dividerUpdating = false;
        }));
    }

    private void addTitleListener(TitledPane pane, SplitPane splitPane) {
        pane.expandedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                // open
                pane.maxHeightProperty().set(Double.POSITIVE_INFINITY);
                if (splitPane.getItems().size() == 2) {
                    splitPane.setDividerPositions(dividerPositionMap.get(splitPane));
                } else if (splitPane.getItems().size() == 3) {
                    // TODO bug: titledPane-1 expanded, titledPane-2,3 closed, tiledPane-3 cannot expand
                    int expandedNum = getExpandedNum(splitPane);
                    System.out.println("Expanded num: " + expandedNum);
                    if (expandedNum != 2) {
                        midTitleCollapse = false;
                        splitPane.setDividerPositions(0.33333, 0.66666);
                        return;
                    }
                    if (!reqParamPane.isExpanded()) {
                        ObservableList<SplitPane.Divider> dividers = splitPane.getDividers();
                        dividers.get(0).setPosition(0.5);
                        dividers.get(1).setPosition(0.5);
                    }
                }
            } else {
                // close
                if (getExpandedNum(splitPane) > 1) {
                    dividerPositionMap.put(splitPane, splitPane.getDividerPositions().clone());
                }
                pane.maxHeightProperty().set(Double.NEGATIVE_INFINITY);
                if (splitPane.getItems().size() == 3) {
                    if (getExpandedNum(splitPane) == 2 && !reqParamPane.isExpanded()) {
                        midTitleCollapse = true;
                    }
                }
            }
        });
    }
    private int getExpandedNum(SplitPane splitPane) {
        int res = 0;
        for (Node item : splitPane.getItems()) {
            if (item instanceof TitledPane titledPane) {
                res += titledPane.isExpanded() ? 1 : 0;
            }
        }
        return res;
    }

    public String getActiveRequestTab() {
        Tab selectedTab = this.mainTabPane.getSelectionModel().getSelectedItem();
        return selectedTab.getText();
    }
}
