package org.vaadin.tltv.gantt;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.vaadin.tltv.gantt.element.StepElement;
import org.vaadin.tltv.gantt.event.GanttClickEvent;
import org.vaadin.tltv.gantt.event.StepClickEvent;
import org.vaadin.tltv.gantt.event.StepMoveEvent;
import org.vaadin.tltv.gantt.event.StepResizeEvent;
import org.vaadin.tltv.gantt.model.Resolution;
import org.vaadin.tltv.gantt.model.Step;
import org.vaadin.tltv.gantt.model.SubStep;
import org.vaadin.tltv.gantt.util.GanttUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * GanttDemoView modified to only use Steps (no SubSteps).
 * Steps can now have child Steps.
 * The Grid is now a TreeGrid so we can show/hide/children.
 */

@PageTitle("Gantt")
@Route("")
public class GanttTreeDemoView extends VerticalLayout {

	private static final long serialVersionUID = 7824367106648133344L;

	private final GanttTree gantt;
	private final FlexLayout scrollWrapper;
	private final DatePicker startDateField;
	private final DatePicker endDateField;
	private final TimePicker startTimeField;
	private final TimePicker endTimeField;

	private SizeOption size = SizeOption.FULL_WIDTH;
	private int clickedBackgroundIndex;
	private LocalDateTime clickedBackgroundDate;
	private int stepCounter;

	private final static int SUBSET_COUNT = 3;

	public GanttTreeDemoView() {
		setWidthFull();
		setPadding(false);

		startDateField = new DatePicker();
		endDateField = new DatePicker();
		startTimeField = new TimePicker();
		endTimeField = new TimePicker();

		AbstractBackEndHierarchicalDataProvider<Step, Void> dataProvider = new AbstractBackEndHierarchicalDataProvider<>() {

			private static final long serialVersionUID = 1L;

			@Override
			public int getChildCount(HierarchicalQuery<Step, Void> query) {
				return getChildCount(query.getParent());
			}

			private int getChildCount(Step item) {
				if (item == null) {
					// this is the top of the tree, so answer the number of roots
					return getRootSteps().size();
				}
				return item.getChildren().size();
			}

			@Override
			public boolean hasChildren(Step item) {
				if (item == null) {
					// this is the top of the tree, so answer the number of roots
					return !getRootSteps().isEmpty();
				}
				return !item.getChildren().isEmpty();
			}

			@Override
			protected Stream<Step> fetchChildrenFromBackEnd(HierarchicalQuery<Step, Void> query) {
				Step parent = query.getParent();
				List<Step> children = null;
				if (parent == null) {
					children = getRootSteps();
				} else {
					children = parent.getChildren();
				}
				return children.stream();
			}

			private List<Step> getRootSteps() {
				return gantt.getSteps()
				    .filter(Step::isRoot)
				    .collect(Collectors.toList());
			}
		};

		gantt = new GanttTree(dataProvider);
		initGantt();
		buildCaptionGrid();
		populateGantt();
		Div controlPanel = buildControlPanel();

		scrollWrapper = new FlexLayout();
		scrollWrapper.setId("scroll-wrapper");
		scrollWrapper.setMinHeight("0");
		scrollWrapper.setWidthFull();
		scrollWrapper.add(
		    getCaptionGrid(),
		    gantt);
		scrollWrapper.setFlexGrow(0, getCaptionGrid());
		scrollWrapper.setFlexGrow(1, gantt);

		add(controlPanel, scrollWrapper);
	}

	private Grid<Step> getCaptionGrid() {
		return gantt.getCaptionGrid();
	}

	private void buildCaptionGrid() {
		Grid<Step> grid = gantt.buildCaptionGrid("Header");
		grid.setWidth("auto");
		grid.setAllRowsVisible(true);
	}

	private void initGantt() {
		gantt.setResolution(Resolution.Day);
		gantt.setStartDate(LocalDateTime.now().toLocalDate());
		gantt.setEndDateTime(LocalDateTime.now().plusDays(SUBSET_COUNT * SUBSET_COUNT * SUBSET_COUNT));
		gantt.setLocale(UI.getCurrent().getLocale());
		gantt.setTimeZone(TimeZone.getDefault());

		gantt.addGanttClickListener(this::onBackgroundClick);
		gantt.addStepClickListener(this::onStepClick);
		gantt.addStepMoveListener(this::onStepMove);
		gantt.addStepResizeListener(this::onStepResize);

		// Add dynamic context menu for gantt background. Clicked index is registered via addGanttClickListener and addStepClickListener.
		addDynamicBackgroundContextMenu(gantt);

		gantt.onAddStep(this::postStepAdded);
	}

