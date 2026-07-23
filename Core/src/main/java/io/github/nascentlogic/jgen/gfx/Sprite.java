package io.github.nascentlogic.jgen.gfx;

import io.github.nascentlogic.jgen.utils.TextureRegion;
import org.joml.Vector4f;

/**
 * F.Dahl, 7/20/2026
 */
public class Sprite {

    private transient Texture[] texture;
    private transient TextureRegion[] region;
    private transient Vector4f[] texCoords;
    private String name;
    private float scaleX;
    private float scaleY;
    private boolean flippedX;
    private boolean flippedY;


}
