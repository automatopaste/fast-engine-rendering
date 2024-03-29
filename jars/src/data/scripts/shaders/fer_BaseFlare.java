package data.scripts.shaders;

import com.fs.starfarer.api.combat.ViewportAPI;
import data.scripts.shaders.util.fer_ShaderRenderer;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.awt.*;

public class fer_BaseFlare {
    private Vector2f loc;
    private Vector2f size;
    private float glowSize;
    private Color color;
    //private final fer_ShaderRenderer renderer;
    //private final fer_ShaderRenderer glowRenderer;
    private float angle;

    private float levelLength;
    private float levelWidth;

    private boolean disabled;

    private float contrailSize;

    public fer_BaseFlare(Vector2f loc, Vector2f size, Color color, float angle) {
        this.loc = loc;
        this.size = size;
        this.color = color;
        this.angle = angle;

        glowSize = 1f;

        contrailSize = 1f;

        /*renderer = new fer_ShaderRenderer(
                "data/shaders/engineflare.vert",
                "data/shaders/engineflare.frag"
        );
        glowRenderer = new fer_ShaderRenderer(
                "data/shaders/glow.vert",
                "data/shaders/glow.frag"
        );*/

        disabled = false;
    }

    public Vector2f getLocation() {
        return loc;
    }

    public Vector2f getSize() {
        return size;
    }

    public void setGlowSize(float glowSize) {
        this.glowSize = glowSize;
    }

    public float getGlowSize() {
        return glowSize;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void dispose() {
        //renderer.dispose();
        //glowRenderer.dispose();
    }

    public float getAngle() {
        return angle;
    }

    public void setSize(Vector2f size) {
        this.size = size;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public void setLocation(Vector2f loc) {
        this.loc = loc;
    }

    public boolean advance(float amount) {
        return false;
    }

    public float getLevelLength() {
        return levelLength;
    }

    public float getLevelWidth() {
        return levelWidth;
    }

    public void setLevelLength(float levelLength) {
        this.levelLength = levelLength;
    }

    public void setLevelWidth(float levelWidth) {
        this.levelWidth = levelWidth;
    }

    public void render(ViewportAPI viewport) {
        if (disabled) return;

        Vector2f size = new Vector2f(this.size.x * levelLength, this.size.y * levelWidth);

        /*renderer.render(viewport,
                loc,
                size,
                angle,
                new Vector4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 0.7f),
                Global.getCombatEngine().getTotalElapsedTime(false),
                new Vector2f(0f, size.y / 2f)
        );
        glowRenderer.render(
                viewport,
                loc,
                new Vector2f(this.size.x * glowSize, this.size.x * glowSize * 2f),
                angle,
                new Vector4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 0.7f),
                Global.getCombatEngine().getTotalElapsedTime(false),
                new Vector2f(this.size.x * glowSize / 2f, this.size.x * glowSize * 2f / 2f)
        );*/
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public fer_ShaderRenderer getFlareRenderer() {
        return null;
    }

    public fer_ShaderRenderer getGlowRenderer() {
        return null;
    }

    public Matrix4f getModelView(Matrix4f view) {
//        float viewMult = viewport.getViewMult();
//        Matrix4f matrix = new Matrix4f();
//
//        matrix.setIdentity();
//
//        //view
//        matrix.translate(new Vector3f(viewport.getVisibleWidth() / (2f * viewMult), viewport.getVisibleHeight() / (2f * viewMult), 0f));
//        matrix.scale(new Vector3f(1f / viewport.getViewMult(), 1f / viewport.getViewMult(), 1f));
//        matrix.translate(new Vector3f(-viewport.getCenter().x, -viewport.getCenter().y, 0f));

        Matrix4f matrix = new Matrix4f(view);

        //model
        matrix.translate(new Vector3f(loc.x, loc.y, 0f));
        matrix.rotate((float) Math.toRadians(angle), new Vector3f(0f, 0f, 1f));
        Vector2f size = new Vector2f(this.size.x * levelLength, this.size.y * levelWidth);
        Vector2f offset = new Vector2f(0f, size.y / 2f);
        matrix.translate(new Vector3f(-offset.x, -offset.y, 0f));
        matrix.scale(new Vector3f(size.x, size.y, 1f));

        return matrix;
    }

    public Matrix4f getModelViewGlow(Matrix4f view) {
//        float viewMult = viewport.getViewMult();
//        Matrix4f matrix = new Matrix4f();
//
//        matrix.setIdentity();
//
//        //view
//        matrix.translate(new Vector3f(viewport.getVisibleWidth() / (2f * viewMult), viewport.getVisibleHeight() / (2f * viewMult), 0f));
//        matrix.scale(new Vector3f(1f / viewport.getViewMult(), 1f / viewport.getViewMult(), 1f));
//        matrix.translate(new Vector3f(-viewport.getCenter().x, -viewport.getCenter().y, 0f));

        Matrix4f matrix = new Matrix4f(view);

        //model
        matrix.translate(new Vector3f(loc.x, loc.y, 0f));
        matrix.rotate((float) Math.toRadians(angle), new Vector3f(0f, 0f, 1f));
        Vector2f size = new Vector2f((contrailSize + (this.size.x * 0.7f)) * glowSize * 1.5f, (contrailSize + (this.size.x * 1.1f)) * glowSize * 1.5f);
        Vector2f offset = new Vector2f(size.x / 2f, size.y / 2f);
        matrix.translate(new Vector3f(-offset.x, -offset.y, 0f));
        matrix.scale(new Vector3f(size.x, size.y, 1f));

        return matrix;
    }

    public boolean getDisabled() {
        return disabled;
    }

    public void setContrailSize(float contrailSize) {
        this.contrailSize = contrailSize;
    }

    public float getContrailSize() {
        return contrailSize;
    }
}
