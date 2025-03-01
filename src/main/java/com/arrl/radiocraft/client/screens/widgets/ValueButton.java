package com.arrl.radiocraft.client.screens.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

/**
 * Very similar to ToggleButton but the "on" state is determined by a value which can be changed elsewhere.
 */
public class ValueButton extends AbstractWidget {

	private final ResourceLocation resourceLocation;
	private final int u;
	private final int v;
	private final int textureWidth;
	private final int textureHeight;
	private final Supplier<Boolean> valueSupplier;
	private final OnInteract onPress;

	public boolean lastState = false;

	public ValueButton(int x, int y, int width, int height, int u, int v, ResourceLocation texLocation, int texWidth, int texHeight, Supplier<Boolean> valueSupplier, OnInteract onPress) {
		super(x, y, width, height, CommonComponents.EMPTY);
		this.textureWidth = texWidth;
		this.textureHeight = texHeight;
		this.u = u;
		this.v = v;
		this.resourceLocation = texLocation;
		this.valueSupplier = valueSupplier;
		this.onPress = onPress;
	}

	@Override
	protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
		boolean value = valueSupplier.get();
		int xBlit = !isHovered() ? u : u + width;
		int yBlit = !value ? v : v + height;
		pGuiGraphics.blit(this.resourceLocation, this.getX(), this.getY(), xBlit, yBlit, width, height, textureWidth, textureHeight);
		if(lastState != value)
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)); // Will play toggle click by itself when the value changes externally.
		lastState = value;
	}

	@Override
	public void onClick(double x, double y, int button) {
		super.onClick(x, y, button);
		if (button == GLFW_MOUSE_BUTTON_LEFT) {
			onPress.execute(this);
		}
	}

	@Override
	protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
		this.defaultButtonNarrationText(narrationElementOutput);
	}

	@Override
	public void playDownSound(@NotNull SoundManager handler) {} // Empty override so the sound doesn't get replayed by the auto handling in render.

	@FunctionalInterface
	public interface OnInteract {
		void execute(ValueButton button);
	}

}