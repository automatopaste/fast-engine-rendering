package data.scripts.plugins;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.shaders.fer_EngineFlareShader;

import java.util.List;

public class fer_CombatEffectsPlugin extends BaseEveryFrameCombatPlugin {
    @Override
    public void init(CombatEngineAPI engine) {
        engine.addLayeredRenderingPlugin(new fer_EngineFlareShader());
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        //did some testing stuff here
    }
}