	private void populateGantt() {
		Step step1 = newStep();
		step1.setCaption("Base-Step-1");
		// step1.setBackgroundColor("#9cfb84");
		LocalDateTime start1 = LocalDateTime.now();
		step1.setStartDate(start1);
		LocalDateTime end1 = start1.plusDays(SUBSET_COUNT * SUBSET_COUNT);
		step1.setEndDate(end1);
		addStep(null, step1);
		createChildSteps(step1);

		Step step2 = newStep();
		step1.setCaption("Base-Step-2");
		step2.setPredecessor(step1);
		// step2.setBackgroundColor("#a3d9ff");
		step2.setStartDate(end1);
		step2.setEndDate(end1.plusDays(SUBSET_COUNT * SUBSET_COUNT));
		addStep(null, step2);
		createChildSteps(step2);
	}

	private void createChildSteps(Step step) {
		LocalDateTime date = step.getStartDate();
		for (int i = 0; i < SUBSET_COUNT; i++) {
			Step subStep = newStep();
			subStep.setCaption(step.getCaption() + "-" + (i + 1));
			subStep.setStartDate(date);
			date = date.plusDays(SUBSET_COUNT);
			subStep.setEndDate(date);
			addStep(step, subStep);
			createGrandChildSteps(subStep);
		}
	}

	private void createGrandChildSteps(Step step) {
		LocalDateTime date = step.getStartDate();
		for (int i = 0; i < SUBSET_COUNT; i++) {
			Step subStep = newStep();
			subStep.setCaption(step.getCaption() + "-" + (i + 1));
			subStep.setStartDate(date);
			date = date.plusDays(1);
			subStep.setEndDate(date);
			addStep(step, subStep);
		}
	}

	private void addStep(Step parent, Step child) {
		if (parent == null) {
			gantt.addStep(child); // this is a root
		} else {
			parent.addChild(child); // dont add it to the tree until the parent is expanded
		}
	}

	private void postStepAdded(Step step) {
		// Add tooltip for step
		gantt.getStepElement(step.getUid()).addTooltip("Tooltip for " + step.getCaption());
		addDynamicStepContextMenu(gantt.getStepElement(step.getUid()));
	}

	// private void addSubStep(SubStep subStep) {
	// gantt.addSubStep(subStep);
	// addDynamicSubStepContextMenu(gantt.getStepElement(subStep.getUid()));
	// }

	private void onStepResize(StepResizeEvent event) {
		Notification.show("Resized step : " + event.getAnyStep().getCaption());

		event.getAnyStep().setStartDate(event.getStart());
		event.getAnyStep().setEndDate(event.getEnd());

		if (event.getAnyStep().isSubstep()) {
			((SubStep) event.getAnyStep()).updateOwnerDatesBySubStep();
			event.getSource().refresh(((SubStep) event.getAnyStep()).getOwner().getUid());
		}

	}

	private void onStepMove(StepMoveEvent event) {
		Notification.show("Moved step : " + event.getAnyStep().getCaption());

		// dates and position are not synchronized automatically to server side model
		event.getAnyStep().setStartDate(event.getStart());
		event.getAnyStep().setEndDate(event.getEnd());

		gantt.moveStep(gantt.indexOf(event.getNewUid()), event.getAnyStep());
	}

	private void addDynamicBackgroundContextMenu(GanttTree gantt2) {
		ContextMenu backgroundContextMenu = new ContextMenu();
		backgroundContextMenu.setTarget(gantt);
		gantt2.getElement().addEventListener("vaadin-context-menu-before-open", event -> {
			backgroundContextMenu.removeAll();
			backgroundContextMenu.addItem("Add step at index " + clickedBackgroundIndex,
			    e -> onHandleInsertStep(clickedBackgroundIndex, clickedBackgroundDate));
			var targetStep = getSelectedStep();
			// backgroundContextMenu.addItem("Add sub-step for " + targetStep.getCaption(),
			// e -> onHandleAddSubStep(targetStep.getUid()));
			backgroundContextMenu.add(new Hr());
			backgroundContextMenu.addItem("Remove step " + targetStep.getCaption(),
			    e -> onHandleRemoveStepContextMenuAction(targetStep.getUid()));
		});
	}

