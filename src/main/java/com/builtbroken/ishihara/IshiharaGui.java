package com.builtbroken.ishihara;

import com.builtbroken.ishihara.api.UncorrectedGui;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

/**
 * The overlay gui for Ishihara. Used to render the color wheels and names of the deficiencies.
 *
 * @author Wyn Price
 */
public class IshiharaGui extends GuiScreen implements UncorrectedGui
{

    /**
     * The number of entries per page
     */
    private static final int PER_PAGE = 7;

    /**
     * The shader manager used to hold the color wheel shader
     */
    private static ShaderManager shaderManager;

    /**
     * The list of deficiencies to display and choose from
     */
    private final List<Deficiency> deficiencies = Lists.newArrayList();

    /**
     * The parent gui, that this gui was opened from. Can be null if it were from in game.
     */
    @Nullable
    private final GuiScreen parent;

    /**
     * Used to keep track of if this is the first time {@link #initGui()} has been called. <br>
     * If it is not the first time, then the screen has been changed and therefore the parent screen should also be changed.
     */
    private boolean firstRun = true;

    private GuiButton nextPage;
    private GuiButton backPage;

    /**
     * The current page that the gui is on
     */
    private int page;

    public IshiharaGui()
    {
        this.parent = Minecraft.getMinecraft().currentScreen;
        //Setup the shader manager
        if (shaderManager == null)
        {
            try
            {
                shaderManager = new ShaderManager(Minecraft.getMinecraft().getResourceManager(), Ishihara.MODID + ":colorwheel");
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        //Get all the deficiencies
        Ishihara.getDeficiencies((name, matrix) -> this.deficiencies.add(new Deficiency(this.deficiencies.size(), name, matrix)));
    }

    @Override
    public void initGui()
    {
        super.initGui();

        //If this isnt the first run, and the parent isn't null, meaning that the screen has changed and needs refreshing.
        if (!this.firstRun && this.parent != null)
        {
            this.parent.initGui();
        }
        this.firstRun = false;
        this.addButton(new GuiButton(5, this.width / 4 - 50, this.height - 40, 100, 20, I18n.format("gui.back")));

        //Only show the next/back page buttons if there is more than one page
        if (this.deficiencies.size() / PER_PAGE > 1)
        {
            int width = MathHelper.clamp(IshiharaGui.this.width / 4, 25, 75);
            int centerX = (int) (11 / 16F * IshiharaGui.this.width);

            this.nextPage = this.addButton(new GuiButton(6, centerX + width + 40 - 10, this.height / 2 - 10, 20, 20, ">"));
            this.backPage = this.addButton(new GuiButton(7, centerX - width - 40 - 10, this.height / 2 - 10, 20, 20, "<"));
            this.backPage.enabled = false;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);

        //Render the deficiencies. Renders everything except the color wheel
        for (Deficiency deficiency : this.deficiencies)
        {
            deficiency.render(false, mouseX, mouseY);
        }

        int centerX = this.width / 4;
        int centerY = this.height / 2;

        int radii = Math.min(centerX, centerY) / 2;

        //Render the large color wheel on the left of the screen
        if (shaderManager != null)
        {
            shaderManager.useShader();
            BufferBuilder buff = Tessellator.getInstance().getBuffer();

            buff.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

            buff.pos(centerX - radii, centerY - radii, 0).tex(0, 0).endVertex();
            buff.pos(centerX - radii, centerY + radii, 0).tex(0, 1).endVertex();
            buff.pos(centerX + radii, centerY + radii, 0).tex(1, 1).endVertex();
            buff.pos(centerX + radii, centerY - radii, 0).tex(1, 0).endVertex();

            Tessellator.getInstance().draw();
            shaderManager.endShader();
        }
    }

    /**
     * Gets once per frame after {@link net.minecraft.client.gui.GuiScreen#drawScreen(int, int, float)}.
     * Anything rendered on this will be excluded from the color correcting
     */
    @Override
    public void drawUncoloredScreen(int mouseX, int mouseY, float partialTicks)
    {
        //Render all the color wheels on the deficiency entries
        for (Deficiency deficiency : this.deficiencies)
        {
            deficiency.render(true, mouseX, mouseY);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        //If the escape key has been pressed instead of doing the close that would run `initScreen`
        //on the parent screen, just quietly display the parent screen. Means anything typed or
        //selected stays the same. Makes this screen act like an overlay.
        if (keyCode == Keyboard.KEY_ESCAPE)
        {
            Ishihara.setScreenQuietly(false, this.parent);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException
    {
        //exit button
        if (button.id == 5)
        {
            Ishihara.setScreenQuietly(false, IshiharaGui.this.parent);

        }
        //next page button
        if (button.id == 6)
        {
            this.page = Math.min(this.page + 1, this.deficiencies.size() / PER_PAGE);
            if (this.page == this.deficiencies.size() / PER_PAGE)
            {
                button.enabled = false;
            }
            this.backPage.enabled = true;
        }
        //previous page button
        if (button.id == 7)
        {
            this.page = Math.max(this.page - 1, 0);
            if (this.page == 0)
            {
                button.enabled = false;
            }
            this.nextPage.enabled = true;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        //Go through the deficiency. If the mouse is over it (deficiency.selected), then set that as the active deficiency
        for (Deficiency deficiency : this.deficiencies)
        {
            if (deficiency.selected)
            {
                IshiharaRenderer.matrix.put(deficiency.matrix);
                break;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /**
     * Contains information about a deficiency.
     */
    public class Deficiency
    {
        /**
         * The index of which this deficiency is at in {@link IshiharaGui#deficiencies}'s list
         */
        private final int index;

        /**
         * The name of this deficiency, as defined in the json file
         */
        private final String name;

        /**
         * The matrix of this deficiency, as defined in the json file
         */
        private final float[] matrix;

        /**
         * Whether the mouse is over this entry or not. Used to determine what deficiency has been clicked.
         */
        private boolean selected;

        public Deficiency(int index, String name, float[] matrix)
        {
            this.index = index;
            this.name = name;
            this.matrix = matrix;
        }

        /**
         * Renders the entry
         *
         * @param wheel  If true, then the wheel is rendered. If false then the background + text is rendered. <br>
         *               Used to render the wheel with color correction off, and the rest with color correction on.
         * @param mouseX The mouse x
         * @param mouseY The mouse y
         */
        private void render(boolean wheel, int mouseX, int mouseY)
        {
            //If it is in the current page. Takes advantage of integer division
            if (this.index / IshiharaGui.PER_PAGE == IshiharaGui.this.page)
            {
                int centerX = (int) (11 / 16F * IshiharaGui.this.width);
                int hWidth = MathHelper.clamp(IshiharaGui.this.width / 4, 25, 100);
                int height = 30;
                //Total deficiencies to show on this page
                int total = Math.min(IshiharaGui.this.deficiencies.size() - IshiharaGui.PER_PAGE * IshiharaGui.this.page, IshiharaGui.PER_PAGE);
                //The y start of this entry
                int start = (IshiharaGui.this.height / 2) - (total * height) / 2 + (this.index % IshiharaGui.PER_PAGE) * height;

                //If we should draw the wheel only. Used for uncorrected rendering
                if (wheel)
                {
                    int radii = 10;
                    int wheelx = centerX - hWidth + height / 2;
                    int wheely = start + height / 2;

                    //Set the shader to use the deficiency and set the deficiencies matrix values.
                    shaderManager.getShaderUniformOrDefault("UseDeficiency").set(1);
                    shaderManager.getShaderUniformOrDefault("Deficiency0").set(this.matrix[0], this.matrix[1], this.matrix[2]);
                    shaderManager.getShaderUniformOrDefault("Deficiency1").set(this.matrix[3], this.matrix[4], this.matrix[5]);
                    shaderManager.getShaderUniformOrDefault("Deficiency2").set(this.matrix[6], this.matrix[7], this.matrix[8]);
                    shaderManager.useShader();
                    BufferBuilder buff = Tessellator.getInstance().getBuffer();
                    buff.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
                    buff.pos(wheelx - radii, wheely - radii, 0).tex(0, 0).endVertex();
                    buff.pos(wheelx - radii, wheely + radii, 0).tex(0, 1).endVertex();
                    buff.pos(wheelx + radii, wheely + radii, 0).tex(1, 1).endVertex();
                    buff.pos(wheelx + radii, wheely - radii, 0).tex(1, 0).endVertex();
                    Tessellator.getInstance().draw();
                    shaderManager.getShaderUniformOrDefault("UseDeficiency").set(0);
                    shaderManager.endShader();
                }
                else
                {
                    //Draw the text + background color.
                    int color = 0xFFAAAAAA;
                    if (mouseX > centerX - hWidth && mouseY > start && mouseX < centerX + hWidth && mouseY < start + height)
                    {
                        color = 0xFFAAAAEE;
                        this.selected = true;
                    }
                    else
                    {
                        this.selected = false;
                    }

                    Gui.drawRect(centerX - hWidth, start, centerX + hWidth, start + height, color);
                    //top
                    Gui.drawRect(centerX - hWidth, start - 1, centerX + hWidth, start + 1, 0xFF555555);
                    //bottom
                    Gui.drawRect(centerX - hWidth, start + height - 1, centerX + hWidth, start + height + 1, 0xFF555555);
                    //left
                    Gui.drawRect(centerX - hWidth - 1, start - 1, centerX - hWidth + 1, start + height + 1, 0xFF555555);
                    //right
                    Gui.drawRect(centerX + hWidth - 1, start - 1, centerX + hWidth + 1, start + height + 1, 0xFF555555);

                    int wheely = start + height / 2;

                    //Only translate the name if there is a key for it.
                    String text = Ishihara.MODID + "." + this.name;
                    if (I18n.hasKey(text))
                    {
                        text = I18n.format(text);
                    }
                    else
                    {
                        text = this.name;
                    }
                    mc.fontRenderer.drawString(text, centerX - (int) (mc.fontRenderer.getStringWidth(text) / 2F - height / 2F), wheely - 3, 0xFF676767, false);
                }
            }
        }
    }
}
