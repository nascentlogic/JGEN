package io.github.nascentlogic.jgen.io;

import io.github.nascentlogic.jgen.gfx.Bitmap;
import io.github.nascentlogic.jgen.utils.AtlasPacker;
import io.github.nascentlogic.jgen.utils.Disposable;
import io.github.nascentlogic.jgen.utils.TextureRegion;
import org.joml.Vector2i;
import org.tinylog.Logger;

import java.util.*;

/**
 * F.Dahl, 7/20/2026
 */
public class Atlas implements Disposable  {

    public static final int PACK_MARGIN = 2;
    public static final String FILE_SUFFIX = "_atlas";

    public static final class Info {
        String name = "";
        int modifiedHash = 0;
        int margin = PACK_MARGIN;
        boolean[] imageTypes = new boolean[ImageType.array.length];
        List<Entry> entries = List.of();
        private Info() { /* GSON */}
    }

    public enum ImageType {
        COLOR(""),
        NORMAL("_normal"),
        HEIGHT("_height"),
        SMOOTHNESS("_smooth"),
        METALLIC("_metallic"),
        EMISSIVE("_emissive");
        public static final ImageType[] array = values();
        public final String fileSuffix;
        ImageType(String fileSuffix) {
            this.fileSuffix = fileSuffix;
        }
    }

