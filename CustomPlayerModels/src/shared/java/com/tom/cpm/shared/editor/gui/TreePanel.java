package com.tom.cpm.shared.editor.gui;

import com.tom.cpl.gui.IGui;
import com.tom.cpl.gui.elements.ButtonIcon;
import com.tom.cpl.gui.elements.Panel;
import com.tom.cpl.gui.elements.ScrollPanel;
import com.tom.cpl.gui.elements.Tree;
import com.tom.cpl.math.Box;
import com.tom.cpm.shared.editor.Editor;
import com.tom.cpm.shared.editor.tree.TreeElement;

public class TreePanel extends Panel {

	public TreePanel(IGui gui, EditorGui e, int width, int height, boolean enableMod) {
		super(gui);
		setBounds(new Box(width - 150, 0, 150, height));
		setBackgroundColor(gui.getColors().panel_background);
		Editor editor = e.getEditor();

		ScrollPanel treePanel = new ScrollPanel(gui);
		treePanel.setBounds(new Box(0, 0, 150, height - 30));
		addElement(treePanel);

		Tree<TreeElement> tree = new Tree<>(e, editor.treeHandler);
		editor.updateGui.add(tree::updateTree);

		Panel tp = new Panel(gui);
		treePanel.setDisplay(tp);
		tp.addElement(tree);

		tree.setSizeUpdate(s -> {
			int w = Math.max(s.x, 146);
			int h = Math.max(s.y, height - 34);
			tp.setBounds(new Box(0, 0, w, h));
			tree.setBounds(new Box(0, 0, w, h));
		});

		if(enableMod) {
			ButtonIcon newBtn = new ButtonIcon(gui, "editor", 0, 16, editor::addNew);
			newBtn.setBounds(new Box(5, height - 24, 18, 18));
			addElement(newBtn);
			editor.setAddEn.add(newBtn::setEnabled);

			ButtonIcon delBtn = new ButtonIcon(gui, "editor", 14, 16, editor::deleteSel);
			delBtn.setBounds(new Box(25, height - 24, 18, 18));
			addElement(delBtn);
			editor.setDelEn.add(delBtn::setEnabled);
		}

		ButtonIcon visBtn = new ButtonIcon(gui, "editor", 42, 16, editor::switchVis);
		visBtn.setBounds(new Box(enableMod ? 45 : 5, height - 24, 18, 18));
		addElement(visBtn);
		editor.setVis.add(b -> {
			if(b == null) {
				visBtn.setEnabled(false);
				visBtn.setU(42);
			} else {
				visBtn.setEnabled(true);
				visBtn.setU(b ? 42 : 28);
			}
		});
	}

}
