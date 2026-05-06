package org.vaadin.tltv.gantt.event;

import org.vaadin.tltv.gantt.Gantt;
import org.vaadin.tltv.gantt.model.GanttStep;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;

@DomEvent("ganttStepClick")
public class StepClickEvent extends ComponentEvent<Gantt> {

	private final String uid;
	private final Integer button;

	public StepClickEvent(Gantt source, boolean fromClient,
			@EventData("event.detail.uid") String uid,
			@EventData("event.detail.event.button") Integer button) {
		super(source, fromClient);
		this.uid = uid;
		this.button = button;
	}

	public GanttStep getAnyStep() {
		return getSource().getAnyStep(uid);
	}

	public int getIndex() {
		return getSource().indexOf(uid);
	}

	public Integer getButton() {
		return button;
	}

}
