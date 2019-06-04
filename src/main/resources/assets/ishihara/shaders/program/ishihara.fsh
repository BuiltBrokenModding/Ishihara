#version 120

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;

//https://ixora.io/projects/colorblindness/color-blindness-simulation-research/
const mat3 Daltonize = mat3(0.31399022, 0.63951294, 0.04649755, 0.15537241, 0.75789446, 0.08670142, 0.01775239, 0.10944209, 0.87256922);
const mat3 InverseDaltonize = mat3(5.47221206, -4.6419601, 0.16963708, -1.1252419, 2.29317094, -0.1678952, 0.02980165, -0.19318073, 1.16364789);

uniform vec3 Deficiency0;
uniform vec3 Deficiency1;
uniform vec3 Deficiency2;

void main() {
    gl_FragColor = vec4(texture2D(DiffuseSampler, texCoord).rgb * Daltonize * mat3(Deficiency0, Deficiency1, Deficiency2) * InverseDaltonize, 1.0);
}
