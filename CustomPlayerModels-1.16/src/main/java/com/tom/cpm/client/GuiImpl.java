package com.tom.cpm.client;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ErrorScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.ChatVisibility;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.fml.ModList;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import com.tom.cpl.gui.Frame;
import com.tom.cpl.gui.IGui;
import com.tom.cpl.gui.KeyCodes;
import com.tom.cpl.gui.KeyboardEvent;
import com.tom.cpl.gui.MouseEvent;
import com.tom.cpl.gui.NativeGuiComponents;
import com.tom.cpl.gui.NativeGuiComponents.NativeConstructor;
import com.tom.cpl.gui.UIColors;
import com.tom.cpl.gui.elements.FileChooserPopup;
import com.tom.cpl.gui.elements.TextField;
import com.tom.cpl.gui.elements.TextField.ITextField;
import com.tom.cpl.math.Box;
import com.tom.cpl.math.Vec2i;
import com.tom.cpm.client.MinecraftObject.DynTexture;
import com.tom.cpm.shared.gui.ViewportPanelBase;

public class GuiImpl extends Screen implements IGui {
	private static final KeyCodes CODES = new GLFWKeyCodes();
	private static final NativeGuiComponents nativeComponents = new NativeGuiComponents();
	private Frame gui;
	private Screen parent;
	private CtxStack stack;
	private UIColors colors;
	private Consumer<Runnable> closeListener;
	private int keyModif;
	public MatrixStack matrixStack;
	private boolean noScissorTest;
	private int vanillaScale = -1;

	static {
		nativeComponents.register(ViewportPanelBase.class, ViewportPanelImpl::new);
		nativeComponents.register(TextField.class, local(GuiImpl::createTextField));
		nativeComponents.register(FileChooserPopup.class, TinyFDChooser::new);
	}

	public GuiImpl(Function<IGui, Frame> creator, Screen parent) {
		super(new StringTextComponent(""));
		this.colors = new UIColors();
		this.parent = parent;
		try {
			this.gui = creator.apply(this);
		} catch (Throwable e) {
			onGuiException("Error creating gui", e, true);
		}
		noScissorTest = isCtrlDown();
	}

	private static <G extends Supplier<IGui>, N> NativeConstructor<G, N> local(Function<GuiImpl, N> fac) {
		return f -> fac.apply((GuiImpl) f.get());
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		renderBackground(matrixStack, 0);
		try {
			this.matrixStack = matrixStack;
			matrixStack.push();
			matrixStack.translate(0, 0, 1000);
			if(!noScissorTest)
				GL11.glEnable(GL11.GL_SCISSOR_TEST);
			stack = new CtxStack(width, height);
			RenderSystem.runAsFancy(() -> gui.draw(mouseX, mouseY, partialTicks));
		} catch (Throwable e) {
			onGuiException("Error drawing gui", e, true);
		} finally {
			if(!noScissorTest)
				GL11.glDisable(GL11.GL_SCISSOR_TEST);
			String modVer = ModList.get().getModContainerById("cpm").map(m -> m.getModInfo().getVersion().toString()).orElse("?UNKNOWN?");
			String s = "Minecraft " + SharedConstants.getVersion().getName() + " (" + minecraft.getVersion() + "/" + ClientBrandRetriever.getClientModName() + ("release".equalsIgnoreCase(minecraft.getVersionType()) ? "" : "/" + minecraft.getVersionType()) + ") " + modVer;
			font.drawString(matrixStack, s, width - font.getStringWidth(s) - 4, 2, 0xff000000);
			s = minecraft.debug;
			if(noScissorTest)s += " No Scissor";
			font.drawString(matrixStack, s, width - font.getStringWidth(s) - 4, 11, 0xff000000);
			this.matrixStack = null;
			matrixStack.pop();
		}
		if(minecraft.player != null) {
			try {
				Method m = ForgeIngameGui.class.getDeclaredMethod("renderChat", int.class, int.class, MatrixStack.class);
				m.setAccessible(true);
				m.invoke(minecraft.ingameGUI, minecraft.getMainWindow().getScaledWidth(), minecraft.getMainWindow().getScaledHeight(), matrixStack);
			} catch (Throwable e) {
			}
		}
	}

	@Override
	public void onClose() {
		if(parent != null) {
			Screen p = parent;
			parent = null;
			minecraft.displayGuiScreen(p);
		}
		if(vanillaScale != -1 && vanillaScale != minecraft.gameSettings.guiScale) {
			minecraft.gameSettings.guiScale = vanillaScale;
			minecraft.updateWindowSize();
		}
	}

	@Override
	public void drawBox(int x, int y, int w, int h, int color) {
		x += getOffset().x;
		y += getOffset().y;
		fill(matrixStack, x, y, x+w, y+h, color);
	}

