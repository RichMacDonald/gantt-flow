package org.vaadin.tltv.gantt.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.vaadin.tltv.gantt.Gantt;

/**
 * A step inside {@link Gantt} component.
 */
public class Step extends GanttStep {

	private Step predecessor;

	private final List<Step> children = new ArrayList<>();
	private Step parent;

	public Step() {
		setUid(UUID.randomUUID().toString()); // The GanttSetp has a bug where equals fails if uid is null
	}

	public Step getParent() {
		return parent;
	}

	public void setParent(Step parent) {
		this.parent = parent;
		parent.addChild(this);
	}

	public List<Step> getChildren() {
		return children;
	}

	public List<Step> getDescendants() {
		List<Step> descendants = new ArrayList<>();
		addIntoDescendants(descendants);
		return descendants;
	}

	private void addIntoDescendants(List<Step> descendants) {
		for (Step child : children) {
			descendants.add(child);
			child.addIntoDescendants(descendants);
		}
	}

	public void addChild(Step child) {
		if (children.contains(child)) {
			return;
		}
		children.add(child);
		child.setParent(this);
	}

	public boolean hasChildren() {
		return !children.isEmpty();
	}

	public boolean hasParent() {
		return parent != null;
	}

	public boolean isRoot() {
		return !hasParent();
	}

	public Step getPredecessor() {
		return predecessor;
	}

	public void setPredecessor(Step predecessor) {
		this.predecessor = predecessor;
	}

	@Override
	public boolean isSubstep() {
		return false;
	}

}
