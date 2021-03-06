package org.hanuna.gitalk.swing_ui.frame;

import org.hanuna.gitalk.graph.elements.GraphElement;
import org.hanuna.gitalk.graph.elements.Node;
import org.hanuna.gitalk.printmodel.GraphPrintCell;
import org.hanuna.gitalk.printmodel.SpecialPrintElement;
import org.hanuna.gitalk.swing_ui.render.GraphCommitCellRender;
import org.hanuna.gitalk.swing_ui.render.painters.GraphCellPainter;
import org.hanuna.gitalk.swing_ui.render.painters.SimpleGraphCellPainter;
import org.hanuna.gitalk.ui.UI_Controller;
import org.hanuna.gitalk.ui.tables.GraphCommitCell;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.BorderUIResource;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static org.hanuna.gitalk.swing_ui.render.Print_Parameters.HEIGHT_CELL;

/**
 * @author erokhins
 */
public class UI_GraphTable extends JTable {
    private final UI_Controller ui_controller;
    private final GraphCellPainter graphPainter = new SimpleGraphCellPainter();
    private final MouseAdapter mouseAdapter = new MyMouseAdapter();

    public UI_GraphTable(UI_Controller ui_controller) {
        super(ui_controller.getGraphTableModel());
        UIManager.put("Table.focusCellHighlightBorder", new BorderUIResource(
                new LineBorder(new Color(255,0,0, 0))));
        this.ui_controller = ui_controller;
        prepare();
    }

    private void prepare() {
        setDefaultRenderer(GraphCommitCell.class, new GraphCommitCellRender(graphPainter));
        setRowHeight(HEIGHT_CELL);
        setShowHorizontalLines(false);
        setIntercellSpacing(new Dimension(0, 0));

        getColumnModel().getColumn(0).setPreferredWidth(700);
        getColumnModel().getColumn(1).setMinWidth(90);
        getColumnModel().getColumn(2).setMinWidth(90);

        addMouseMotionListener(mouseAdapter);
        addMouseListener(mouseAdapter);
    }

    public void jumpToRow(int rowIndex) {
        scrollRectToVisible(getCellRect(rowIndex, 0, false));
        setRowSelectionInterval(rowIndex, rowIndex);
        scrollRectToVisible(getCellRect(rowIndex, 0, false));
    }

    private class MyMouseAdapter extends MouseAdapter {
        private final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);
        private final Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);

        @Nullable
        private GraphElement overCell(MouseEvent e) {
            int rowIndex = e.getY() / HEIGHT_CELL;
            int y = e.getY() - rowIndex * HEIGHT_CELL;
            int x = e.getX();
            GraphCommitCell commitCell = (GraphCommitCell) UI_GraphTable.this.getModel().getValueAt(rowIndex, 0);
            GraphPrintCell row = commitCell.getPrintCell();
            return graphPainter.mouseOver(row, x, y);
        }

        @Nullable
        private Node arrowToNode(MouseEvent e) {
            int rowIndex = e.getY() / HEIGHT_CELL;
            int y = e.getY() - rowIndex * HEIGHT_CELL;
            int x = e.getX();
            GraphCommitCell commitCell = (GraphCommitCell) UI_GraphTable.this.getModel().getValueAt(rowIndex, 0);
            GraphPrintCell row = commitCell.getPrintCell();
            SpecialPrintElement printElement = graphPainter.mouseOverArrow(row, x, y);
            if (printElement != null) {
                if (printElement.getType() == SpecialPrintElement.Type.DOWN_ARROW) {
                    return printElement.getGraphElement().getEdge().getDownNode();
                } else {
                    return printElement.getGraphElement().getEdge().getUpNode();
                }
            }
            return null;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 1) {
                Node jumpToNode = arrowToNode(e);
                if (jumpToNode != null) {
                    jumpToRow(jumpToNode.getRowIndex());
                }
                ui_controller.click(overCell(e));
            } else {
                int rowIndex = e.getY() / HEIGHT_CELL;
                ui_controller.doubleClick(rowIndex);
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            Node jumpToNode = arrowToNode(e);
            if (jumpToNode != null) {
                setCursor(HAND_CURSOR);
            } else {
                setCursor(DEFAULT_CURSOR);
            }
            ui_controller.over(overCell(e));
        }


    }

}
