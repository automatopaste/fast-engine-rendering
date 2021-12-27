#version 330

in vec2 vCoord;
in vec4 modColor;
in float vBoost;

out vec4 fColor;

uniform float iTime;
//author tomatopaste, Jon Micheelsen
//messing with this can break stuff so make a backup first :slight_smile:
void main() {
    float a = iTime * 0.7; //speed
    const float b = 5.0; //lobe depth
    const float bInv = 1.0 / b;
    const float n = 2.5; //number of lobes
    float x = vCoord.x;
    float y = vCoord.y;
    const float pi = 3.14159265359;
    float h = (1.0 - (x * x * x)) * ((cos(pi * 2.5 * n * (x - a)) * bInv) + (1.0 - bInv));

    float l = 0.025; //fadeout at mult edge distance

    float m = clamp((x - l) * 2.0, 0.0, 1.0);//no if or division

    //abs distance from func
    float e1 = abs(y - 0.53);
    float d1 = abs(e1 - h);

    float t1 = clamp(((d1 / e1) - 1.0), 0.0, 2.0);
    float test = t1;
    float w1 = clamp((0.35 * h + 0.75) * 2.0 * abs((0.45 * -(1.0 - x)) + e1), 0.15, 3.0);
    t1 *= w1;

    float e2 = abs(y - 0.47);
    float d2 = abs(e2 - h);

    float t2 = clamp(((d2 / e2) - 1.0), 0.0, 2.0);

    float w2 = clamp((0.35 * h + 0.75) * 2.0 * abs((0.45 * -(1.0 - x)) + e2), 0.15, 3.0);
    t2*= w2;

    float t = t1 + t2;

    //    float fade = sqrt(1.0 - vCoord.x);
    float fade = (1.0 - vCoord.x * vCoord.x);//looks almost identical without a sqrt

    fColor = modColor * t * (vBoost * 1.1);
    //fColor = vec4(fColor.r + 0.5, fColor.g + 0.5, fColor.b + 0.5, fColor.a);
    fColor.a *= m;
    fColor *= fade;
    fColor *= 6.0;
}