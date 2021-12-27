#version 330 core

in vec2 vCoord;
in vec4 modColor;

out vec4 fColor;

uniform float iTime;
//author tomatopaste, Jon Micheelsen
void main() {
    const float speedPulse = 3.14159 * 2.0;
    const float intensity = 8.0;
    const float intensityPulse = 0.5;
    const float falloffSize = 24.0;
    const float falloffCutoff = 1.0 / (1.0 + falloffSize * falloffSize);//compiler will most likely const fold the calculation.

    vec2 pos = vCoord * 2.0 - 1.0;
    pos *= falloffSize;

    float inverseSqrFalloff = max(0.0, 1.0 / (1.0 + dot(pos, pos)) - falloffCutoff);//inverse square falloff, like light, attenuation distance is always the end of the billboard!

    float pulse = (intensity + cos(iTime * speedPulse) * intensityPulse);

    float alpha = min(modColor.a, 1.0);
    fColor = vec4(vec3(inverseSqrFalloff * modColor * pulse), alpha);//if it's additive blending, consider making the .a component just 1
}