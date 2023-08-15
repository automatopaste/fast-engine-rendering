package data.scripts.shaders;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.loading.specs.EngineSlot;
import data.scripts.fer_ModPlugin;
import data.scripts.shaders.util.fer_FlareRenderer;
import data.scripts.shaders.util.fer_GlowRenderer;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class fer_EnginePlugin extends BaseEveryFrameCombatPlugin {
    private static final String DATA_KEY = "we_do_a_little_trolling";

    private final boolean enabled;
    private final fer_FlareRenderer flareRenderer;
    private final fer_GlowRenderer glowRenderer;

    public fer_EnginePlugin() {
        enabled = true;

        flareRenderer = new fer_FlareRenderer();
        glowRenderer = Global.getSettings().getBoolean("fer_UseSecondGlowShader") ? new fer_GlowRenderer() : null;

        flareRenderer.setLayer(getActiveLayer());
        Global.getCombatEngine().addLayeredRenderingPlugin(flareRenderer);

        if (glowRenderer != null) {
            glowRenderer.setLayer(getActiveLayer());
            Global.getCombatEngine().addLayeredRenderingPlugin(glowRenderer);
        }



    }

    public static void addFlare(ShipEngineControllerAPI.ShipEngineAPI engine, fer_BaseFlare flare, CombatEntityAPI entity) {
        List<FlareData> data = (List<FlareData>) Global.getCombatEngine().getCustomData().get(DATA_KEY);
        if (data != null) {
            data.add(new FlareData(engine, flare, entity, entity.getAngularVelocity()));
            Global.getCombatEngine().getCustomData().put(DATA_KEY, data);
        }
    }

    /**
     * For external API
     * FlareData instance must be tracked externally
     */
    public static void addFlare(FlareData flareData) {
        List<FlareData> data = (List<FlareData>) Global.getCombatEngine().getCustomData().get(DATA_KEY);
        if (data != null) {
            data.add(flareData);
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

    public static fer_BaseFlare getFlare(ShipEngineControllerAPI.ShipEngineAPI engine) {
        List<FlareData> data = (List<FlareData>) Global.getCombatEngine().getCustomData().get(DATA_KEY);
        if (data != null) for (FlareData flareData : data) if (flareData.engine != null && flareData.engine.equals(engine)) return flareData.flare;
        return null;
    }

//    public static fer_BaseFlare getFlare(MissileAPI missile) {
//        List<FlareData> data = (List<FlareData>) Global.getCombatEngine().getCustomData().get(DATA_KEY);
//        if (data != null) for (FlareData flareData : data) if (flareData.entity.equals(missile)) return flareData.flare;
//        return null;
//    }

    @Override
    public void init(CombatEngineAPI engine) {
        Global.getCombatEngine().getCustomData().put(DATA_KEY, new ArrayList<>());
    }

    public boolean isExpired() {
        return false;
    }

    private final List<ShipAPI> ships = new ArrayList<>();
    private final List<ShipWeaponRedrawData> shipsToRedrawWeapons = new ArrayList<>();
    //private final List<MissileAPI> missiles = new ArrayList<>();

    private static class ShipWeaponRedrawData {
        public ShipAPI ship;
        public float value;
        public ShipWeaponRedrawData(ShipAPI ship, float value) {
            this.ship = ship;
            this.value = value;
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> inputs) {
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
            ship:
            for (ShipAPI ship : engine.getShips()) {
                boolean isStored = ships.contains(ship);

                ship.setRenderEngines(false);

                Color priorityColour = ship.getEngineController().getFlameColorShifter().getCurr();

                for (ShipEngineControllerAPI.ShipEngineAPI controller : ship.getEngineController().getShipEngines()) {
                    if (!fer_ModPlugin.FORCE_OVERRIDE_STYLES && !fer_ModPlugin.INCLUDED_ENGINE_STYLES.contains(controller.getStyleId())) {
                        ship.setRenderEngines(true);
                        continue ship;
                    }

                    Vector2f pos = controller.getEngineSlot().computePosition(ship.getLocation(), ship.getFacing());
                    float angle = controller.getEngineSlot().getAngle();
                    Vector2f size = new Vector2f(
                            controller.getEngineSlot().getLength() * (ship.getEngineController().getExtendLengthFraction().getCurr() + 1f),
                            controller.getEngineSlot().getWidth() * (ship.getEngineController().getExtendWidthFraction().getCurr() + 1f)
                    );

                    Color color = controller.getEngineColor();
                    if (priorityColour != null) {
                        color = blend(color, priorityColour, 0.7f);
                    }

                    if (isStored) {
                        fer_BaseFlare flare = fer_EnginePlugin.getFlare(controller);
                        if (flare != null) {
                            flare.setAngle(angle + ship.getFacing());
                            flare.setSize(size);
                            flare.setLocation(pos);
                            flare.setColor(color);

                            float targetL = 0.3f, targetW = 0.6f, targetG = 0.6f;

                            if (ship.getEngineController().isStrafingLeft() || ship.getEngineController().isStrafingRight()) {
                                targetW = 1.1f;
                                targetG = 1.3f;
                            }

                            if (ship.getEngineController().isAccelerating()) {
                                targetL = 0.9f;
                                targetG = 1.2f;
                                targetW = 1f;
                            } else if (ship.getEngineController().isAcceleratingBackwards()) {
                                targetL = 0.4f;
                                targetW = 1.2f;
                                targetG = 2f;
                            }

                            if (ship.getEngineController().isDecelerating()) {
                                targetW = 1.2f;
                                targetL = 0.2f;
                                targetG = 1.4f;
                            }

                            if (((EngineSlot) controller.getEngineSlot()).isSystemActivated()) {
                                if (ship.getSystem() != null && ship.getSystem().isOn()) {
                                    flare.setDisabled(false);
                                } else {
                                    targetG = 0f;
                                    targetL = 0f;
                                    targetW = 0f;

                                    flare.setDisabled(true);
                                }
                            }

                            if (ship.getPhaseCloak() != null) {
                                flare.setDisabled(ship.isPhased());
                            }

                            if (ship.getFluxTracker().isEngineBoostActive()) {
                                targetG *= 1.3f;
                                targetL *= 1.3f;
                                targetW *= 1.3f;
                            }

                            if (controller.isDisabled()) {
                                targetG = 0f;
                                targetL = 0f;
                                targetW = 0f;
                            }

                            float dl = (targetL - flare.getLevelLength()) * amount * 2f;
                            float dw = (targetW - flare.getLevelWidth()) * amount * 2f;
                            flare.setLevelLength(MathUtils.clamp(dl + flare.getLevelLength(), 0.2f, 3f));
                            flare.setLevelWidth(MathUtils.clamp(dw + flare.getLevelWidth(), 0.2f, 2f));

                            float g = MathUtils.clamp(((targetG - flare.getGlowSize()) * amount) + flare.getGlowSize(), 0.8f, 1.2f);
                            flare.setGlowSize(g);

                            if (g > 1f) {
                                boolean ignore = false;
                                for (ShipWeaponRedrawData s : shipsToRedrawWeapons) {
                                    if (s.ship.equals(ship)) {
                                        ignore = true;
                                        break;
                                    }
                                }
                                if (!ignore) shipsToRedrawWeapons.add(new ShipWeaponRedrawData(ship, g));
                            }

                            flare.setContrailSize(controller.getEngineSlot().getContrailWidth());

                            //flare.setDisabled(false);
                        }
                    } else {
                        ships.add(ship);
                        fer_EnginePlugin.addFlare(controller, new fer_BaseFlare(pos, size, color, angle), ship);
                    }
                }
            }

            flareRenderer.setDrawTargets(data);
            if (glowRenderer != null) glowRenderer.setDrawTargets(data);

            // to be continued?
            /*for (MissileAPI missile : engine.getMissiles()) {
                Missile m = (Missile) missile;
                if (m.getEngineLocations().isEmpty()) continue;

                boolean isStored = missiles.contains(missile);

                missile.getEngineController().extendFlame(this, -1f, -1f, -1f);
                missile.setGlowRadius(0f);

                Vector2f pos = missile.getLocation();
                float angle = missile.getFacing();


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

        if (!fer_ModPlugin.RENDER_OVER_WEAPONS) {
            for (ShipWeaponRedrawData shipWeaponRedrawData : shipsToRedrawWeapons) {
                for (WeaponAPI weapon : shipWeaponRedrawData.ship.getAllWeapons()) {
                    if (weapon.getSprite() != null && !weapon.getSlot().isHidden()) {
                        Vector2f pos = weapon.getSlot().computePosition(shipWeaponRedrawData.ship);
                        float alpha = (shipWeaponRedrawData.value - 1f) / 2f;
                        weapon.getSprite().setAlphaMult(alpha);
                        weapon.getSprite().renderAtCenter(pos.x, pos.y);
                        weapon.getSprite().setAlphaMult(2f);
                    }

                    //barrel offsets are ignored, too icky and probably too much overhead
                    //if (weapon.getBarrelSpriteAPI() != null) weapon.getBarrelSpriteAPI().renderAtCenter(pos.x, pos.y);
                }
            }
            shipsToRedrawWeapons.clear();
        }
    }

    private CombatEngineLayers getActiveLayer() {
        if (fer_ModPlugin.RENDER_OVER_WEAPONS) return CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER;
        return CombatEngineLayers.PHASED_SHIPS_LAYER;
    }

    public static class FlareData {
        public final ShipEngineControllerAPI.ShipEngineAPI engine;
        public final fer_BaseFlare flare;
        public final CombatEntityAPI entity;
        public float angVel;

        public FlareData(ShipEngineControllerAPI.ShipEngineAPI engine, fer_BaseFlare flare, CombatEntityAPI entity, float angVel) {
            this.engine = engine;
            this.flare = flare;
            this.entity = entity;
            this.angVel = angVel;
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