package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class fer_ModPlugin extends BaseModPlugin {
    public static final String MOD_AUTHOR = "tomatopaste";
    public static final String MOD_ID = "fast_engine_rendering";

    public static boolean RENDER_OVER_WEAPONS = Global.getSettings().getBoolean("fer_RenderOverWeapons");
    public static boolean USE_GLOW_SHADER = Global.getSettings().getBoolean("fer_UseSecondGlowShader");
    public static boolean FORCE_OVERRIDE_STYLES = Global.getSettings().getBoolean("fer_ForceOverrideAllStyles");

    public static List<String> INCLUDED_ENGINE_STYLES = new ArrayList<>();

    @Override
    public void onApplicationLoad() throws ClassNotFoundException {
        try {
            Global.getSettings().getScriptClassLoader().loadClass("org.lazywizard.lazylib.ModUtils");
        } catch (ClassNotFoundException ex) {
            String message = System.lineSeparator()
                    + System.lineSeparator() + "LazyLib is required to run at least one of the mods you have installed."
                    + System.lineSeparator() + System.lineSeparator()
                    + "You can download LazyLib at http://fractalsoftworks.com/forum/index.php?topic=5444"
                    + System.lineSeparator();
            throw new ClassNotFoundException(message);
        }

        if (RENDER_OVER_WEAPONS) throw new NullPointerException("To stop this crash from occuring disable engines rendering over weapons. (blame the mafia)");

        loadData();
    }

    @Override
    public void onDevModeF8Reload() {
        loadData();
    }

    private void loadData() {
        INCLUDED_ENGINE_STYLES.clear();

        try {
            JSONArray data = Global.getSettings().getMergedSpreadsheetDataForMod("engine_style_id", "data/config/fastenginerendering/included_engine_styles.csv", MOD_ID);
            for (int i = 0; i < data.length(); i++) {
                JSONObject row = data.getJSONObject(i);
                String style = row.getString("engine_style_id");
                if (style != null && !style.startsWith("#")) INCLUDED_ENGINE_STYLES.add(style);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }
}