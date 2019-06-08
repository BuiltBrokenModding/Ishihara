#version 120

//The shader used to draw color wheels. This also has an optional built-in deficiency.

varying vec2 texCoord;

const float PI = 3.14159265;

const mat3 Daltonize = mat3(0.31399022, 0.63951294, 0.04649755, 0.15537241, 0.75789446, 0.08670142, 0.01775239, 0.10944209, 0.87256922);
const mat3 InverseDaltonize = mat3(5.47221206, -4.6419601, 0.16963708, -1.1252419, 2.29317094, -0.1678952, 0.02980165, -0.19318073, 1.16364789);

uniform float UseDeficiency;
uniform vec3 Deficiency0;
uniform vec3 Deficiency1;
uniform vec3 Deficiency2;

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main(void) {
    vec2 pos = texCoord - 0.5;
    if (pos.x*pos.x + pos.y*pos.y < 0.25) {
        float angle = atan(pos.y, pos.x) - PI / 2.0;
        vec3 hsv = vec3(angle / (PI * 2.0), 1.0, 1.0);
        vec3 color = hsv2rgb(hsv);
        //Should the deficiency be applied. 0 = False, 1 = True
        if (UseDeficiency == 1.0) {
            color = color * Daltonize * mat3(Deficiency0, Deficiency1, Deficiency2) * InverseDaltonize;
        }
        gl_FragColor = vec4(color, 1.0);
    } else {
        gl_FragColor = vec4(1.0, 1.0, 1.0, 0.0);
    }
}
