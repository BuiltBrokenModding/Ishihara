package com.builtbroken.ishihara;

import com.builtbroken.ishihara.api.UncorrectedGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.io.IOException;

/**
 * The class used for rendering the shader
 *
 * @author Wyn Price
 */
@Mod.EventBusSubscriber(modid = Ishihara.MODID)
public class IshiharaRenderer
{

    private static final Minecraft mc = Minecraft.getMinecraft();

    /**
     * The shader group containing the main shader.
     */
    private static ShaderGroup shaderGroup;

    /**
     * Used to keep track of if the screen has changed size, as if it has then the framebuffer should be re-initialized.
     */
    private static int previousWidth, previousHeight;

    /**
     * The matrix that holds the current values
     */
    public static GroupedMatrix matrix;

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            if (shaderGroup == null)
            {
                //This works as the registering a reload listener makes that listener get called straight away
                ((IReloadableResourceManager) mc.getResourceManager()).registerReloadListener(IshiharaRenderer::loadShader);
            }
            int width = mc.displayWidth;
            int height = mc.displayHeight;

            //If the width or height has changed, the recreate the framebuffer
            if (width != previousWidth || height != previousHeight)
            {
                shaderGroup.createBindFramebuffers(width, height);
                previousWidth = width;
                previousHeight = height;
            }

            //Do the actual rendering
            shaderGroup.render(event.renderTickTime);

            //Bind the main framebuffer back
            mc.getFramebuffer().bindFramebuffer(true);
            //Used as the shader rendering isn't very good at cleaning up after itself
            GlStateManager.enableAlpha();

            //If the gui is an instance of UncorrectedGui, then render the gui.
            //as this is done after the color correcting rendering, the stuff
            //rendered here is not affected by the color correcting.
            if (mc.currentScreen instanceof UncorrectedGui)
            {
                ScaledResolution res = new ScaledResolution(mc);
                int h = res.getScaledHeight();
                ((UncorrectedGui) mc.currentScreen).drawUncoloredScreen(Mouse.getX() * res.getScaledWidth() / mc.displayWidth, h - Mouse.getY() * h / mc.displayHeight - 1, event.renderTickTime);
            }

        }
    }

    /**
     * Load the shader from the files
     *
     * @param resourceManager the manager of which to get the files.
     */
    public static void loadShader(IResourceManager resourceManager)
    {
        try
        {
            previousWidth = previousHeight = -1;
            shaderGroup = new ShaderGroup(mc.getTextureManager(), resourceManager, mc.getFramebuffer(), new ResourceLocation(Ishihara.MODID, "shaders/post/ishihara.json"));

            //We need to get the uniforms of our ishihara shader, as to allow us to edit them.
            for (Shader shader : shaderGroup.listShaders)
            {
                ShaderManager manager = shader.getShaderManager();
                if (manager.programFilename.equals("ishihara:ishihara"))
                {
                    matrix = new GroupedMatrix(manager.getShaderUniform("Deficiency0"), manager.getShaderUniform("Deficiency1"), manager.getShaderUniform("Deficiency2"));
                    matrix.put(1.0F, 0.0F, 0.0F,
                            0F, 1.0F, 0F,
                            0.0F, 0.0F, 1.0F);

                }
            }
        }
        catch (IOException e)
        {
            Ishihara.logger.error("Error loading shader", e);
        }
    }

    /**
     * Used to work around the issue that Minecraft's ShaderManager doesn't allow for 3x3 matrices <br>
     * Splits the matrix uniform up into 3 vec3s. Top, middle and base.
     */
    public static class GroupedMatrix
    {
        private final ShaderUniform top, middle, base;

        public GroupedMatrix(ShaderUniform top, ShaderUniform middle, ShaderUniform base)
        {
            this.top = top;
            this.middle = middle;
            this.base = base;
        }

        public void put(float... afloat)
        {
            this.top.set(afloat[0], afloat[1], afloat[2]);
            this.middle.set(afloat[3], afloat[4], afloat[5]);
            this.base.set(afloat[6], afloat[7], afloat[8]);
        }
    }

}
