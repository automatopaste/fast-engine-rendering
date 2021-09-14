package data.scripts.shaders;

import com.fs.starfarer.api.combat.ViewportAPI;
import data.scripts.shaders.util.fer_ShaderRenderer;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public interface fer_EngineFlareAPI {
    Vector2f getLocation();

    void setLocation(Vector2f location);

    Vector2f getSize();

    void setGlowSize(float size);

    float getGlowSize();

    void setSize(Vector2f size);

    Color getColor();

    void setColor(Color color);

    void dispose();

    float getAngle();

    void setAngle(float angle);

    boolean advance(float amount);

    void render(ViewportAPI viewport);

    float getLevelLength();

    float getLevelWidth();

    void setLevelLength(float level);

    void setLevelWidth(float level);

    void setDisabled(boolean disabled);

    fer_ShaderRenderer getFlareRenderer();

    fer_ShaderRenderer getGlowRenderer();

    Matrix4f getModelView(ViewportAPI viewport);

    Matrix4f getModelViewGlow(ViewportAPI viewportAPI);

    boolean getDisabled();

    void setContrailSize(float contrailSize);

    float getContrailSize();
}
