#version 330 core

in vec2 vCoord;
in vec4 modColor;

out vec4 fColor;

uniform float iTime;

void main() {
    float dist = length(vCoord - vec2(0.5, 0.5)) / length(vec2(0.5, 0.5));
    float c = (0.025 * cos(iTime * 3.14159 * 2.0) + 0.7) - dist;

    vec4 col = vec4(vec3(c), 1.0);

    float a = 0.125 / ((dist + 0.0)) - 0.5;

    col = vec4((0.7 - dist)) * a;
    fColor = modColor * (c + col * 0.4);
}