	@Override
	public void drawBox(float x, float y, float w, float h, int color) {
		x += getOffset().x;
		y += getOffset().y;

		float minX = x;
		float minY = y;
		float maxX = x+w;
		float maxY = y+h;

		if (minX < maxX) {
			float i = minX;
			minX = maxX;
			maxX = i;
		}

		if (minY < maxY) {
			float j = minY;
			minY = maxY;
			maxY = j;
		}
		Matrix4f matrix = matrixStack.getLast().getMatrix();
		float f3 = (color >> 24 & 255) / 255.0F;
		float f = (color >> 16 & 255) / 255.0F;
		float f1 = (color >> 8 & 255) / 255.0F;
		float f2 = (color & 255) / 255.0F;
		BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
		bufferbuilder.pos(matrix, minX, maxY, 0.0F).color(f, f1, f2, f3).endVertex();
		bufferbuilder.pos(matrix, maxX, maxY, 0.0F).color(f, f1, f2, f3).endVertex();
		bufferbuilder.pos(matrix, maxX, minY, 0.0F).color(f, f1, f2, f3).endVertex();
		bufferbuilder.pos(matrix, minX, minY, 0.0F).color(f, f1, f2, f3).endVertex();
		bufferbuilder.finishDrawing();
		WorldVertexBufferUploader.draw(bufferbuilder);
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
	}

