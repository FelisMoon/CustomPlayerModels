package com.tom.cpm.shared.animation;

import com.tom.cpl.gui.IGui;

public interface IPose {
	String getName(IGui gui, String display);

	default long getTime(AnimationState state, long time) {
		return time;
	}
}