	// private void addDynamicSubStepContextMenu(StepElement stepElement) {
	// stepElement.addContextMenu((contextMenu, uid) -> {
	// contextMenu.removeAll();
	//
	// contextMenu.addItem("Insert Step Before",
	// e -> onHandleInsertStep(clickedBackgroundIndex, stepElement.getStartDateTime()));
	//
	// contextMenu.addItem("Insert Step After",
	// e -> onHandleInsertStep(clickedBackgroundIndex + 1, stepElement.getEndDateTime()));
	//
	// //cant add a substep to a substep
	//
	// contextMenu.add(new Hr());
	//
	// contextMenu.addItem("Remove step " + stepElement.getCaption(),
	// e -> onHandleRemoveStepContextMenuAction(uid));
	// });
	// }

	private Step getSelectedStep() {
		return gantt.getStepsList().get(clickedBackgroundIndex);
	}

	private void addDynamicStepContextMenu(StepElement stepElement) {
		stepElement.addContextMenu((contextMenu, uid) -> {
			contextMenu.removeAll();

			contextMenu.addItem("Insert Step Before",
			    e -> onHandleInsertStep(clickedBackgroundIndex, stepElement.getStartDateTime()));

			contextMenu.addItem("Insert Step After",
			    e -> onHandleInsertStep(clickedBackgroundIndex + 1, stepElement.getEndDateTime()));

			// var targetStep = getSelectedStep();
			// contextMenu.addItem("Add sub-step for " + targetStep.getCaption(),
			// e -> onHandleAddSubStep(targetStep.getUid()));

			contextMenu.add(new Hr());

			contextMenu.addItem("Remove step " + stepElement.getCaption(),
			    e -> onHandleRemoveStepContextMenuAction(uid));
		});
	}

	private void onHandleRemoveStepContextMenuAction(String uid) {
		gantt.removeAnyStep(uid);
		// TODO: Dec 18, 2023 RJM If this was a Step, also remove all substeps
	}

	// private void onHandleAddSubStep(String uid) {
	// var substep = createDefaultSubStep(uid);
	// gantt.addSubStep(substep);
	// addDynamicSubStepContextMenu(gantt.getStepElement(substep.getUid()));
	// }

	private void onHandleInsertStep(int index, LocalDateTime startDate) {
		var step = createDefaultNewStep();
		if (startDate != null) {
			step.setStartDate(startDate);
			step.setEndDate(startDate.plusDays(7));
		}
		gantt.addStep(index, step);
	}

	private void onBackgroundClick(GanttClickEvent event) {
		clickedBackgroundIndex = event.getIndex() != null ? event.getIndex() : 0;
		clickedBackgroundDate = event.getDate();
		if (event.getButton() == 2) {
			Notification.show("Background clicked with mouse 2 at index: " + event.getIndex());
		} else {
			Notification.show("Background clicked at index: " + event.getIndex() + " at date " + event.getDate().format(DateTimeFormatter.ofPattern("M/d/yyyy HH:mm")));
		}
	}

	private void onStepClick(StepClickEvent event) {
		clickedBackgroundIndex = event.getIndex();
		Notification.show("Clicked on step " + event.getAnyStep().getCaption());
	}

	private Div buildControlPanel() {
		Div div = new Div();
		div.setWidthFull();

		MenuBar menu = buildMenu();
		HorizontalLayout tools = createTools();
		div.add(menu, tools);
		return div;
	}

	private HorizontalLayout createTools() {
		HorizontalLayout tools = new HorizontalLayout();
		Select<Resolution> resolutionField = new Select<>();
		resolutionField.setItems(Resolution.values());
		resolutionField.setLabel("Resolution");
		resolutionField.setValue(gantt.getResolution());
		resolutionField.addValueChangeListener(event -> {
			gantt.setResolution(event.getValue());
			if (event.getValue() == Resolution.Hour) {
				startDateTimeToModel(startDateField.getValue().atTime(startTimeField.getValue()));
				endDateTimeToModel(endDateField.getValue().atTime(endTimeField.getValue()));
			} else {
				gantt.setStartDate(startDateField.getValue());
				gantt.setEndDate(endDateField.getValue());
			}
			setupToolsByResolution(event.getValue());
		});

		startDateField.setValue(gantt.getStartDate());
		startDateField.setLabel("Start Date");
		startDateField.addValueChangeListener(event -> gantt.setStartDate(event.getValue()));

		startTimeField.setLabel("Start Time");
		startTimeField.setValue(gantt.getStartDateTime().toLocalTime());
		startTimeField.setWidth("8em");
		startTimeField.addValueChangeListener(
		    event -> gantt.setStartDateTime(startDateField.getValue().atTime(event.getValue())));

		endDateField.setValue(gantt.getEndDate());
		endDateField.setLabel("End Date");
		endDateField.addValueChangeListener(
		    event -> gantt.setEndDate(event.getValue()));

		endTimeField.setValue(gantt.getEndDateTime().toLocalTime());
		endTimeField.setLabel("End Time (inclusive)");
		endTimeField.setWidth("8em");
		endTimeField.addValueChangeListener(
		    event -> gantt.setEndDateTime(endDateField.getValue().atTime(event.getValue())));

		tools.add(resolutionField, startDateField, startTimeField, endDateField, endTimeField);
		tools.add(createTimeZoneField());
		tools.add(createLocaleField());

		setupToolsByResolution(gantt.getResolution());
		return tools;
	}

