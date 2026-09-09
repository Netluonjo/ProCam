package com.sondeptrai.procam.core.gl

object GlShaders {

    const val VERTEX_SHADER = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uTexMatrix;
        attribute vec4 aPosition;
        attribute vec4 aTextureCoord;
        varying vec2 vTextureCoord;
        
        void main() {
            gl_Position = uMVPMatrix * aPosition;
            vTextureCoord = (uTexMatrix * aTextureCoord).xy;
        }
    """

    const val FRAGMENT_SHADER = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        
        varying vec2 vTextureCoord;
        uniform samplerExternalOES sTexture;
        
        uniform vec2 uTexelSize;
        uniform int uEnablePeaking;
        uniform vec3 uPeakingColor;
        uniform float uPeakingThreshold;
        
        uniform int uEnableZebra;
        uniform float uZebraThreshold;
        uniform float uTime;
        uniform vec3 uWbTint;
        
        float getLuminance(vec4 color) {
            return dot(color.rgb, vec3(0.299, 0.587, 0.114));
        }
        
        void main() {
            vec4 baseColor = texture2D(sTexture, vTextureCoord);
            vec3 finalColor = baseColor.rgb;
            
            // 1. Focus Peaking using 3x3 Sobel Operator
            if (uEnablePeaking == 1) {
                float tl = getLuminance(texture2D(sTexture, vTextureCoord + vec2(-uTexelSize.x,  uTexelSize.y)));
                float tc = getLuminance(texture2D(sTexture, vTextureCoord + vec2(          0.0,  uTexelSize.y)));
                float tr = getLuminance(texture2D(sTexture, vTextureCoord + vec2( uTexelSize.x,  uTexelSize.y)));
                
                float ml = getLuminance(texture2D(sTexture, vTextureCoord + vec2(-uTexelSize.x,           0.0)));
                float mr = getLuminance(texture2D(sTexture, vTextureCoord + vec2( uTexelSize.x,           0.0)));
                
                float bl = getLuminance(texture2D(sTexture, vTextureCoord + vec2(-uTexelSize.x, -uTexelSize.y)));
                float bc = getLuminance(texture2D(sTexture, vTextureCoord + vec2(          0.0, -uTexelSize.y)));
                float br = getLuminance(texture2D(sTexture, vTextureCoord + vec2( uTexelSize.x, -uTexelSize.y)));
                
                float gx = -tl - 2.0 * ml - bl + tr + 2.0 * mr + br;
                float gy = -tl - 2.0 * tc - tr + bl + 2.0 * bc + br;
                float magnitude = sqrt(gx * gx + gy * gy);
                
                if (magnitude > uPeakingThreshold) {
                    finalColor = mix(finalColor, uPeakingColor, 0.85);
                }
            }
            
            // 2. Zebra Stripes for Overexposure Clipping Warning
            if (uEnableZebra == 1) {
                float luma = getLuminance(baseColor);
                if (luma >= uZebraThreshold) {
                    // 45-degree moving diagonal stripes pattern
                    float stripe = mod(gl_FragCoord.x + gl_FragCoord.y + uTime * 20.0, 16.0);
                    if (stripe < 8.0) {
                        finalColor = vec3(0.0); // black stripe
                    } else {
                        finalColor = vec3(1.0); // white stripe
                    }
                }
            }
            
            finalColor = clamp(finalColor * uWbTint, 0.0, 1.0);
            gl_FragColor = vec4(finalColor, baseColor.a);
        }
    """
}
