package data.scripts.shaders.util;

import cmu.shaders.BaseRenderPlugin;
import cmu.shaders.ShaderProgram;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.ViewportAPI;
import data.scripts.shaders.fer_EnginePlugin;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import java.awt.*;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL31.glDrawElementsInstanced;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

public class fer_FlareRenderer extends BaseRenderPlugin {

    private final int textureID;

    private List<fer_EnginePlugin.FlareData> drawTargets;
    private FloatBuffer projectionBuffer;

    private FloatBuffer modelViewBuffer;
    private FloatBuffer colourBuffer;
    private FloatBuffer boostBuffer;
    private FloatBuffer timeBuffer;


    private CombatEngineLayers layer;

    public fer_FlareRenderer() {
        drawTargets = new ArrayList<>();
        textureID = Global.getSettings().getSprite("fer", "engine_texture_test").getTextureId();
    }

    public fer_FlareRenderer(int textureID) {
        drawTargets = new ArrayList<>();
        this.textureID = textureID;
    }

    @Override
    protected int[] initBuffers() {
        projectionBuffer = BufferUtils.createFloatBuffer(16);
        timeBuffer = BufferUtils.createFloatBuffer(1);

        // vertices
        int verticesVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, verticesVBO);
        glBufferData(GL_ARRAY_BUFFER, VERTICES_BUFFER, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        int size = 2;
        glVertexAttribPointer(0, size, GL_FLOAT, false, size * Float.SIZE / Byte.SIZE, 0);

        // Create buffer for model view matrices
        final int modelViewVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, modelViewVBO);
        int start = 1;
        for (int i = 0; i < 4; i++) {
            glVertexAttribPointer(start, 4, GL_FLOAT, false, 4 * 4 * 4, i * 4 * 4);
            glVertexAttribDivisor(start, 1);
            glEnableVertexAttribArray(start);
            start++;
        }

        // Create buffer for colours
        final int colourVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, colourVBO);
        glVertexAttribPointer(5, 4, GL_FLOAT, false, 4 * Float.SIZE / Byte.SIZE, 0);
        glVertexAttribDivisor(5, 1);
        glEnableVertexAttribArray(5);

        final int boostVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, boostVBO);
        glVertexAttribPointer(6, 1, GL_FLOAT, false, Float.SIZE / Byte.SIZE, 0);
        glVertexAttribDivisor(6, 1);
        glEnableVertexAttribArray(6);

        return new int[] {
                verticesVBO,
                modelViewVBO,
                colourVBO,
                boostVBO
        };
    }

    @Override
    protected void populateUniforms(int glProgramID, CombatEngineLayers combatEngineLayers, ViewportAPI viewport) {
        projectionBuffer.clear();
        orthogonal(viewport.getVisibleWidth() / viewport.getViewMult(), viewport.getVisibleHeight() / viewport.getViewMult()).store(projectionBuffer);
        projectionBuffer.flip();
        int loc = glGetUniformLocation(glProgramID, "projection");
        glUniformMatrix4(loc, false, projectionBuffer);

        timeBuffer.put(Global.getCombatEngine().getTotalElapsedTime(false));
        timeBuffer.flip();
        glUniform1(glGetUniformLocation(glProgramID, "iTime"), timeBuffer);
    }

    @Override
    protected void updateBuffers(int[] buffers, CombatEngineLayers combatEngineLayers, ViewportAPI viewport) {
        numElements = drawTargets.size();

        modelViewBuffer = BufferUtils.createFloatBuffer(16 * numElements);
        colourBuffer = BufferUtils.createFloatBuffer(4 * numElements);
        boostBuffer = BufferUtils.createFloatBuffer(numElements);

        Matrix4f view = getViewMatrix(viewport);

        for (fer_EnginePlugin.FlareData flare : drawTargets) {
            Matrix4f modelView = flare.flare.getModelView(view);
            modelView.store(modelViewBuffer);

            Color c = flare.flare.getColor();
            float alpha = (flare.flare.getDisabled() || flare.flare.getLevelWidth() <= 0.25f || flare.flare.getLevelLength() <= 0.25f) ? 0f : c.getAlpha() / 255f;
            new Vector4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, alpha).store(colourBuffer);

            float b = flare.flare.getLevelLength() / 3f;
            boostBuffer.put(b);
        }

        modelViewBuffer.flip();
        colourBuffer.flip();
        boostBuffer.flip();

        glBindBuffer(GL_ARRAY_BUFFER, buffers[1]);
        glBufferData(GL_ARRAY_BUFFER, modelViewBuffer, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, buffers[2]);
        glBufferData(GL_ARRAY_BUFFER, colourBuffer, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, buffers[3]);
        glBufferData(GL_ARRAY_BUFFER, boostBuffer, GL_DYNAMIC_DRAW);
    }

    @Override
    protected void draw(CombatEngineLayers combatEngineLayers, ViewportAPI viewportAPI) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureID);

        glDrawElementsInstanced(GL_TRIANGLES, INDICES_BUFFER, numElements);

        glBindTexture(GL_TEXTURE_2D, 0);

        modelViewBuffer.clear();
        colourBuffer.clear();
        boostBuffer.clear();
    }

    @Override
    protected ShaderProgram initShaderProgram() {
        String vert;
        String frag;
        ShaderProgram shaderProgram = new ShaderProgram();
        try {
            vert = Global.getSettings().loadText("data/shaders/engineflare.vert");
            frag = Global.getSettings().loadText("data/shaders/engineflare_optimised.frag");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        shaderProgram.createVertexShader(vert);
        shaderProgram.createFragmentShader(frag);
        shaderProgram.link();

        return shaderProgram;
    }

    public void setDrawTargets(List<fer_EnginePlugin.FlareData> targets) {
        drawTargets = new ArrayList<>(targets);
    }
}
