package org.example;

import org.joml.Vector2f;
import org.joml.Vector2i;

/**
 * F.Dahl, 7/28/2026
 */
public class TerrainView {


    private Vector2i viewSize = new Vector2i();
    private Vector2i viewPos = new Vector2i();
    private Vector2i clippedPos = new Vector2i();
    private Vector2i clippedSize = new Vector2i();

    public TerrainView(Vector2i targetResolution, int tileSize, int margin) {
        int w = (int) Math.ceil((float) targetResolution.x / tileSize) + margin;
        int h = (int) Math.ceil((float) targetResolution.y / tileSize) + margin;
        set(w,h);
    }

    public TerrainView(int width, int height) { set(width,height); }

    public void set(int width, int height) {
        clippedSize.set(viewSize.set(width,height));
        clippedPos.set(viewPos.set(0));
    }

    static void main() {
        TerrainView view = new TerrainView(new Vector2i(960,540),32,2);
        view.update(new Vector2f(0,0),    512);
        view.update(new Vector2f(512,512),512);
        view.update(new Vector2f(256,256),512);
        view.update(new Vector2f(128,128),512);
    }


    public void update(Vector2f cameraPos, int mapSize) {
        viewPos.x = Math.round(cameraPos.x) - viewSize.x / 2;
        viewPos.y = Math.round(cameraPos.y) - viewSize.y / 2;
        clippedPos.x = Math.clamp(viewPos.x,0, mapSize);
        clippedPos.y = Math.clamp(viewPos.y,0, mapSize);
        clippedSize.x = Math.clamp(viewPos.x + viewSize.x,clippedPos.x,mapSize) - clippedPos.x;
        clippedSize.y = Math.clamp(viewPos.y + viewSize.y,clippedPos.y,mapSize) - clippedPos.y;
        int i = 0;
    }





}
