package io.github.nascentlogic.jgen.io;

import io.github.nascentlogic.jgen.Disposable;
import io.github.nascentlogic.jgen.Jgen;
import io.github.nascentlogic.jgen.gfx.Bitmap;
import io.github.nascentlogic.jgen.utils.AtlasPacker;
import io.github.nascentlogic.jgen.utils.TextureRegion;
import org.joml.Vector2i;
import org.tinylog.Logger;

import java.util.*;

/**
 * F.Dahl, 6/21/2026
 */
public class TextureAtlas implements Disposable {

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
    private TextureAtlas() {
        this.atlasName = "";
        this.modifiedHash = 0;
        this.entries = Collections.emptyList();
        this.regionMap = new HashMap<>();
    }

    TextureAtlas(String name, List<Bitmap> bitmaps, List<String> names, int modifiedHash) {
        this.atlasName = Objects.requireNonNull(name, "Atlas name cannot be null");
        this.modifiedHash = modifiedHash;
        this.entries = generate(bitmaps,names);
        rebuildLookupMap();
    }

    private List<Entry> generate(List<Bitmap> bitmaps, List<String> names) {
        int count = Math.min(bitmaps.size(),names.size());
        if (count == 0) return new ArrayList<>();
        List<AtlasPacker.Rectangle> packRequest = new ArrayList<>(count);
        int bitmapChannels = 1;
        for (int i = 0; i < count; i++) {
            Bitmap bitmap = bitmaps.get(i);
            bitmapChannels = Math.max( // could opt for always 4 instead
                    bitmapChannels,
                    bitmap.channels());
            packRequest.add(new AtlasPacker.Rectangle(i,
                    bitmap.width(),
                    bitmap.height()));
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
            Entry entry = new Entry(name,result.r());
            bitmap.blitRegion(
                    atlasRegion,
                    entry.region.x,
                    entry.region.y);
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
