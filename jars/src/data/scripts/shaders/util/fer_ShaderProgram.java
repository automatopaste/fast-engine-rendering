package data.scripts.shaders.util;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;

public class fer_ShaderProgram {
    int programID;

    int vertexShaderID;
    int fragmentShaderID;

    public fer_ShaderProgram() {
        programID = glCreateProgram();
    }

    public fer_ShaderProgram createVertexShader(String shaderCode) {
        // Create the shader and set the source
        vertexShaderID = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShaderID, shaderCode);

        // Compile the shader
        glCompileShader(vertexShaderID);

        // Check for errors
        if (glGetShaderi(vertexShaderID, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException("Error creating vertex shader\n"
                    + glGetShaderInfoLog(vertexShaderID, glGetShaderi(vertexShaderID, GL_INFO_LOG_LENGTH)));

        // Attach the shader
        glAttachShader(programID, vertexShaderID);

        return this;
    }

    public fer_ShaderProgram createFragmentShader(String shaderCode) {
        // Create the shader and set the source
        fragmentShaderID = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShaderID, shaderCode);

        // Compile the shader
        glCompileShader(fragmentShaderID);

        // Check for errors
        if (glGetShaderi(fragmentShaderID, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException("Error creating fragment shader\n"
                    + glGetShaderInfoLog(fragmentShaderID, glGetShaderi(fragmentShaderID, GL_INFO_LOG_LENGTH)));

        // Attach the shader
        glAttachShader(programID, fragmentShaderID);

        return this;
    }

    public fer_ShaderProgram link() {
        glLinkProgram(programID);
        if (glGetProgrami(programID, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Unable to link shader program: " + glGetProgramInfoLog(programID, 1024));
        }

        return this;
    }

    public void bind() {
        glUseProgram(programID);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void dispose() {
        // Unbind the program
        unbind();

        // Detach the shaders
        glDetachShader(programID, vertexShaderID);
        glDetachShader(programID, fragmentShaderID);

        // Delete the shaders
        glDeleteShader(vertexShaderID);
        glDeleteShader(fragmentShaderID);

        // Delete the program
        glDeleteProgram(programID);
    }

    public int getProgramID() {
        return programID;
    }
}