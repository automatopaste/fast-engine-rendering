#version 330

layout (location = 0) in vec2 vertex;
layout (location = 1) in mat4 modelViewInstanced;
layout (location = 5) in vec4 colorInstanced;
layout (location = 6) in float boost;

out vec2 vCoord;
out vec4 modColor;
out float vBoost;

uniform mat4 projection;

void main() {
	gl_Position = projection * modelViewInstanced * vec4(vertex, 0.0, 1.0);
	vCoord = vertex;
	modColor = colorInstanced;
	vBoost = boost;
}