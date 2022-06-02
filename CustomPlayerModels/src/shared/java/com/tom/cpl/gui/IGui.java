package com.tom.cpl.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import com.tom.cpl.math.Box;
import com.tom.cpl.math.Vec2i;
import com.tom.cpm.shared.MinecraftClientAccess;
import com.tom.cpm.shared.util.ErrorLog;
import com.tom.cpm.shared.util.ErrorLog.LogLevel;
import com.tom.cpm.shared.util.Log;

public interface IGui {
	void drawBox(int x, int y, int w, int h, int color);
	void drawGradientBox(int x, int y, int w, int h, int topLeft, int topRight, int bottomLeft, int bottomRight);
	void drawText(int x, int y, String text, int color);
	String i18nFormat(String key, Object... obj);
	int textWidth(String text);
	void drawTexture(int x, int y, int w, int h, int u, int v, String texture);
	void drawTexture(int x, int y, int w, int h, int u, int v, String texture, int color);
	void drawTexture(int x, int y, int width, int height, float u1, float v1, float u2, float v2);
	void close();
	UIColors getColors();
	void setCloseListener(Consumer<Runnable> listener);
	boolean isShiftDown();
	boolean isCtrlDown();
	boolean isAltDown();
	KeyCodes getKeyCodes();
	NativeGuiComponents getNative();
	void setClipboardText(String text);
	String getClipboardText();
	Frame getFrame();
	void setScale(int value);
	int getScale();
	int getMaxScale();
	CtxStack getStack();
	void displayError(String msg);

	default void drawBox(float x, float y, float w, float h, int color) {
		drawBox((int) x, (int) y, (int) w, (int) h, color);
	}

	default void executeLater(Runnable r) {
		MinecraftClientAccess.get().executeLater(() -> {
			try {
				r.run();
			} catch (Throwable e) {
				Log.error("Exception while executing task", e);
				ErrorLog.addLog(LogLevel.ERROR, "Exception while executing task", e);
			}
		});
	}

	public static class Ctx {
		public Vec2i off;
		public Box cutBox;

		public Ctx(Ctx old) {
			off = new Vec2i(old.off);
			cutBox = new Box(old.cutBox);
		}

		public Ctx(int w, int h) {
			off = new Vec2i(0, 0);
			cutBox = new Box(0, 0, w, h);
		}
	}

	default void pushMatrix() {
		getStack().push();
	}

	default void setPosOffset(Box box) {
		Ctx current = getContext();
		current.cutBox = current.cutBox.intersect(new Box(current.off.x + box.x, current.off.y + box.y, box.w, box.h));
		current.cutBox.w = Math.max(current.cutBox.w, 0);
		current.cutBox.h = Math.max(current.cutBox.h, 0);
		current.off = new Vec2i(current.off.x + box.x, current.off.y + box.y);
	}

	void setupCut();

	default void popMatrix() {
		getStack().pop();
	}

	default Ctx getContext() {
		return getStack().current;
	}

	public static class CtxStack {
		private Stack<Ctx> stack;
		private Ctx current;

		public CtxStack(int w, int h) {
			current = new Ctx(w, h);
			stack = new Stack<>();
		}

		public void push() {
			stack.push(current);
			current = new Ctx(current);
		}

		public Ctx pop() {
			return current = stack.pop();
		}
	}

	default Vec2i getOffset() {
		return getStack().current.off;
	}

	default void drawRectangle(int x, int y, int w, int h, int color) {
		drawBox(x, y, w, 1, color);
		drawBox(x, y, 1, h, color);
		drawBox(x, y+h-1, w, 1, color);
		drawBox(x+w-1, y, 1, h, color);
	}

	default void drawRectangle(float x, float y, float w, float h, int color) {
		drawBox(x, y, w, 1, color);
		drawBox(x, y, 1, h, color);
		drawBox(x, y+h-1, w, 1, color);
		drawBox(x+w-1, y, 1, h, color);
	}

	default void onGuiException(String msg, Throwable e, boolean fatal) {
		Log.error(msg, e);
		ErrorLog.addLog(LogLevel.ERROR, msg, e);
		if(fatal) {
			displayError(msg + ": " + e.toString());
		} else {
			Frame frm = getFrame();
			if(frm != null) {
				try {
					frm.logMessage(msg + ": " + e.toString());
				} catch (Throwable ex) {
					e.addSuppressed(ex);
					onGuiException(msg, e, true);
				}
			} else {
				displayError(msg + "\n" + e.toString());
			}
		}
	}

	default String wordWrap(String in, int w) {
		return wordWrap(in, w, this::textWidth);
	}

	public static String wordWrap(String in, int w, ToIntFunction<String> width) {
		List<String> text = new ArrayList<>();
		int splitStart = 0;
		int space = -1;
		for (int i = 0;i<in.length();i++) {
			char c = in.charAt(i);
			if(c == ' ' || c == '\\') {
				String s = in.substring(splitStart, i);
				int lw = width.applyAsInt(s);
				if(lw > w) {
					if(splitStart == space + 1) {
						text.add(s);
						splitStart = i + 1;
					} else {
						text.add(in.substring(splitStart, space));
						splitStart = space + 1;
					}
				}
				space = i;
			}
			if(c == '\\') {
				text.add(in.substring(splitStart, i));
				splitStart = i + 1;
				continue;
			}
		}
		text.add(in.substring(splitStart, in.length()));
		return text.stream().collect(Collectors.joining("\\"));
	}

	default void drawText(int x, int y, String text, int bgColor, int color) {
		drawBox(x, y, textWidth(text), 10, bgColor);
		drawText(x, y + 1, text, color);
	}
}