	private void startDateTimeToModel(LocalDateTime dt) {
		gantt.setStartDateTime(dt);
	}

	private void endDateTimeToModel(LocalDateTime dt) {
		gantt.setEndDateTime(dt);
	}

	private void startDateTimeToDisplay(LocalDateTime dt) {
		gantt.setStartDateTime(dt);
	}

	private void setupToolsByResolution(Resolution value) {
		if (Resolution.Hour.equals(value)) {
			startTimeField.setVisible(true);
			endTimeField.setVisible(true);
		} else {
			startTimeField.setVisible(false);
			endTimeField.setVisible(false);
		}
	}

	private ComboBox<String> createTimeZoneField() {
		ComboBox<String> timeZoneField = new ComboBox<>("Timezone", getSupportedTimeZoneIds());
		timeZoneField.setWidth("350px");
		timeZoneField.setValue("Default");
		timeZoneField.setItemLabelGenerator(item -> {
			if ("Default".equals(item)) {
				return "Default (" + getDefaultTimeZone().getDisplayName(TextStyle.FULL, UI.getCurrent().getLocale())
				    + ")";
			}
			TimeZone tz = TimeZone.getTimeZone(item);
			return tz.getID() + " (raw offset " + (tz.getRawOffset() / 60000) + "m)";
		});
		timeZoneField.addValueChangeListener(e -> Optional.ofNullable(e.getValue()).ifPresent(zoneId -> {
			if ("Default".equals(zoneId)) {
				gantt.setTimeZone(TimeZone.getTimeZone(getDefaultTimeZone()));
			} else {
				gantt.setTimeZone(TimeZone.getTimeZone(ZoneId.of(zoneId)));
			}
		}));
		return timeZoneField;
	}

	private ComboBox<Locale> createLocaleField() {
		ComboBox<Locale> localeField = new ComboBox<>("Locale",
		    Stream.of(Locale.getAvailableLocales()).collect(Collectors.toList()));
		localeField.setWidth("350px");
		localeField.setItemLabelGenerator(l -> l.getDisplayName(UI.getCurrent().getLocale()));
		localeField.setValue(gantt.getLocale());
		localeField.addValueChangeListener(e -> Optional.ofNullable(e.getValue()).ifPresent(l -> gantt.setLocale(l)));
		return localeField;
	}

