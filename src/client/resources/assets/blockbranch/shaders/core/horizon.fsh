#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:globals.glsl>
#include <minecraft:texture_sampling.glsl>
#include <minecraft:terrainglobals.glsl>
#include <minecraft:chunksection.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec2 tileCoord;
layout(location = 4) flat in vec4 spriteRect;
layout(location = 5) flat in float materialFlag;

layout(location = 0) out vec4 fragColor;

// Leaf-like cells stand for many layered blocks, so they are drawn opaque. Where a texel is
// transparent, native rendering shows a leaf block behind it; that is emulated by sampling the
// same sprite at shifted offsets, darker per layer, which keeps tinted and pre-coloured
// textures alike. LEAF_BACKING is the rare fallback once every layer was transparent.
const vec2 LAYER_OFFSET = vec2(0.375, 0.625);
const float LAYER_SHADE = 0.72;
const float LEAF_BACKING = 0.40;

vec4 sampleTile(vec2 coord, vec2 size, vec2 inset, vec2 pixelSize, vec2 du, vec2 dv, vec2 texelScreenSize) {
    vec2 uv = clamp(spriteRect.xy + fract(coord) * size, spriteRect.xy + inset, spriteRect.zw - inset);
    return sampleNearest(Sampler0, uv, pixelSize, du, dv, texelScreenSize);
}

void main() {
    vec2 pixelSize = 1.0 / vec2(TextureSize);
    vec2 size = spriteRect.zw - spriteRect.xy;
    // Derivatives come from the continuous tiling coordinate, so the per-block wrap
    // does not pick the smallest mip along the seam.
    vec2 du = dFdx(tileCoord) * size;
    vec2 dv = dFdy(tileCoord) * size;
    vec2 texelScreenSize = sqrt(du * du + dv * dv);
    // Keep samples half a texel of the current mip inside the sprite so neighbours in
    // the atlas never bleed into a repeated face.
    float lod = max(0.0, log2(max(max(texelScreenSize.x, texelScreenSize.y) / max(pixelSize.x, pixelSize.y), 1e-6)));
    vec2 inset = min(pixelSize * exp2(floor(lod)) * 0.5, size * 0.5);
    vec4 tex = sampleTile(tileCoord, size, inset, pixelSize, du, dv, texelScreenSize);

    vec4 color;
    if (materialFlag > 0.875) {
        color = vec4(tex.rgb, 1.0);
    } else if (materialFlag > 0.625) {
        vec4 layer = tex;
        float shade = 1.0;
        vec2 coord = tileCoord;
        for (int i = 0; i < 4 && layer.a < 0.5; i++) {
            coord += LAYER_OFFSET;
            shade *= LAYER_SHADE;
            layer = sampleTile(coord, size, inset, pixelSize, du, dv, texelScreenSize);
        }
        color = vec4(layer.a < 0.5 ? vec3(LEAF_BACKING) : layer.rgb * shade, 1.0);
    } else if (materialFlag > 0.375) {
        if (tex.a < 0.5) {
            discard;
        }
        color = vec4(tex.rgb, 1.0);
    } else {
        color = tex;
    }
    color *= vertexColor;
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
