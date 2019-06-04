package com.builtbroken.ishihara;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.shader.*;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.*;
import java.util.function.BiConsumer;

/**
 * @author Wyn Price
 */
@Mod(modid = Ishihara.MODID, name = Ishihara.NAME, version = Ishihara.VERSION, clientSideOnly = true)
@Mod.EventBusSubscriber
public class Ishihara {
    public static final String MODID = "ishihara";
    public static final String NAME = "Ishihara";
    public static final String VERSION = "1.0";
    public static Logger logger = LogManager.getLogger(MODID);

    private static final Minecraft mc = Minecraft.getMinecraft();
    public static KeyBinding guiBinding;
    public static File dir = new File(mc.gameDir, "colorblind.json");

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        guiBinding = new KeyBinding("key." + MODID + ".desc", -1, MODID);
        guiBinding.setKeyModifierAndCode(KeyModifier.CONTROL, Keyboard.KEY_N);
        ClientRegistry.registerKeyBinding(guiBinding);
    }

    /**
     * Loads all the deficiencies in the json file, and passes them to {@code cons}
     * @param cons The consumer to take in the deficiency name and matrix values.
     */
    public static void getDeficiencies(BiConsumer<String, float[]> cons) {
        //Check that the file exists, and if not try and create a new file
        if (!dir.exists()) {
            try {
                if(dir.createNewFile()) {
                    writeDefaultValues();
                } else {
                    throw new FileNotFoundException("Unable to create " + dir.getAbsolutePath() + " as a file");
                }
            } catch (IOException e) {
                logger.error("Unable to create new file", e);
            }
        }

        //Consume the identity deficiency. This is the only one that isn't in the json file
        cons.accept("identity", new float[]{1,0,0,0,1,0,0,0,1});

        JsonObject parsed;
        try {
            JsonElement parse = new JsonParser().parse(new InputStreamReader(new FileInputStream(dir)));
            if(parse.isJsonNull()) {
                Ishihara.writeDefaultValues();
                parse = new JsonParser().parse(new InputStreamReader(new FileInputStream(dir)));
            }
            if(parse.isJsonNull()) {
                throw new IllegalArgumentException("Error loading json. If this problem persists please delete the colorblind.json");
            }
            parsed = parse.getAsJsonObject();
        } catch (IOException e) {
            throw new RuntimeException("Unable to parse json file, " + dir.getAbsolutePath(), e);
        }

        //Get the "values" array in the json file, and iterate through it. For each element get the matrix array and
        //put the values into a float[9], then have the consumer consume the "name" property and the matrix values.
        JsonArray values = JsonUtils.getJsonArray(parsed, "values");
        for (JsonElement value : values) {
            if(value.isJsonObject()) {
                JsonObject obj = value.getAsJsonObject();
                JsonArray matrix = obj.getAsJsonArray("matrix");
                float[] afloat = new float[9];
                for (int i = 0; i < 9; i++) {
                    afloat[i] = matrix.get(i).getAsFloat();
                }
                cons.accept(obj.get("name").getAsString(), afloat);
            }
        }

    }

    /**
     * Writes the default values of the json file.
     */
    private static void writeDefaultValues() {
        logger.info("Writing colorblind.json file");
        JsonArray arr = new JsonArray();

        JsonObject protanopia = new JsonObject();
        protanopia.addProperty("name", "protanopia");
        protanopia.add("matrix", convertToArray(0.0, 1.05118294, -0.05116099, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0));
        arr.add(protanopia);

        JsonObject deuteranopia = new JsonObject();
        deuteranopia.addProperty("name", "deuteranopia");
        deuteranopia.add("matrix", convertToArray(1.0, 0.0, 0.0, 0.9513092, 0.0, 0.04866992, 0.0, 0.0, 1.0));
        arr.add(deuteranopia);

        JsonObject tritanopes = new JsonObject();
        tritanopes.addProperty("name", "tritanopes");
        tritanopes.add("matrix", convertToArray(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, -0.86744736, 1.86727089, 0.0));
        arr.add(tritanopes);

        JsonObject coneMonochromats = new JsonObject();
        coneMonochromats.addProperty("name", "cone_monochromats");
        coneMonochromats.add("matrix", convertToArray(0.01775, 0.10945, 0.87262, 0.01775, 0.10945, 0.87262, 0.01775, 0.10945, 0.87262));
        arr.add(coneMonochromats);

        JsonObject rodMonochromats = new JsonObject();
        rodMonochromats.addProperty("name", "rod_monochromats");
        rodMonochromats.add("matrix", convertToArray(0.212656, 0.715158, 0.072186, 0.212656, 0.715158, 0.072186, 0.212656, 0.715158, 0.072186));
        arr.add(rodMonochromats);

        JsonObject greenConeMonochromat = new JsonObject();
        greenConeMonochromat.addProperty("name", "green_cone_monochromat");
        greenConeMonochromat.add("matrix", convertToArray(0.15537, 0.75792, 0.08670, 0.15537, 0.75792, 0.08670, 0.15537, 0.75792, 0.08670));
        arr.add(greenConeMonochromat);

        JsonObject obj = new JsonObject();
        obj.addProperty("__comment", "matrix values are taken and calculated from from https://ixora.io/projects/colorblindness/color-blindness-simulation-research/");
        obj.add("values", arr);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try(FileWriter fw = new FileWriter(dir)) {
            gson.toJson(obj, fw);
        } catch (IOException e) {
            logger.error("Error writing to file " + dir.getAbsolutePath(),e);
        }
    }


    /**
     * Converts an array of doubles to a JsonArray
     * @param doubles the doubles to be converted
     * @return A JsonArray composing of the input doubles.
     */
    private static JsonArray convertToArray(double... doubles) {
        JsonArray arr = new JsonArray();
        for (double d : doubles) {
            arr.add(d);
        }
        return arr;
    }

    /**
     * A copy of {@link Minecraft#displayGuiScreen(GuiScreen)}, without the modifying of the previous gui screen <br>
     * Allows for a seemless transition between guis.
     * @param initlize should the new gui screen be initilized
     * @param guiScreenIn the screen to display
     */
    public static void setScreenQuietly(boolean initlize, GuiScreen guiScreenIn) {

        if (guiScreenIn == null && mc.world == null)
        {
            guiScreenIn = new GuiMainMenu();
        }
        else if (guiScreenIn == null && mc.player.getHealth() <= 0.0F)
        {
            guiScreenIn = new GuiGameOver((ITextComponent)null);
        }

        if (guiScreenIn instanceof GuiMainMenu || guiScreenIn instanceof GuiMultiplayer)
        {
            mc.gameSettings.showDebugInfo = false;
            mc.ingameGUI.getChatGUI().clearChatMessages(true);
        }

        mc.currentScreen = guiScreenIn;

        if (guiScreenIn != null)
        {
            if(initlize) {
                mc.setIngameNotInFocus();
                KeyBinding.unPressAllKeys();

                while (Mouse.next())
                {
                    ;
                }

                while (Keyboard.next())
                {
                    ;
                }

                ScaledResolution scaledresolution = new ScaledResolution(mc);
                int i = scaledresolution.getScaledWidth();
                int j = scaledresolution.getScaledHeight();
                guiScreenIn.setWorldAndResolution(mc, i, j);
                mc.skipRenderWorld = false;
            }
        }
        else
        {
            mc.getSoundHandler().resumeSounds();
            mc.setIngameFocus();
        }
    }

    @SubscribeEvent
    public static void keyPressed(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if(Keyboard.isKeyDown(guiBinding.getKeyCode()) && guiBinding.getKeyConflictContext().isActive() && guiBinding.getKeyModifier().isActive(guiBinding.getKeyConflictContext()) && !(mc.currentScreen instanceof IshiharaGui)) {
            setScreenQuietly(true, new IshiharaGui());
        }
    }

    @SubscribeEvent
    public static void keyPressed(InputEvent.KeyInputEvent event) {
        if(guiBinding.isPressed() && !(mc.currentScreen instanceof IshiharaGui)) {
            setScreenQuietly(true, new IshiharaGui());
        }
    }
}
