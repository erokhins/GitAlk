package org.hanuna.gitalk.graphmodel.impl;

import org.hanuna.gitalk.common.Executor;
import org.hanuna.gitalk.common.Get;
import org.hanuna.gitalk.common.compressedlist.Replace;
import org.hanuna.gitalk.graph.Graph;
import org.hanuna.gitalk.graph.elements.Edge;
import org.hanuna.gitalk.graph.elements.Node;
import org.hanuna.gitalk.graph.mutable.GraphBuilder;
import org.hanuna.gitalk.graph.mutable.GraphDecorator;
import org.hanuna.gitalk.graph.mutable.MutableGraph;
import org.hanuna.gitalk.graphmodel.FragmentManager;
import org.hanuna.gitalk.graphmodel.GraphModel;
import org.hanuna.gitalk.graphmodel.fragment.FragmentManagerImpl;
import org.hanuna.gitalk.log.commit.CommitParents;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author erokhins
 */
public class GraphModelImpl implements GraphModel {
    private final MutableGraph graph;
    private final FragmentManagerImpl fragmentManager;
    private final BranchVisibleNodes visibleNodes;
    private final List<Executor<Replace>> listeners = new ArrayList<Executor<Replace>>();
    private final GraphBranchShowFixer branchShowFixer;

    private Get<Node, Boolean> isStartedBranchVisibilityNode = new Get<Node, Boolean>() {
        @NotNull
        @Override
        public Boolean get(@NotNull Node key) {
            return true;
        }
    };

    public GraphModelImpl(MutableGraph graph) {
        this.graph = graph;
        this.fragmentManager = new FragmentManagerImpl(graph, new FragmentManagerImpl.CallBackFunction() {
            @Override
            public Replace runIntermediateUpdate(@NotNull Node upNode, @NotNull Node downNode) {
                return GraphModelImpl.this.updateIntermediate(upNode, downNode);
            }

            @Override
            public void fullUpdate() {
                GraphModelImpl.this.fullUpdate();
            }
        });
        this.visibleNodes = new BranchVisibleNodes(graph);
        visibleNodes.setVisibleNodes(visibleNodes.generateVisibleBranchesNodes(isStartedBranchVisibilityNode));
        branchShowFixer = new GraphBranchShowFixer(graph, fragmentManager);
        graph.setGraphDecorator(new GraphDecorator() {
            private final GraphDecorator decorator = fragmentManager.getGraphDecorator();
            @Override
            public boolean isVisibleNode(@NotNull Node node) {
                return visibleNodes.isVisibleNode(node) && decorator.isVisibleNode(node);
            }

            @Override
            public Edge getHideFragmentDownEdge(@NotNull Node node) {
                return decorator.getHideFragmentDownEdge(node);
            }

            @Override
            public Edge getHideFragmentUpEdge(@NotNull Node node) {
                return decorator.getHideFragmentUpEdge(node);
            }
        });
        graph.updateVisibleRows();
    }

    @NotNull
    private Replace updateIntermediate(@NotNull Node upNode, @NotNull Node downNode) {
        int upRowIndex = upNode.getRowIndex();
        int downRowIndex = downNode.getRowIndex();
        graph.updateVisibleRows();

        Replace replace = Replace.buildFromToInterval(upRowIndex, downRowIndex, upNode.getRowIndex(), downNode.getRowIndex());
        callUpdateListener(replace);
        return replace;
    }

    private void fullUpdate() {
        int oldSize = graph.getNodeRows().size();
        graph.updateVisibleRows();
        Replace replace = Replace.buildFromToInterval(0, oldSize - 1, 0, graph.getNodeRows().size() - 1);
        callUpdateListener(replace);
    }

    @NotNull
    @Override
    public Graph getGraph() {
        return graph;
    }

    @Override
    public void appendCommitsToGraph(@NotNull List<CommitParents> commitParentses) {
        int oldSize = graph.getNodeRows().size();
        GraphBuilder.addCommitsToGraph(graph, commitParentses);
        visibleNodes.setVisibleNodes(visibleNodes.generateVisibleBranchesNodes(isStartedBranchVisibilityNode));
        graph.updateVisibleRows();

        Replace replace = Replace.buildFromToInterval(0, oldSize - 1, 0, graph.getNodeRows().size() - 1);
        callUpdateListener(replace);
    }

    @Override
    public void setVisibleBranchesNodes(@NotNull Get<Node, Boolean> isStartedNode) {
        this.isStartedBranchVisibilityNode = isStartedNode;
        Set<Node> prevVisibleNodes = visibleNodes.getVisibleNodes();
        Set<Node> newVisibleNodes = visibleNodes.generateVisibleBranchesNodes(isStartedNode);
        branchShowFixer.fixCrashBranches(prevVisibleNodes, newVisibleNodes);
        visibleNodes.setVisibleNodes(newVisibleNodes);
        fullUpdate();
    }

    @NotNull
    @Override
    public FragmentManager getFragmentManager() {
        return fragmentManager;
    }

    private void callUpdateListener(@NotNull Replace replace) {
        for (Executor<Replace> listener : listeners) {
            listener.execute(replace);
        }
    }

    @Override
    public void addUpdateListener(@NotNull Executor<Replace> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeAllListeners() {
        listeners.clear();
    }
}