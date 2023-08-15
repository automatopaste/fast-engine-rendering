package data.scripts.plugins;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import data.scripts.shaders.fer_EnginePlugin;

import java.util.List;

public class fer_CombatEffectsPlugin extends BaseEveryFrameCombatPlugin {
    @Override
    public void init(CombatEngineAPI engine) {
        engine.addPlugin(new fer_EnginePlugin());
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        //did some testing stuff here
    }
}