	private MenuBar buildMenu() {

		MenuBar menu = new MenuBar();
		MenuItem menuView = menu.addItem("View");
		MenuItem sizeMenu = menuView.getSubMenu().addItem("Size");
		MenuItem size100x100 = sizeMenu.getSubMenu().addItem(SizeOption.FULL_SIZE.getText());
		size100x100.setChecked(this.size == SizeOption.FULL_SIZE);
		size100x100.setCheckable(true);
		MenuItem size100xAuto = sizeMenu.getSubMenu().addItem(SizeOption.FULL_WIDTH.getText());
		size100xAuto.setCheckable(true);
		size100xAuto.setChecked(this.size == SizeOption.FULL_WIDTH);
		MenuItem size50x100 = sizeMenu.getSubMenu().addItem(SizeOption.HALF_WIDTH.getText());
		size50x100.setCheckable(true);
		size100x100.setChecked(this.size == SizeOption.HALF_WIDTH);
		MenuItem size100x50 = sizeMenu.getSubMenu().addItem(SizeOption.HALF_HEIGHT.getText());
		size100x50.setCheckable(true);
		size100x100.setChecked(this.size == SizeOption.HALF_HEIGHT);

		size100x100.addClickListener(event -> {
			setSize(SizeOption.FULL_SIZE);
			event.getSource().setChecked(true);
			size100xAuto.setChecked(false);
			size100x50.setChecked(false);
			size50x100.setChecked(false);
		});
		size100xAuto.addClickListener(event -> {
			setSize(SizeOption.FULL_WIDTH);
			event.getSource().setChecked(true);
			size100x100.setChecked(false);
			size100x50.setChecked(false);
			size50x100.setChecked(false);
		});
		size50x100.addClickListener(event -> {
			setSize(SizeOption.HALF_WIDTH);
			event.getSource().setChecked(true);
			size100xAuto.setChecked(false);
			size100x100.setChecked(false);
			size100x50.setChecked(false);
		});
		size100x50.addClickListener(event -> {
			setSize(SizeOption.HALF_HEIGHT);
			event.getSource().setChecked(true);
			size100xAuto.setChecked(false);
			size100x100.setChecked(false);
			size50x100.setChecked(false);
		});

		initMenu_TwelveHourClock(menuView);
		initMenu_ShowYear(menuView);
		initMenu_ShowMonth(menuView);
		initMenu_ShowCaptionGrid(menuView);
		MenuItem menuEdit = menu.addItem("Edit");
		initMenu_MovableSteps(menuEdit);
		initMenu_ResizableSteps(menuEdit);
		initMenu_MovableStepsBetweenRows(menuEdit);
		initMenu_AddNewStep(menu);
		initMenu_ClampDates(menu);

		return menu;
	}

	private void initMenu_ClampDates(MenuBar menu) {
		MenuItem menuClampDates = menu.addItem("Clamp Dates");
		menuClampDates.addClickListener(event -> clampDates());
	}

	private void initMenu_AddNewStep(MenuBar menu) {
		MenuItem menuAdd = menu.addItem("Add new Step");
		menuAdd.addClickListener(event -> insertNewStep());
	}

	private void initMenu_ShowMonth(MenuItem menuView) {
		MenuItem showMonth = menuView.getSubMenu().addItem("Show month");
		showMonth.addClickListener(event -> {
			gantt.setMonthRowVisible(event.getSource().isChecked());
		});
		showMonth.setCheckable(true);
		showMonth.setChecked(gantt.isMonthRowVisible());
	}

	private void initMenu_ShowYear(MenuItem menuView) {
		MenuItem showYear = menuView.getSubMenu().addItem("Show year");
		showYear.addClickListener(event -> {
			gantt.setYearRowVisible(event.getSource().isChecked());
		});
		showYear.setCheckable(true);
		showYear.setChecked(gantt.isYearRowVisible());
	}

	private void initMenu_TwelveHourClock(MenuItem menuView) {
		MenuItem twelveHourClock = menuView.getSubMenu().addItem("Twelve hour clock");
		twelveHourClock.addClickListener(event -> {
			gantt.setTwelveHourClock(event.getSource().isChecked());
		});
		twelveHourClock.setCheckable(true);
		twelveHourClock.setChecked(gantt.isTwelveHourClock());
	}

	private void initMenu_ShowCaptionGrid(MenuItem menuView) {
		MenuItem showCaptionGrid = menuView.getSubMenu().addItem("Show Caption Grid");
		Grid<Step> grid = getCaptionGrid();
		showCaptionGrid.addClickListener(event -> {
			if (event.getSource().isChecked()) {
				buildCaptionGrid();
				scrollWrapper.addComponentAsFirst(getCaptionGrid());
				scrollWrapper.setFlexGrow(0, getCaptionGrid());
			} else {
				gantt.removeCaptionGrid();
				scrollWrapper.remove(grid);
			}
			setSize(this.size);
		});
		showCaptionGrid.setCheckable(true);
		showCaptionGrid.setChecked(grid.isVisible());
	}

	private void initMenu_ResizableSteps(MenuItem menuEdit) {
		MenuItem resizableSteps = menuEdit.getSubMenu().addItem("Resizable steps");
		resizableSteps.addClickListener(event -> {
			gantt.setResizableSteps(event.getSource().isChecked());
		});
		resizableSteps.setCheckable(true);
		resizableSteps.setChecked(gantt.isResizableSteps());
	}

	private void initMenu_MovableSteps(MenuItem menuEdit) {
		MenuItem movableSteps = menuEdit.getSubMenu().addItem("Movable steps");
		movableSteps.addClickListener(event -> {
			gantt.setMovableSteps(event.getSource().isChecked());
		});
		movableSteps.setCheckable(true);
		movableSteps.setChecked(gantt.isMovableSteps());
	}