	@Override
	protected void init() {
		try {
			gui.init(width, height);
		} catch (Throwable e) {
			onGuiException("Error in init gui", e, true);
		}
	}
	@Override
	public void drawText(int x, int y, String text, int color) {
		x += getOffset().x;
		y += getOffset().y;
		font.drawString(matrixStack, text, x, y, color);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		try {
			this.keyModif = modifiers;
			KeyboardEvent evt = new KeyboardEvent(keyCode, scanCode, (char) -1, GLFW.glfwGetKeyName(keyCode, scanCode));
			gui.keyPressed(evt);
			if(!evt.isConsumed()) {
				if(minecraft.player != null && minecraft.gameSettings.keyBindChat.matchesKey(keyCode, scanCode) && minecraft.gameSettings.chatVisibility != ChatVisibility.HIDDEN) {
					minecraft.displayGuiScreen(new Overlay());
					return true;
				}
			}
			return true;
		} catch (Throwable e) {
			onGuiException("Error processing key event", e, false);
			return true;
		}
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		try {
			this.keyModif = modifiers;
			KeyboardEvent evt = new KeyboardEvent(-1, -1, codePoint, null);
			gui.keyPressed(evt);
			return evt.isConsumed();
		} catch (Throwable e) {
			onGuiException("Error processing key event", e, false);
			return true;
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		try {
			MouseEvent evt = new MouseEvent((int) mouseX, (int) mouseY, button);
			gui.mouseClick(evt);
			return evt.isConsumed();
		} catch (Throwable e) {
			onGuiException("Error processing mouse event", e, false);
			return true;
		}
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		try {
			MouseEvent evt = new MouseEvent((int) mouseX, (int) mouseY, button);
			gui.mouseDrag(evt);
			return evt.isConsumed();
		} catch (Throwable e) {
			onGuiException("Error processing mouse event", e, false);
			return true;
		}
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		try {
			MouseEvent evt = new MouseEvent((int) mouseX, (int) mouseY, button);
			gui.mouseRelease(evt);
			return evt.isConsumed();
		} catch (Throwable e) {
			onGuiException("Error processing mouse event", e, false);
			return true;
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if(delta != 0) {
			try {
				MouseEvent evt = new MouseEvent((int) mouseX, (int) mouseY, (int) delta);
				gui.mouseWheel(evt);
				return evt.isConsumed();
			} catch (Throwable e) {
				onGuiException("Error processing mouse event", e, false);
				return true;
			}
		}
		return false;
	}

	@Override
	public void displayError(String e) {
		Screen p = parent;
		parent = null;
		Minecraft.getInstance().displayGuiScreen(new ErrorScreen(new StringTextComponent("Custom Player Models"), new TranslationTextComponent("error.cpm.crash", e)) {
			private Screen parent = p;

			@Override
			public void onClose() {
				if(parent != null) {
					Screen p = parent;
					parent = null;
					minecraft.displayGuiScreen(p);
				}
			}
		});
	}

	@Override
	public void close() {
		if(closeListener != null) {
			closeListener.accept(() -> this.minecraft.displayGuiScreen((Screen)null));
		} else
			this.minecraft.displayGuiScreen((Screen)null);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void drawTexture(int x, int y, int w, int h, int u, int v, String texture) {
		minecraft.getTextureManager().bindTexture(new ResourceLocation("cpm", "textures/gui/" + texture + ".png"));
		x += getOffset().x;
		y += getOffset().y;
		RenderSystem.color4f(1, 1, 1, 1);
		blit(matrixStack, x, y, u, v, w, h);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void drawTexture(int x, int y, int width, int height, float u1, float v1, float u2, float v2) {
		x += getOffset().x;
		y += getOffset().y;
		RenderSystem.color4f(1, 1, 1, 1);
		minecraft.getTextureManager().bindTexture(DynTexture.getBoundLoc());
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
		float bo = getBlitOffset();
		Matrix4f matrix = matrixStack.getLast().getMatrix();
		bufferbuilder.pos(matrix, x, y + height, bo).tex(u1, v2).endVertex();
		bufferbuilder.pos(matrix, x + width, y + height, bo).tex(u2, v2).endVertex();
		bufferbuilder.pos(matrix, x + width, y, bo).tex(u2, v1).endVertex();
		bufferbuilder.pos(matrix, x, y, bo).tex(u1, v1).endVertex();
		RenderSystem.enableAlphaTest();
		tessellator.draw();
	}

	@Override
	public String i18nFormat(String key, Object... obj) {
		return I18n.format(key, obj);
	}

	@Override
	public void setupCut() {
		if(!noScissorTest) {
			int dw = minecraft.getMainWindow().getWidth();
			int dh = minecraft.getMainWindow().getHeight();
			float multiplierX = dw / (float)width;
			float multiplierY = dh / (float)height;
			Box box = getContext().cutBox;
			GL11.glScissor((int) (box.x * multiplierX), dh - (int) ((box.y + box.h) * multiplierY),
					(int) (box.w * multiplierX), (int) (box.h * multiplierY));
		}
	}

	@Override
	public int textWidth(String text) {
		return font.getStringWidth(text);
	}

	private ITextField createTextField() {
		return new TxtField();
	}

	private class TxtField implements ITextField, Consumer<String> {
		private TextFieldWidget field;
		private Runnable eventListener;
		private Vec2i currentOff = new Vec2i(0, 0);
		private Box bounds = new Box(0, 0, 0, 0);
		private boolean settingText, updateField, enabled;
		public TxtField() {
			this.field = new TextFieldWidget(font, 0, 0, 0, 0, new TranslationTextComponent("narrator.cpm.field"));
			this.field.setMaxStringLength(1024*1024);
			this.field.setEnableBackgroundDrawing(false);
			this.field.setVisible(true);
			this.field.setTextColor(16777215);
			this.field.setResponder(this);
			this.enabled = true;
		}

		@Override
		public void draw(int mouseX, int mouseY, float partialTicks, Box bounds) {
			Vec2i off = getOffset();
			field.x = bounds.x + off.x + 4;
			field.y = bounds.y + off.y + 6;
			currentOff.x = off.x;
			currentOff.y = off.y;
			this.bounds.x = bounds.x;
			this.bounds.y = bounds.y;
			this.bounds.w = bounds.w;
			this.bounds.h = bounds.h;
			field.setWidth(bounds.w - 5);
			field.setHeight(bounds.h - 12);
			if(updateField) {
				settingText = true;
				field.setCursorPositionEnd();
				settingText = false;
				updateField = false;
			}
			field.render(matrixStack, mouseX, mouseY, partialTicks);
		}

		@Override
		public void keyPressed(KeyboardEvent evt) {
			if(evt.isConsumed())return;
			if(evt.keyCode == -1) {
				if(field.charTyped(evt.charTyped, keyModif))
					evt.consume();
			} else {
				if(field.keyPressed(evt.keyCode, evt.scancode, keyModif))
					evt.consume();
			}
		}

		@Override
		public void mouseClick(MouseEvent evt) {
			if(evt.isConsumed()) {
				field.mouseClicked(Integer.MIN_VALUE, Integer.MIN_VALUE, evt.btn);
				return;
			}
			field.x = bounds.x + currentOff.x;
			field.y = bounds.y + currentOff.y;
			field.setWidth(bounds.w);
			field.setHeight(bounds.h);
			if(field.mouseClicked(evt.x + currentOff.x, evt.y + currentOff.y, evt.btn))
				evt.consume();
		}

		@Override
		public String getText() {
			return field.getText();
		}

		@Override
		public void setText(String txt) {
			this.settingText = true;
			field.setText(txt);
			this.settingText = false;
			this.updateField = true;
		}

		@Override
		public void setEventListener(Runnable eventListener) {
			this.eventListener = eventListener;
		}

		@Override
		public void accept(String value) {
			if(eventListener != null && !settingText && enabled)eventListener.run();
		}

		@Override
		public void setEnabled(boolean enabled) {
			field.setEnabled(enabled);
			this.enabled = enabled;
		}

		@Override
		public boolean isFocused() {
			return field.isFocused();
		}

		@Override
		public void setFocused(boolean focused) {
			field.setFocused2(focused);
		}
	}

	@Override
	public UIColors getColors() {
		return colors;
	}

	@Override
	public void setCloseListener(Consumer<Runnable> listener) {
		this.closeListener = listener;
	}

	@Override
	public boolean isShiftDown() {
		return hasShiftDown();
	}

	@Override
	public boolean isCtrlDown() {
		return hasControlDown();
	}

	@Override
	public boolean isAltDown() {
		return hasAltDown();
	}

	public class Overlay extends ChatScreen {

		public Overlay() {
			super("");
		}

		@Override
		public void render(MatrixStack st, int mouseX, int mouseY, float partialTicks) {
			GuiImpl.this.render(st, Integer.MIN_VALUE, Integer.MIN_VALUE, partialTicks);
			super.render(st, mouseX, mouseY, partialTicks);
		}

		public Screen getGui() {
			return GuiImpl.this;
		}
	}

	@Override
	public KeyCodes getKeyCodes() {
		return CODES;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void drawGradientBox(int x, int y, int w, int h, int topLeft, int topRight, int bottomLeft,
			int bottomRight) {
		x += getOffset().x;
		y += getOffset().y;
		int left = x;
		int top = y;
		int right = x + w;
		int bottom = y + h;
		float atr = (topRight >> 24 & 255) / 255.0F;
		float rtr = (topRight >> 16 & 255) / 255.0F;
		float gtr = (topRight >> 8 & 255) / 255.0F;
		float btr = (topRight & 255) / 255.0F;
		float atl = (topLeft >> 24 & 255) / 255.0F;
		float rtl = (topLeft >> 16 & 255) / 255.0F;
		float gtl = (topLeft >> 8 & 255) / 255.0F;
		float btl = (topLeft & 255) / 255.0F;
		float abl = (bottomLeft >> 24 & 255) / 255.0F;
		float rbl = (bottomLeft >> 16 & 255) / 255.0F;
		float gbl = (bottomLeft >> 8 & 255) / 255.0F;
		float bbl = (bottomLeft & 255) / 255.0F;
		float abr = (bottomRight >> 24 & 255) / 255.0F;
		float rbr = (bottomRight >> 16 & 255) / 255.0F;
		float gbr = (bottomRight >> 8 & 255) / 255.0F;
		float bbr = (bottomRight & 255) / 255.0F;
		RenderSystem.disableTexture();
		RenderSystem.enableBlend();
		RenderSystem.disableAlphaTest();
		RenderSystem.defaultBlendFunc();
		RenderSystem.shadeModel(7425);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuffer();
		Matrix4f mat = matrixStack.getLast().getMatrix();
		bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
		bufferbuilder.pos(mat, right, top, this.getBlitOffset()).color(rtr, gtr, btr, atr).endVertex();
		bufferbuilder.pos(mat, left, top, this.getBlitOffset()).color(rtl, gtl, btl, atl).endVertex();
		bufferbuilder.pos(mat, left, bottom, this.getBlitOffset()).color(rbl, gbl, bbl, abl).endVertex();
		bufferbuilder.pos(mat, right, bottom, this.getBlitOffset()).color(rbr, gbr, bbr, abr).endVertex();
		tessellator.draw();
		RenderSystem.shadeModel(7424);
		RenderSystem.disableBlend();
		RenderSystem.enableAlphaTest();
		RenderSystem.enableTexture();
	}

	@Override
	public NativeGuiComponents getNative() {
		return nativeComponents;
	}

	@Override
	public void setClipboardText(String text) {
		minecraft.keyboardListener.setClipboardString(text);
	}

	@Override
	public Frame getFrame() {
		return gui;
	}

	@Override
	public String getClipboardText() {
		return minecraft.keyboardListener.getClipboardString();
	}

	@Override
	public void setScale(int value) {
		if(value != minecraft.gameSettings.guiScale) {
			if(vanillaScale == -1)vanillaScale = minecraft.gameSettings.guiScale;
			if(value == -1) {
				if(minecraft.gameSettings.guiScale != vanillaScale) {
					minecraft.gameSettings.guiScale = vanillaScale;
					vanillaScale = -1;
					minecraft.updateWindowSize();
				}
			} else {
				minecraft.gameSettings.guiScale = value;
				minecraft.updateWindowSize();
			}
		}
	}

	@Override
	public int getScale() {
		return minecraft.gameSettings.guiScale;
	}

	@Override
	public int getMaxScale() {
		return minecraft.getMainWindow().calcGuiScale(0, minecraft.getForceUnicodeFont()) + 1;
	}

	@Override
	public CtxStack getStack() {
		return stack;
	}

	@Override
	public void tick() {
		try {
			gui.tick();
		} catch (Throwable e) {
			onGuiException("Error in tick gui", e, true);
		}
	}
}
