package io.github.nascentlogic.jgen.io;

import io.github.nascentlogic.jgen.utils.Disposable;
import io.github.nascentlogic.jgen.gfx.Bitmap;
import io.github.nascentlogic.jgen.utils.AtlasPacker;
import io.github.nascentlogic.jgen.utils.TextureRegion;
import org.joml.Vector2i;
import org.tinylog.Logger;

import java.util.*;

/**
 * F.Dahl, 6/21/2026
 */
public class BitmapAtlas implements Disposable {


    public static final String FILE_SUFFIX = "_atlas";

    public record Entry(String name, TextureRegion region) {
        public Entry(String name, TextureRegion region) {
            Objects.requireNonNull(region, "Region cannot be null");
            Objects.requireNonNull(name, "name cannot be null");
            this.name = name;
            this.region = region;
        } public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Entry other = (Entry) obj;
            return name.equals(other.name);
        } public int hashCode() {
            return name.hashCode();
        }
    }

    private final String atlasName;
    private final int modifiedHash;
    private final List<Entry> entries;
    private transient Map<String,TextureRegion> regionMap;
    private transient Bitmap bitmap;

    /** GSON */
    private BitmapAtlas() {
        this.atlasName = "";
        this.modifiedHash = 0;
        this.entries = Collections.emptyList();
        this.regionMap = new HashMap<>();
    }

    BitmapAtlas(String name, List<Bitmap> bitmaps, List<String> names, int modifiedHash) {
        this.atlasName = Objects.requireNonNull(name, "Atlas name cannot be null");
        this.modifiedHash = modifiedHash;
        this.entries = generate(bitmaps,names);
        rebuildLookupMap();
    }

    private static final int MARGIN = 2;
    private List<Entry> generate(List<Bitmap> bitmaps, List<String> names) {
        int count = Math.min(bitmaps.size(),names.size());
        if (count == 0) return new ArrayList<>();
        List<AtlasPacker.Rectangle> packRequest = new ArrayList<>(count);
        int bitmapChannels = 1;
        for (int i = 0; i < count; i++) {
            Bitmap bitmap = bitmaps.get(i);
            bitmapChannels = Math.max(
                    bitmapChannels,
                    bitmap.channels());
            packRequest.add(new AtlasPacker.Rectangle(i,
                    bitmap.width() + (MARGIN * 2),
                    bitmap.height() + (MARGIN * 2)));
        } Vector2i size = new Vector2i();
        List<AtlasPacker.Region> packResult;
        packResult = AtlasPacker.pack(packRequest,size);
        if (packResult.isEmpty()) {
            Logger.warn("Packing resulted in zero valid regions.");
            return List.of();
        }
        Disposable.free(bitmap); // redundant but fine
        bitmap = new Bitmap(size.x,size.y,bitmapChannels);
        List<Entry> entries = new ArrayList<>(packResult.size());
        for (AtlasPacker.Region result : packResult) {
            Bitmap atlasRegion = bitmaps.get(result.id());
            String name = names.get(result.id());
            TextureRegion region = result.r();
            region.x += MARGIN;
            region.y += MARGIN;
            region.w = atlasRegion.width();
            region.h = atlasRegion.height();
            Entry entry = new Entry(name,region);
            bitmap.blitRegion(
                    atlasRegion,
                    entry.region.x,
                    entry.region.y);

            // =========================================================
            // Edge Extrusion
            // =========================================================

            int rx = region.x;
            int ry = region.y;
            int rw = region.w;
            int rh = region.h;
            int rEdgeX = rx + rw - 1;
            int bEdgeY = ry + rh - 1;

            if (atlasRegion.channels() == 4) {
                // 1. Vertical Edges
                for (int y = 0; y < rh; y++) {
                    int targetY = ry + y;
                    int l = atlasRegion.getPixel(0, y);
                    if ((l >>> 24) != 0) {
                        for (int m = 1; m <= MARGIN; m++)
                            bitmap.setPixel(l, rx - m, targetY);
                    }
                    int r = atlasRegion.getPixel(rw - 1, y);
                    if ((r >>> 24) != 0) {
                        for (int m = 1; m <= MARGIN; m++)
                            bitmap.setPixel(r, rEdgeX + m, targetY);
                    }
                }
                // 2. Horizontal Edges
                for (int x = 0; x < rw; x++) {
                    int targetX = rx + x;
                    int t = atlasRegion.getPixel(x, 0);
                    if ((t >>> 24) != 0) {
                        for (int m = 1; m <= MARGIN; m++)
                            bitmap.setPixel(t, targetX, ry - m);
                    }
                    int b = atlasRegion.getPixel(x, rh - 1);
                    if ((b >>> 24) != 0) {
                        for (int m = 1; m <= MARGIN; m++)
                            bitmap.setPixel(b, targetX, bEdgeY + m);
                    }
                }
                // 3. Four Corners
                int tl = atlasRegion.getPixel(0, 0);
                if ((tl >>> 24) != 0) {
                    for (int mx = 1; mx <= MARGIN; mx++)
                        for (int my = 1; my <= MARGIN; my++)
                            bitmap.setPixel(tl, rx - mx, ry - my);
                }
                int tr = atlasRegion.getPixel(rw - 1, 0);
                if ((tr >>> 24) != 0) {
                    for (int mx = 1; mx <= MARGIN; mx++)
                        for (int my = 1; my <= MARGIN; my++)
                            bitmap.setPixel(tr, rEdgeX + mx, ry - my);
                }
                int bl = atlasRegion.getPixel(0, rh - 1);
                if ((bl >>> 24) != 0) {
                    for (int mx = 1; mx <= MARGIN; mx++)
                        for (int my = 1; my <= MARGIN; my++)
                            bitmap.setPixel(bl, rx - mx, bEdgeY + my);
                }
                int br = atlasRegion.getPixel(rw - 1, rh - 1);
                if ((br >>> 24) != 0) {
                    for (int mx = 1; mx <= MARGIN; mx++)
                        for (int my = 1; my <= MARGIN; my++)
                            bitmap.setPixel(br, rEdgeX + mx, bEdgeY + my);
                }
            } else {
                // 1. Vertical Edges
                for (int y = 0; y < rh; y++) {
                    int targetY = ry + y;
                    int l = atlasRegion.getPixel(0, y);
                    int r = atlasRegion.getPixel(rw - 1, y);
                    for (int m = 1; m <= MARGIN; m++) {
                        bitmap.setPixel(l, rx - m, targetY);
                        bitmap.setPixel(r, rEdgeX + m, targetY);
                    }
                }
                // 2. Horizontal Edges
                for (int x = 0; x < rw; x++) {
                    int targetX = rx + x;
                    int t = atlasRegion.getPixel(x, 0);
                    int b = atlasRegion.getPixel(x, rh - 1);
                    for (int m = 1; m <= MARGIN; m++) {
                        bitmap.setPixel(t, targetX, ry - m);
                        bitmap.setPixel(b, targetX, bEdgeY + m);
                    }
                }
                // 3. Four Corners
                int tl = atlasRegion.getPixel(0, 0);
                int tr = atlasRegion.getPixel(rw - 1, 0);
                int bl = atlasRegion.getPixel(0, rh - 1);
                int br = atlasRegion.getPixel(rw - 1, rh - 1);
                for (int mx = 1; mx <= MARGIN; mx++) {
                    for (int my = 1; my <= MARGIN; my++) {
                        bitmap.setPixel(tl, rx - mx, ry - my);
                        bitmap.setPixel(tr, rEdgeX + mx, ry - my);
                        bitmap.setPixel(bl, rx - mx, bEdgeY + my);
                        bitmap.setPixel(br, rEdgeX + mx, bEdgeY + my);
                    }
                }
            }
            entries.add(entry);
        } return entries;
    }

    void rebuildLookupMap() {
        Map<String, TextureRegion> newMap = HashMap.newHashMap(entries.size());
        for (Entry entry : entries) newMap.put(entry.name(), entry.region());
        this.regionMap = newMap;
    }

    void setBitmap(Bitmap atlas) {
        this.bitmap = atlas;
    }

    boolean isOutdated(int modifiedHash) {
        return modifiedHash != this.modifiedHash;
    }

    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public TextureRegion get(String name) {
        return regionMap.get(name);
    }

    public Bitmap bitmap() {
        return bitmap;
    }

    @Override
    public void free() {
        Disposable.free(bitmap);
    }
}