	private void initMenu_MovableStepsBetweenRows(MenuItem menuEdit) {
		MenuItem movableStepsBetweenRows = menuEdit.getSubMenu().addItem("Movable steps between rows");
		movableStepsBetweenRows.addClickListener(event -> {
			gantt.setMovableStepsBetweenRows(event.getSource().isChecked());
		});
		movableStepsBetweenRows.setCheckable(true);
		movableStepsBetweenRows.setChecked(gantt.isMovableStepsBetweenRows());
	}

	private void setSize(SizeOption newSize) {
		this.size = newSize;
		Grid<Step> caption = getCaptionGrid();
		switch (size) {
			case FULL_SIZE:
				setSizeFull();
				gantt.setWidth("70%");
				gantt.setHeight("100%");
				if (caption != null) {
					caption.setWidth("30%");
					caption.setHeight("100%");
				}
				setFlexGrow(1, scrollWrapper);
				break;
			case FULL_WIDTH:
				setWidthFull();
				setHeight(null);
				gantt.setWidth("70%");
				gantt.setHeight(null);
				if (caption != null) {
					// grid.setWidth("30%");
					caption.setWidth("auto");
					caption.setHeight(null);
					caption.setAllRowsVisible(true);
				}
				setFlexGrow(0, scrollWrapper);
				break;
			case HALF_WIDTH:
				setSizeFull();
				gantt.setWidth("40%");
				gantt.setHeight("100%");
				if (caption != null) {
					caption.setWidth("10%");
					caption.setHeight("100%");
				}
				setFlexGrow(1, scrollWrapper);
				break;
			case HALF_HEIGHT:
				setSizeFull();
				gantt.setWidth("70%");
				gantt.setHeight("50%");
				if (caption != null) {
					caption.setWidth("30%");
					caption.setHeight("50%");
				}
				setFlexGrow(1, scrollWrapper);
				break;
		}
	}

	private ZoneId getDefaultTimeZone() {
		ZoneId zone = ZoneId.systemDefault();
		return zone;
	}

	private List<String> getSupportedTimeZoneIds() {
		List<String> items = new ArrayList<>();
		items.add("Default");
		items.addAll(Arrays.asList(TimeZone.getAvailableIDs()));
		return items;
	}

	private void insertNewStep() {
		var step = createDefaultNewStep();
		gantt.addStep(step);
	}

	private Step createDefaultNewStep() {
		Step step = newStep();
		step.setBackgroundColor(String.format("#%06x", new Random().nextInt(0xffffff + 1)));
		LocalDateTime defaultStartDate = GanttUtil.clampStartDate(gantt, clickedBackgroundDate);
		step.setStartDate(defaultStartDate);
		step.setEndDate(defaultStartDate.plusDays(1));
		return step;
	}

	private Step newStep() {
		stepCounter++;
		Step step = new Step();
		step.setCaption("New Step " + stepCounter);
		return step;
	}

	// private SubStep createDefaultSubStep(String ownerUid) {
	// var owner = gantt.getStep(ownerUid);
	// SubStep substep = new SubStep(owner);
	// substep.setCaption("New Sub Step");
	// substep.setBackgroundColor(String.format("#%06x", new Random().nextInt(0xffffff + 1)));
	// if (gantt.getSubStepElements(ownerUid).count() == 0) {
	// substep.setStartDate(owner.getStartDate());
	// substep.setEndDate(owner.getEndDate());
	// } else {
	// substep.setStartDate(owner.getEndDate());
	// substep.setEndDate(owner.getEndDate().plusDays(7));
	// owner.setEndDate(substep.getEndDate());
	// gantt.refresh(ownerUid);
	// }
	// return substep;
	// }

	// Set the display start and end dates to be the start and end dates of the gannt
	private void clampDates() {
		LocalDateTime earliest = GanttUtil.getEarliestStepStart(gantt);
		startDateTimeToModel(earliest);
		startDateField.setValue(earliest.toLocalDate());
		startTimeField.setValue(earliest.toLocalTime());

		LocalDateTime latest = GanttUtil.getLatestStepEnd(gantt);
		endDateField.setValue(latest.toLocalDate());
		endTimeField.setValue(latest.toLocalTime());
	}

	enum SizeOption {

		FULL_SIZE("100% x 100%"),
		FULL_WIDTH("100% x auto"),
		HALF_WIDTH("50% x 100%"),
		HALF_HEIGHT("100% x 50%");

		private final String text;
		SizeOption(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}
	}
}
