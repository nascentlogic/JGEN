package io.github.nascentlogic.jgen.io;

import io.github.nascentlogic.jgen.gfx.Bitmap;
import io.github.nascentlogic.jgen.utils.AtlasPacker;
import io.github.nascentlogic.jgen.utils.Disposable;
import io.github.nascentlogic.jgen.utils.TextureRegion;
import org.joml.Vector2i;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;


/**
 * F.Dahl, 7/25/2026
 */
public class Atlas implements Disposable {

    public static final int PACK_MARGIN = 2;
    public static final String FILE_SUFFIX = "_atlas";

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

    public record SorurceImage(Bitmap bitmap, String name) {
        public SorurceImage(Bitmap bitmap, String name) {
            this.bitmap = Objects.requireNonNull(bitmap);
            this.name = name == null ? "" : name;
        }
    }

    public record PackedImage(Bitmap bitmap, List<Entry> entries) {
        public PackedImage(Bitmap bitmap, List<Entry> entries) {
            this.entries = entries == null ? List.of() : entries;
            this.bitmap = bitmap;
        }
    }

    public record Entry(String name, TextureRegion region) {
        public Entry(String name, TextureRegion region) {
            this.region = Objects.requireNonNull(region);
            this.name = Objects.requireNonNull(name);
        } public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Entry other = (Entry) obj;
            return name.equals(other.name);
        } public int hashCode() {
            return name.hashCode();
        }
    }

    public static final class Info {
        public String name = "";
        public String directory = "";
        public String cache = "";
        public int mondifiedHash = 0;
        public int margin = PACK_MARGIN;
        public List<Entry> entries = new ArrayList<>();
        public Info() { /* GSON */ }
    }

    public static class TextureGen {
        /** Alloacate mipmaps. 0 = false */
        public int texMipmap = 0;
        /** Texture filter */
        public int texFilter = GL_NEAREST;
        /** Texture wrap  */
        public int texeWrap = GL_CLAMP_TO_EDGE;
        /** 0 = Euclidean, 1 = Manhattan, 2 = Chebyshev */
        public int distFunc = 0;
        public int maxDist = 38;
        /** */
        public float heightExp = 0.55f;
        /** */
        public float luminanceInfluence = 1.0f;
        /** */
        public float luminanceExp = 0.45f;
        /** Luminance influence on height output */
        public float heightDetail = 0.55f;
        /** Scale strength of normal output.*/
        public float normalScalar = 1.15f;

    }

    private final Bitmap[] bitmaps = new Bitmap[ImageType.array.length];
    private final Info info;

    Atlas(Info info, Bitmap[] bitmaps) {
        this.info = Objects.requireNonNull(info);
        Objects.requireNonNull(bitmaps);
        System.arraycopy(bitmaps, 0, this.bitmaps, 0, bitmaps.length);
    }

    Atlas(Info info, List<SorurceImage> images) {
        Objects.requireNonNull(images);
        this.info = Objects.requireNonNull(info);
        PackedImage packed = pack(images,info.margin,true);
        this.bitmaps[ImageType.COLOR.ordinal()] = packed.bitmap();
        this.info.entries = packed.entries();
    }

    @Override
    public void free() {
        Disposable.free(bitmaps);
    }


    public static PackedImage pack(List<SorurceImage> images, int margin, boolean freeImages) {
        if (images == null || images.isEmpty()) return new PackedImage(null,null);
        margin = Math.max(0,margin);
        int margin2 = margin * 2;
        int nextID = 0;
        int atlasChannels = 1;
        List<AtlasPacker.Rectangle> packRequest = new ArrayList<>(images.size());
        for (SorurceImage image : images) {
            Bitmap bm = image.bitmap;
            atlasChannels = Math.max(atlasChannels,bm.channels());
            packRequest.add(new AtlasPacker.Rectangle(nextID++, bm.width() + margin2, bm.height() + margin2));
        } Vector2i size = new Vector2i();
        List<AtlasPacker.Region> packResult = AtlasPacker.pack(packRequest,size);
        if (packResult.isEmpty()) {
            if (freeImages) for (SorurceImage image : images) image.bitmap.free();
            Logger.warn("Packing resulted in zero valid regions");
            return new PackedImage(null,null);
        } Bitmap bitmap = new Bitmap(size.x,size.y,atlasChannels);
        List<Entry> entries = new ArrayList<>(packResult.size());
        for (AtlasPacker.Region result : packResult) {
            SorurceImage source = images.get(result.id());
            TextureRegion region = result.r();
            region.x += margin;
            region.y += margin;
            region.w = source.bitmap.width();
            region.h = source.bitmap.height();
            bitmap.blitRegion(source.bitmap, region.x, region.y);
            entries.add(new Entry(source.name,region));
            if (freeImages) source.bitmap.free();
        } return new PackedImage(bitmap,entries);
    }
}
