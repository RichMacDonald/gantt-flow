package org.vaadin.tltv.gantt;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.vaadin.tltv.gantt.model.Step;
import org.vaadin.tltv.gantt.model.SubStep;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.treegrid.CollapseEvent;
import com.vaadin.flow.component.treegrid.ExpandEvent;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider;
import com.vaadin.flow.function.ValueProvider;

/**
 * Only uses Steps, not SubSteps
 * Uses a TreeGrid
 */
public class GanttTree extends Gantt {

	private static final long serialVersionUID = -2929183300181012531L;

	private final HierarchicalDataProvider<Step, ?> dataProvider;
	private Consumer<Step> onAddStepConsumer;

	public GanttTree(HierarchicalDataProvider<Step, ?> dataProvider) {
		this.dataProvider = dataProvider;
	}

	public void onAddStep(Consumer<Step> onAddStepConsumer) {
		this.onAddStepConsumer = onAddStepConsumer;
	}

	@Override
	public Grid<Step> buildCaptionGrid(String header) {
		removeCaptionGrid();
		TreeGrid grid = new TreeGrid<Step>();
		this.captionGrid = grid;
		grid.getStyle().set("--gantt-caption-grid-row-height", "30px");
		grid.addClassName("gantt-caption-grid");

		grid.setDataProvider(dataProvider);

		// grid.addColumn(LitRenderer.<Step>of("<span>${item.caption}</span>").withProperty("caption", Step::getCaption))
		// .setHeader(header).setResizable(true);
		ValueProvider<Step, String> valueProv = Step::getCaption;
		Column mainCol = grid.addHierarchyColumn(valueProv).setHeader(header);
		mainCol.setAutoWidth(true);
		mainCol.setResizable(true);

		captionGridColumnResizeListener = grid.addColumnResizeListener(event -> {
			if (event.isFromClient()) {
				refreshForHorizontalScrollbar();
			}
		});
		// grid.setItems(dataProvider);
		captionGridDataChangeListener = addDataChangeListener(event -> {
			// grid.getLazyDataView().refreshAll();
			refreshForHorizontalScrollbar();
		});
		getElement().executeJs("this.registerScrollElement($0.$.table)", grid);
		refreshForHorizontalScrollbar();

		// TODO: Dec 19, 2023 RJM turn these into method refs. Having trouble with generics?
		grid.addCollapseListener(new ComponentEventListener<CollapseEvent<Step, TreeGrid<Step>>>() {

			private static final long serialVersionUID = 1L;

			@Override
			public void onComponentEvent(CollapseEvent<Step, TreeGrid<Step>> event) {
				onCollapsed(event.getItems().iterator().next());
			}
		});

		grid.addExpandListener(new ComponentEventListener<ExpandEvent<Step, TreeGrid<Step>>>() {

			private static final long serialVersionUID = 1L;

			@Override
			public void onComponentEvent(ExpandEvent<Step, TreeGrid<Step>> event) {
				onExpanded(event.getItems().iterator().next());
			}
		});

		return grid;
	}

	void onCollapsed(Step collapsedParent) {
		removeSteps(collapsedParent.getDescendants().stream());
		if (captionGrid != null) {
			captionGrid.recalculateColumnWidths();
		}
	}

	void onExpanded(Step expandedParent) {
		int parentIndex = indexOf(expandedParent);
		for (Step child : expandedParent.getChildren()) {
			addStep(++parentIndex, child);
		}
		if (captionGrid != null) {
			captionGrid.recalculateColumnWidths();
		}
	}

	@Override
	public void addSubStep(SubStep subStep) {
		throw new IllegalArgumentException("Not supporting SubSteps");
	}

	@Override
	public void addStep(int index, Step step) {
		super.addStep(index, step);
		addedStep(step);
	}

	@Override
	public void addStep(Step step) {
		super.addStep(step);
		addedStep(step);
	}

	@Override
	public void addSteps(Step... steps) {
		super.addSteps(steps);
		for (Step step : steps) {
			addedStep(step);
		}
	}
	@Override
	public void addSteps(Collection<Step> steps) {
		super.addSteps(steps);
		for (Step step : steps) {
			addedStep(step);
		}
	}

	@Override
	public void addSteps(Stream<Step> steps) {
		if (onAddStepConsumer != null) {
			throw new RuntimeException("Cant support streams with a listener");
		}
		super.addSteps(steps);
	}

	void addedStep(Step step) {
		if (onAddStepConsumer != null) {
			onAddStepConsumer.accept(step);
		}
	}

}
