package data.scripts.shaders;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.coreui.S;
import data.scripts.fer_ModPlugin;
import data.scripts.shaders.util.fer_ShaderProgram;
import data.scripts.shaders.util.fer_ShaderRendererInstanced;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.ListIterator;

public class fer_EngineFlareShader implements CombatLayeredRenderingPlugin {
    private static final String DATA_KEY = "we_do_a_little_trolling";

    private final boolean enabled;

    private static final List<String> ignoreShips = new ArrayList<>();
    static {
            ignoreShips.add("buffalo");
            ignoreShips.add("hound");
    };

    private fer_ShaderProgram program;
    private fer_ShaderProgram glowProgram;

    public fer_EngineFlareShader() {
        enabled = true;

        String vert;
        String frag;
        this.program = new fer_ShaderProgram();
        try {
            vert = Global.getSettings().loadText("data/shaders/engineflare.vert");
            frag = Global.getSettings().loadText("data/shaders/engineflare.frag");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        program.createVertexShader(vert);
        program.createFragmentShader(frag);
        program.link();

        String vertGlow;
        String fragGlow;
        this.glowProgram = new fer_ShaderProgram();
        try {
            vertGlow = Global.getSettings().loadText("data/shaders/glow.vert");
            fragGlow = Global.getSettings().loadText("data/shaders/glow.frag");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        glowProgram.createVertexShader(vertGlow);
        glowProgram.createFragmentShader(fragGlow);
        glowProgram.link();
    }

    public static void addFlare(ShipEngineControllerAPI.ShipEngineAPI engine, fer_EngineFlareAPI flare, CombatEntityAPI entity) {
        List<FlareData> data = (List<FlareData>) Global.getCombatEngine().getCustomData().get(DATA_KEY);
        if (data != null) {
            data.add(new FlareData(engine, flare, entity));
            Global.getCombatEngine().getCustomData().put(DATA_KEY, data);
        }
    }

    public static void removeFlare(FlareData flare) {
        List<FlareData> data = (List<FlareData>) Global.getCombatEngine().getCustomData().get(DATA_KEY);
        if (data != null) {
            data.remove(flare);
            Global.getCombatEngine().getCustomData().put(DATA_KEY, data);
        }
    }

    public static fer_EngineFlareAPI getFlare(ShipEngineControllerAPI.ShipEngineAPI engine) {
        List<FlareData> data = (List<FlareData>) Global.getCombatEngine().getCustomData().get(DATA_KEY);
        if (data != null) for (FlareData flareData : data) if (flareData.engine != null && flareData.engine.equals(engine)) return flareData.flare;
        return null;
    }

    public static fer_EngineFlareAPI getFlare(MissileAPI missile) {
        List<FlareData> data = (List<FlareData>) Global.getCombatEngine().getCustomData().get(DATA_KEY);
        if (data != null) for (FlareData flareData : data) if (flareData.entity.equals(missile)) return flareData.flare;
        return null;
    }

    private void drawFlares() {
        List<FlareData> data = (List<FlareData>) Global.getCombatEngine().getCustomData().get(DATA_KEY);
        if (data == null) return;

        ViewportAPI viewport = Global.getCombatEngine().getViewport();
        Matrix4f projection = fer_ShaderRendererInstanced.orthogonal(viewport.getVisibleWidth() / viewport.getViewMult(), viewport.getVisibleHeight() / viewport.getViewMult());

        //instanced render
        fer_ShaderRendererInstanced renderer = new fer_ShaderRendererInstanced(program, data.size());
        renderer.renderInstanced(viewport, data, projection, false);
        renderer.dispose();

        if (fer_ModPlugin.USE_GLOW_SHADER) {
            fer_ShaderRendererInstanced glowRenderer = new fer_ShaderRendererInstanced(glowProgram, data.size());
            glowRenderer.renderInstanced(viewport, data, projection, true);
            glowRenderer.dispose();
        }
    }

    @Override
    public void init(CombatEntityAPI entity) {
        Global.getCombatEngine().getCustomData().put(DATA_KEY, new ArrayList<>());
    }

    @Override
    public void cleanup() {
        CombatEngineAPI engine = Global.getCombatEngine();

        List<FlareData> data = (List<FlareData>) engine.getCustomData().get(DATA_KEY);
        if (data == null) return;

        for (FlareData flareData : data) {
            flareData.flare.dispose();
        }

        program.dispose();
        glowProgram.dispose();

        engine.getCustomData().put(DATA_KEY, new ArrayList<>());
    }

    @Override
    public boolean isExpired() {
        return false;
    }

    private final List<ShipAPI> ships = new ArrayList<>();
    //private final List<MissileAPI> missiles = new ArrayList<>();

    @Override
    public void advance(float amount) {
        if (!enabled) return;

        CombatEngineAPI engine = Global.getCombatEngine();

        List<FlareData> data = (List<FlareData>) engine.getCustomData().get(DATA_KEY);
        if (data == null) return;

        if (!engine.isPaused()) {
            ListIterator<FlareData> iterator = data.listIterator();
            while (iterator.hasNext()) {
                FlareData next = iterator.next();
                if (!engine.isEntityInPlay(next.entity) || (next.entity instanceof ShipAPI && !((ShipAPI) next.entity).isAlive())) {
                    next.flare.dispose();
                    iterator.remove();
                }
            }
        }

        if (!engine.isPaused()) {
            for (ShipAPI ship : engine.getShips()) {
                if (ship.getHullStyleId().equals("OMEGA")) continue;

                boolean isStored = ships.contains(ship);

                ship.setRenderEngines(false);

                // dont like doing this but engine colour fading is not accessible through api
                Color priorityColour = null;
                if (ship.getSystem() != null && ship.getSystem().getId().equals("plasmajets") && ship.getSystem().isOn()) {
                    priorityColour = new Color(100,255,100,255);
                } else if (ship.getSystem() != null && ship.getSystem().getId().equals("plasmaburn") && ship.getSystem().isOn()) {
                    priorityColour = new Color(100,255,100,255);
                } else if (ship.getVariant().hasHullMod("safetyoverrides")) {
                    priorityColour = new Color(255,100,255,255);
                }

                for (ShipEngineControllerAPI.ShipEngineAPI controller : ship.getEngineController().getShipEngines()) {
                    Vector2f pos = controller.getEngineSlot().computePosition(ship.getLocation(), ship.getFacing());
                    float angle = controller.getEngineSlot().getAngle();
                    Vector2f size = new Vector2f(
                            controller.getEngineSlot().getLength() * (ship.getEngineController().getExtendLengthFraction().getCurr() + 1),
                            controller.getEngineSlot().getWidth() * (ship.getEngineController().getExtendWidthFraction().getCurr() + 1)
                    );

                    Color color = controller.getEngineColor();
                    if (priorityColour != null) {
                        color = blend(color, priorityColour, 0.7f);
                    }

                    if (isStored) {
                        fer_EngineFlareAPI flare = fer_EngineFlareShader.getFlare(controller);
                        if (flare != null) {
                            flare.setAngle(angle + ship.getFacing());
                            flare.setSize(size);
                            flare.setLocation(pos);
                            flare.setColor(color);

                            float targetL = 0.5f, targetW = 0.6f, targetG = 0.6f;

                            if (ship.getEngineController().isStrafingLeft() || ship.getEngineController().isStrafingRight()) {
                                targetW = 1.1f;
                                targetL = 0.5f;
                                targetG = 1.3f;
                            }

                            if (ship.getEngineController().isAccelerating()) {
                                targetL = 1.2f;
                                targetG = 1.2f;
                                targetW = 1f;
                            } else if (ship.getEngineController().isAcceleratingBackwards()) {
                                targetL = 0.4f;
                                targetW = 2f;
                                targetG = 2f;
                            }

                            if (ship.getEngineController().isDecelerating()) {
                                targetW = 3f;
                                targetL = 0.2f;
                                targetG = 1.4f;
                            }

                            float contrail = controller.getEngineSlot().getContrailWidth();
                            if (contrail == 32f && ship.getSystem() != null && !ignoreShips.contains(ship.getHullSpec().getBaseHullId())) { // it's just magic
                                if (ship.getSystem() != null && ship.getSystem().isOn()) {
                                    flare.setDisabled(false);
                                } else {
                                    targetG = 0f;
                                    targetL = 0f;
                                    targetW = 0f;

                                    flare.setDisabled(true);
                                }
                            } else {
                                flare.setDisabled(ship.isPhased());
                            }

                            if (ship.getFluxTracker().isEngineBoostActive()) {
                                targetG *= 1.4f;
                                targetL *= 1.4f;
                                targetW *= 1.4f;
                            }

                            float l = MathUtils.clamp(((targetL - flare.getLevelLength()) * amount) + flare.getLevelLength(), 0.2f, 3f);
                            float w = MathUtils.clamp(((targetW - flare.getLevelWidth()) * amount) + flare.getLevelWidth(), 0.2f, 3f);
                            flare.setLevelLength(l);
                            flare.setLevelWidth(w);

                            float g = MathUtils.clamp(((targetG - flare.getGlowSize()) * amount) + flare.getGlowSize(), 0.2f, 2.5f);
                            flare.setGlowSize(g);

                            flare.setContrailSize(controller.getEngineSlot().getContrailWidth());
                        }
                    } else {
                        ships.add(ship);
                        fer_EngineFlareShader.addFlare(controller, new fer_BaseFlare(pos, size, color, angle), ship);
                    }
                }
            }

            // to be continued?
            /*for (MissileAPI missile : engine.getMissiles()) {
                boolean isStored = missiles.contains(missile);

                missile.getEngineController().extendFlame(this, -1f, -1f, -1f);
                //missile.setGlowRadius(0f);

                Vector2f pos = missile.getTailEnd();
                float angle = missile.getFacing();

                Missile m = (Missile) missile;
                m.getEngineBoostLevel();
                Vector2f size = new Vector2f(
                        m.getEngineLocations().get(0).getWidth(),
                        m.getEngineLocations().get(0).getLength()
                );

                Color color = m.getEngineLocations().get(0).getColor();

                if (isStored) {
                    fer_EngineFlareAPI flare = fer_EngineFlareShader.getFlare(missile);
                    if (flare != null) {
                        flare.setAngle(angle + missile.getFacing());
                        flare.setSize(size);
                        flare.setLocation(pos);
                        flare.setColor(color);
                    }
                } else {
                    missiles.add(missile);
                    fer_EngineFlareShader.addFlare(null, new fer_BaseFlare(pos, size, color, angle), missile);
                }
            }*/
        }
    }

    @Override
    public EnumSet<CombatEngineLayers> getActiveLayers() {
        if (fer_ModPlugin.RENDER_OVER_WEAPONS) return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
        return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
    }

    @Override
    public float getRenderRadius() {
        return Float.MAX_VALUE;
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        if (!enabled) return;

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        if (fer_ModPlugin.RENDER_OVER_WEAPONS) {
            if (layer == CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER) drawFlares();
        } else {
            if (layer == CombatEngineLayers.ABOVE_SHIPS_LAYER) drawFlares();

            for (ShipAPI ship : engine.getShips()) {
                for (WeaponAPI weapon : ship.getAllWeapons()) {
                    Vector2f pos = weapon.getSlot().computePosition(ship);
                    if (weapon.getSprite() != null && !weapon.getSlot().isHidden()) weapon.getSprite().renderAtCenter(pos.x, pos.y);

                    //barrel offsets are ignored, too icky and probably too much overhead
                    //if (weapon.getBarrelSpriteAPI() != null) weapon.getBarrelSpriteAPI().renderAtCenter(pos.x, pos.y);
                }
            }
        }
    }

    public static class FlareData {
        public final ShipEngineControllerAPI.ShipEngineAPI engine;
        public final fer_EngineFlareAPI flare;
        public final CombatEntityAPI entity;

        public FlareData(ShipEngineControllerAPI.ShipEngineAPI engine, fer_EngineFlareAPI flare, CombatEntityAPI entity) {
            this.engine = engine;
            this.flare = flare;
            this.entity = entity;
        }
    }

    public static Color blend(Color c1, Color c2, float ratio) {
        if ( ratio > 1f ) ratio = 1f;
        else if ( ratio < 0f ) ratio = 0f;
        float iRatio = 1.0f - ratio;

        int i1 = c1.getRGB();
        int i2 = c2.getRGB();

        int a1 = (i1 >> 24 & 0xff);
        int r1 = ((i1 & 0xff0000) >> 16);
        int g1 = ((i1 & 0xff00) >> 8);
        int b1 = (i1 & 0xff);

        int a2 = (i2 >> 24 & 0xff);
        int r2 = ((i2 & 0xff0000) >> 16);
        int g2 = ((i2 & 0xff00) >> 8);
        int b2 = (i2 & 0xff);

        int a = (int)((a1 * iRatio) + (a2 * ratio));
        int r = (int)((r1 * iRatio) + (r2 * ratio));
        int g = (int)((g1 * iRatio) + (g2 * ratio));
        int b = (int)((b1 * iRatio) + (b2 * ratio));

        return new Color( a << 24 | r << 16 | g << 8 | b );
    }
}