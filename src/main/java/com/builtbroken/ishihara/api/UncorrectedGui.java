package com.builtbroken.ishihara.api;

/**
 * Goes on {@link net.minecraft.client.gui.GuiScreen}.
 *
 * @author Wyn Price
 */
public interface UncorrectedGui
{
    /**
     * Gets once per frame after {@link net.minecraft.client.gui.GuiScreen#drawScreen(int, int, float)}.
     * Anything rendered on this will be excluded from the color correcting
     */
    void drawUncoloredScreen(int mouseX, int mouseY, float partialTicks);
}
