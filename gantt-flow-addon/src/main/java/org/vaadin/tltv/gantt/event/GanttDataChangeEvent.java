package org.vaadin.tltv.gantt.event;

import org.vaadin.tltv.gantt.Gantt;
import com.vaadin.flow.component.ComponentEvent;

public class GanttDataChangeEvent extends ComponentEvent<Gantt> {

	private static final long serialVersionUID = 1L;

	public GanttDataChangeEvent(Gantt source) {
		super(source, false);
	}
}
