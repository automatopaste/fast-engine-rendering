package data.scripts.shaders.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ViewportAPI;
import data.scripts.shaders.fer_EngineFlareShader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import java.awt.*;
import java.nio.FloatBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.glDrawArraysInstanced;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

public class fer_ShaderRendererInstanced {
    private final int vao;
    private final fer_ShaderProgram program;

    private final int modelViewVBO;
    private final int colorVBO;
    private final int boostVBO;
    private FloatBuffer modelViewBuffer;
    private FloatBuffer colorBuffer;
    private FloatBuffer boostBuffer;

    public fer_ShaderRendererInstanced(fer_ShaderProgram program, int numInstances) {
        this.program = program;

        //configure vao and vbos
        float[] vertices = new float[] {
                0f, 1f,
                1f, 0f,
                0f, 0f,

                0f, 1f,
                1f, 1f,
                1f, 0f,
        };

        FloatBuffer verticesBuffer = BufferUtils.createFloatBuffer(vertices.length);
        verticesBuffer.put(vertices).flip();

        // Create the VAO and bind to it
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        // Create the VBO and bind to it
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        int size = 2;
        glVertexAttribPointer(0, size, GL_FLOAT, false, size * Float.SIZE / Byte.SIZE, 0);

        modelViewVBO = glGenBuffers();
        modelViewBuffer = BufferUtils.createFloatBuffer(16 * numInstances);
        glBindBuffer(GL_ARRAY_BUFFER, modelViewVBO);
        int start = 1;
        for (int i = 0; i < 4; i++) {
            glVertexAttribPointer(start, 4, GL_FLOAT, false, 4 * 4 * 4, i * 4 * 4);
            glVertexAttribDivisor(start, 1);
            glEnableVertexAttribArray(start);
            start++;
        }

        colorVBO = glGenBuffers();
        colorBuffer = BufferUtils.createFloatBuffer(4 * numInstances);
        glBindBuffer(GL_ARRAY_BUFFER, colorVBO);
        glVertexAttribPointer(5, 4, GL_FLOAT, false, 4 * Float.SIZE / Byte.SIZE, 0);
        glVertexAttribDivisor(5, 1);
        glEnableVertexAttribArray(5);

        boostVBO = glGenBuffers();
        boostBuffer = BufferUtils.createFloatBuffer(numInstances);
        glBindBuffer(GL_ARRAY_BUFFER, boostVBO);
        glVertexAttribPointer(6, 1, GL_FLOAT, false, Float.SIZE / Byte.SIZE, 0);
        glVertexAttribDivisor(6, 1);
        glEnableVertexAttribArray(6);

        glBindVertexArray(0);
    }

    public void initRender() {
        int start = 1;
        int numElements = 4;
        for (int i = 0; i < numElements; i++) {
            glEnableVertexAttribArray(start + i);
        }
    }

    protected void endRender() {
        int start = 1;
        int numElements = 4;
        for (int i = 0; i < numElements; i++) {
            glDisableVertexAttribArray(start + i);
        }
    }

    public void renderInstanced(ViewportAPI viewport, List<fer_EngineFlareShader.FlareData> flares, Matrix4f projection, boolean isGlow) {
        glBindVertexArray(vao);

        program.bind();

        initRender();

        FloatBuffer projectionBuffer = BufferUtils.createFloatBuffer(16);
        projection.store(projectionBuffer);
        projectionBuffer.flip();
        int loc = glGetUniformLocation(program.getProgramID(), "projection");
        glUniformMatrix4(loc, false, projectionBuffer);

        FloatBuffer timeBuffer = BufferUtils.createFloatBuffer(1);
        timeBuffer.put(Global.getCombatEngine().getTotalElapsedTime(false));
        timeBuffer.flip();
        glUniform1(glGetUniformLocation(program.getProgramID(), "iTime"), timeBuffer);

        modelViewBuffer.clear();
        colorBuffer.clear();
        boostBuffer.clear();
        for (fer_EngineFlareShader.FlareData flareData : flares) {
            Matrix4f modelView = isGlow ? flareData.flare.getModelViewGlow(viewport) : flareData.flare.getModelView(viewport);
            modelView.store(modelViewBuffer);

            Color c = flareData.flare.getColor();
            float alpha = (flareData.flare.getDisabled() || flareData.flare.getLevelWidth() <= 0.25f || flareData.flare.getLevelLength() <= 0.25f) ? 0f : c.getAlpha() / 255f;
            new Vector4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, alpha).store(colorBuffer);

            float b = flareData.flare.getLevelLength() / 3f;
            boostBuffer.put(b);
        }
        modelViewBuffer.flip();
        colorBuffer.flip();
        boostBuffer.flip();

        glBindBuffer(GL_ARRAY_BUFFER, modelViewVBO);
        glBufferData(GL_ARRAY_BUFFER, modelViewBuffer, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, colorVBO);
        glBufferData(GL_ARRAY_BUFFER, colorBuffer, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, boostVBO);
        glBufferData(GL_ARRAY_BUFFER, boostBuffer, GL_DYNAMIC_DRAW);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);

        glDrawArraysInstanced(GL_TRIANGLES, 0, 6, flares.size());

        endRender();

        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glBindVertexArray(0);

        program.unbind();

        GL11.glDisable(GL11.GL_BLEND);
    }

    public static Matrix4f orthogonal(float right, float top) {
        Matrix4f matrix = new Matrix4f();

        float left = 0f;
        float bottom = 0f;
        float zNear = -100f;
        float zFar = 100f;

        matrix.m00 = 2f / (right - left);

        matrix.m11 = 2f / (top - bottom);
        matrix.m22 = 2f / (zNear - zFar);

        matrix.m30 = -(right + left) / (right - left);
        matrix.m31 = -(top + bottom) / (top - bottom);
        matrix.m32 = -(zFar + zNear) / (zFar - zNear);

        matrix.m33 = 1f;

        return matrix;
    }

    public void dispose() {
        glDeleteBuffers(modelViewVBO);
        glDeleteBuffers(colorVBO);
        glDeleteBuffers(boostVBO);
        glDeleteVertexArrays(vao);

        if (modelViewBuffer != null) {
            modelViewBuffer.clear();
            modelViewBuffer = null;
        }
        if (colorBuffer != null) {
            colorBuffer.clear();
            colorBuffer = null;
        }
        if (boostBuffer != null) {
            boostBuffer.clear();
            boostBuffer = null;
        }
    }
}