    public record Entry(String name, TextureRegion region) {
        public Entry(String name, TextureRegion region) {
            this.region = Objects.requireNonNull(region, "Region cannot be null");
            this.name = Objects.requireNonNull(name, "name cannot be null");
        } public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Entry other = (Entry) obj;
            return name.equals(other.name);
        } public int hashCode() {
            return name.hashCode();
        }
    }

    private final transient Bitmap[] bitmaps = new Bitmap[ImageType.array.length];
    private transient Map<String,TextureRegion> regionMap;
    private Info info;


    private Atlas() { /* GSON */ }

    Atlas(Info info, Bitmap[] bitmaps) {

    }

    Atlas(String name, List<Bitmap> bitmaps, List<String> names, int modifiedHash) {

    }



    public Bitmap bitmap(ImageType type) { return bitmaps[type.ordinal()]; }

    public List<Entry> entries() {
        return Collections.unmodifiableList(info.entries);
    }

    public TextureRegion get(String name) {
        return regionMap.get(name);
    }

    public void free() { Disposable.free(bitmaps); }




    void setBitmap(Bitmap bitmap, ImageType type) { bitmaps[type.ordinal()] = bitmap; }

    void rebuildLookupMap() {
        Map<String, TextureRegion> map = HashMap.newHashMap(info.entries.size());
        for (Entry entry : info.entries) map.put(entry.name(), entry.region());
        regionMap = map;
    }


    private List<Entry> generateAtlas(List<Bitmap> bitmaps, List<String> names, int margin) {
        int count = Math.min(bitmaps.size(),names.size());
        if (count == 0) return List.of();
        List<AtlasPacker.Rectangle> packRequest = new ArrayList<>(count);
        int bitmapChannels = 1;
        for (int i = 0; i < count; i++) {
            Bitmap bitmap = bitmaps.get(i);
            bitmapChannels = Math.max(
                    bitmapChannels,
                    bitmap.channels());
            packRequest.add(new AtlasPacker.Rectangle(i,
                    bitmap.width() + (margin * 2),
                    bitmap.height() + (margin * 2)));
        } Vector2i size = new Vector2i();
        List<AtlasPacker.Region> packResult;
        packResult = AtlasPacker.pack(packRequest,size);
        if (packResult.isEmpty()) {
            Logger.warn("Packing resulted in zero valid regions.");
            return List.of();
        }

        return null;
    }

    private void runEdgeExtrusion(Bitmap bitmap, List<Entry> entries, int margin) {
        if (bitmap == null || entries.isEmpty() || margin <= 0) return;
        final boolean hasAlpha = bitmap.channels() == 4;
        for (Entry entry : entries) {
            final TextureRegion region = entry.region();
            final int rx = region.x;
            final int ry = region.y;
            final int rw = region.w;
            final int rh = region.h;
            final int rEdgeX = rx + rw - 1;
            final int bEdgeY = ry + rh - 1;
            final int tl = bitmap.getPixel(rx, ry);
            final int tr = bitmap.getPixel(rEdgeX, ry);
            final int bl = bitmap.getPixel(rx, bEdgeY);
            final int br = bitmap.getPixel(rEdgeX, bEdgeY);
            if (hasAlpha) {
                for (int y = 0; y < rh; y++) {
                    int targetY = ry + y;
                    int l = bitmap.getPixel(rx, targetY);
                    if ((l >>> 24) != 0) {
                        for (int m = 1; m <= margin; m++) {
                            bitmap.setPixel(l, rx - m, targetY);
                        }
                    }
                    int r = bitmap.getPixel(rEdgeX, targetY);
                    if ((r >>> 24) != 0) {
                        for (int m = 1; m <= margin; m++) {
                            bitmap.setPixel(r, rEdgeX + m, targetY);
                        }
                    }
                }
                for (int x = 0; x < rw; x++) {
                    int targetX = rx + x;
                    int t = bitmap.getPixel(targetX, ry);
                    if ((t >>> 24) != 0) {
                        for (int m = 1; m <= margin; m++) {
                            bitmap.setPixel(t, targetX, ry - m);
                        }
                    }
                    int b = bitmap.getPixel(targetX, bEdgeY);
                    if ((b >>> 24) != 0) {
                        for (int m = 1; m <= margin; m++) {
                            bitmap.setPixel(b, targetX, bEdgeY + m);
                        }
                    }
                } if ((tl >>> 24) != 0) {
                    for (int mx = 1; mx <= margin; mx++) {
                        for (int my = 1; my <= margin; my++) {
                            bitmap.setPixel(tl, rx - mx, ry - my);
                        }
                    }
                } if ((tr >>> 24) != 0) {
                    for (int mx = 1; mx <= margin; mx++) {
                        for (int my = 1; my <= margin; my++) {
                            bitmap.setPixel(tr, rEdgeX + mx, ry - my);
                        }
                    }
                } if ((bl >>> 24) != 0) {
                    for (int mx = 1; mx <= margin; mx++) {
                        for (int my = 1; my <= margin; my++) {
                            bitmap.setPixel(bl, rx - mx, bEdgeY + my);
                        }
                    }
                } if ((br >>> 24) != 0) {
                    for (int mx = 1; mx <= margin; mx++) {
                        for (int my = 1; my <= margin; my++) {
                            bitmap.setPixel(br, rEdgeX + mx, bEdgeY + my);
                        }
                    }
                }
            } else {
                for (int y = 0; y < rh; y++) {
                    int targetY = ry + y;
                    int l = bitmap.getPixel(rx, targetY);
                    int r = bitmap.getPixel(rEdgeX, targetY);
                    for (int m = 1; m <= margin; m++) {
                        bitmap.setPixel(l, rx - m, targetY);
                        bitmap.setPixel(r, rEdgeX + m, targetY);
                    }
                } for (int x = 0; x < rw; x++) {
                    int targetX = rx + x;
                    int t = bitmap.getPixel(targetX, ry);
                    int b = bitmap.getPixel(targetX, bEdgeY);
                    for (int m = 1; m <= margin; m++) {
                        bitmap.setPixel(t, targetX, ry - m);
                        bitmap.setPixel(b, targetX, bEdgeY + m);
                    }
                } for (int mx = 1; mx <= margin; mx++) {
                    for (int my = 1; my <= margin; my++) {
                        bitmap.setPixel(tl, rx - mx, ry - my);
                        bitmap.setPixel(tr, rEdgeX + mx, ry - my);
                        bitmap.setPixel(bl, rx - mx, bEdgeY + my);
                        bitmap.setPixel(br, rEdgeX + mx, bEdgeY + my);
                    }
                }
            }
        }
    }


}
