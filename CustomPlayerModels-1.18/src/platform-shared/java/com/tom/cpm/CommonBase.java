package com.tom.cpm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.KeybindComponent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;

import com.tom.cpl.config.ModConfigFile;
import com.tom.cpl.text.TextRemapper;
import com.tom.cpl.text.TextStyle;
import com.tom.cpl.util.ILogger;
import com.tom.cpm.api.CPMApiManager;
import com.tom.cpm.shared.MinecraftCommonAccess;

public abstract class CommonBase implements MinecraftCommonAccess {
	public static final Logger LOG = LogManager.getLogger("CPM");
	public static final ILogger log = new Log4JLogger(LOG);

	public static CPMApiManager api;
	protected ModConfigFile cfg;

	public CommonBase() {
		api = new CPMApiManager();
	}

	protected void apiInit() {
		LOG.info("Customizable Player Models API initialized: " + api.getPluginStatus());
		api.buildCommon().player(Player.class).init();
	}

	@Override
	public ModConfigFile getConfig() {
		return cfg;
	}

	@Override
	public ILogger getLogger() {
		return log;
	}

	@Override
	public TextRemapper<MutableComponent> getTextRemapper() {
		return new TextRemapper<>(TranslatableComponent::new, TextComponent::new, MutableComponent::append, KeybindComponent::new,
				CommonBase::styleText);
	}

	private static MutableComponent styleText(MutableComponent in, TextStyle style) {
		return in.withStyle(Style.EMPTY.withBold(style.bold).withItalic(style.italic).withUnderlined(style.underline).withStrikethrough(style.strikethrough));
	}

	@Override
	public CPMApiManager getApi() {
		return api;
	}

	@Override
	public String getMCVersion() {
		return SharedConstants.getCurrentVersion().getName();
	}
}
