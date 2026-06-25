package io.github.nascentlogic.jgen.utils;


import org.joml.Vector2i;
import org.tinylog.Logger;

import java.util.*;

import static io.github.nascentlogic.jgen.utils.JgenMath.nextPowerOfTwo;

/**
 * F.Dahl, 6/21/2026
 */
public class AtlasPacker {

    public static final int REGION_MAX = 1024;
    public static final int ESTIMATE_MAX = 4096;

    /**
     * Packs a collection of independent 2D rectangular dimensions into a single
     * optimized, power-of-two texture atlas.
     * <p>This implementation employs a deterministic shelf-packing algorithm with
     * active historical gap-merging and reactive sizing steps.
     * @param rectangles the source list of item dimensions requested for the atlas;
     * individual rectangles must have a width and height greater
     * than zero and less than or equal to {@link #REGION_MAX},
     * and elements with duplicate IDs are automatically filtered out
     * @param sizeDst an out-parameter vector populated with the final allocated
     * power-of-two dimensions (width, height) of the generated
     * atlas canvas upon successful execution, or zeroed out on failure
     * @return a list containing the packed results where each {@link Region} holds
     * the original asset ID along with its absolute mapped (x, y) coordinates
     * and dimensions within the final canvas boundary, or an empty list if
     * packing fails or inputs are invalid
     */
    public static List<Region> pack(List<Rectangle> rectangles, Vector2i sizeDst) {
        Objects.requireNonNull(rectangles, "null argument pack request list.");
        Objects.requireNonNull(sizeDst, "null argument atlas size destination.");
        if (rectangles.isEmpty()) {
            Logger.warn("Unable to pack: Empty list regions");
            sizeDst.zero();
            return List.of();
        }

        List<Rectangle> open = new ArrayList<>(rectangles.size());
        {
            Set<Rectangle> validSet = new HashSet<>();
            for (Rectangle rect : rectangles) {
                if (rect != null) {
                    if (rect.h > 0 && rect.h <= REGION_MAX) {
                        if (rect.w > 0 && rect.w <= REGION_MAX) {
                            if (validSet.add(rect)) {
                                open.add(rect);
                            }
                        }
                    }
                }
            }
        }
        if (open.isEmpty()) {
            Logger.warn("Unable to pack: All regions are invalid (make sure they have unique id's)");
            sizeDst.zero();
            return List.of();
        }

        // Pre-sort rectangles descending: Tallest to shortest
        open.sort(Comparator.naturalOrder());

        // =============================================================================
        // Make a initial estimate of the atlas size (everything in power of two)
        // =============================================================================
        int rectMaxH = open.getFirst().h;
        int rectMinH = open.getLast().h;
        int rectMinW = Integer.MAX_VALUE;
        int rectMaxW = 0;
        int sumArea = 0;

        for (Rectangle rect : open) {
            sumArea += (rect.w * rect.h);
            rectMinW = Math.min(rectMinW,rect.w);
            rectMaxW = Math.max(rectMaxW,rect.w);
        }

        int atlasW = nextPowerOfTwo(Math.max(rectMaxW, rectMaxH));
        int atlasH = atlasW;

        // Area estimation loop: Favors growing horizontally first
        while ((atlasW * atlasH) < sumArea) {
            if (atlasW == atlasH) {
                atlasW *= 2;
            } else atlasH = atlasW;
        }

        if (atlasW > ESTIMATE_MAX || atlasH > ESTIMATE_MAX) {
            Logger.warn("Unreasonable atlas size estimate: ({}x{})",atlasW,atlasH);
            sizeDst.zero();
            return List.of();
        }

        // =============================================================================
        // Packing Begins
        // =============================================================================
        List<Region> closedList = new ArrayList<>(open.size());
        RegionStack rowRegions = new RegionStack(open.size());
        boolean packed = false;

        while (!packed) {
            packed = true;
            Strip gaps = null;
            closedList.clear();
            rowRegions.clear();

            int posX = 0;
            int posY = 0;
            int rowH = 0;

            for (Rectangle rect : open) {

                // REACHED THE END OF A ROW
                if (posX + rect.w > atlasW) {
                    // Check for any horizontal gap left at the end of the current row
                    int leftoverWidth = atlasW - posX;
                    if (leftoverWidth >= rectMinW) {
                        Region last = rowRegions.peek();
                        Strip gap = new Strip(posX, posY, leftoverWidth, last.r.h);
                        gaps = gaps == null ? gap : gaps.addStrip(gap);
                    }
                    Strip currentGap = null;
                    while (!rowRegions.isEmpty()) {
                        Region region = rowRegions.pop();
                        int gapHeight = rowH - region.r.h;
                        if (gapHeight >= rectMinH) {
                            int x = region.r.x;
                            int y = posY + region.r.h;
                            int w = region.r.w;
                            // Try to horizontally merge this gap with the adjacent one we just processed
                            if (currentGap != null && currentGap.height == gapHeight) {
                                currentGap.width += w;
                                currentGap.x = x;
                            } else { // Either it's the first gap, or the height changed, so spawn a new strip
                                currentGap = new Strip(x, y, w, gapHeight);
                                gaps = gaps == null ? currentGap : gaps.addStrip(currentGap);
                            }
                        } else { currentGap = null; // Break the chain if a rectangle leaves no usable gap
                        }
                        closedList.add(region);
                    } // prepare for next row.
                    posY += rowH;
                    posX = 0;
                    rowH = 0;
                }

                // VERTICAL OVERFLOW RESIZE CHECK
                if (posY + rect.h > atlasH) {
                    if (atlasH > atlasW) {
                        packed = false;
                        break;
                    } atlasH *= 2; // Favor height growth
                }

                Region region = new Region(rect.id, new TextureRegion(rect.w,rect.h));
                // Attempt to fit inside an available gap
                if (gaps != null && gaps.tryAddRect(region,closedList)) continue;
                // Otherwise, place it natively onto the current running row line
                rowH = Math.max(rect.h,rowH);
                region.r.setPosition(posX,posY);
                rowRegions.push(region); // push rectangle on row
                posX += region.r.w;
            }

            if (packed) {
                sizeDst.set(atlasW, atlasH);
                while (!rowRegions.isEmpty())
                    closedList.add(rowRegions.pop());
            } else {
                atlasW *= 2;
                atlasH = atlasW;
            }
        }
        return closedList;
    }

    public record Rectangle(int id, int w, int h) implements Comparable<Rectangle> {
        /* sorted by max h -> max w */
        public int compareTo(Rectangle o) {
            int c = Integer.compare(o.h, h);
            return c == 0 ? Integer.compare(o.w, w) : c;
        } public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Rectangle other = (Rectangle) obj;
            return id == other.id;
        } public int hashCode() {
            return Integer.hashCode(id);
        } public String toString() {
            return "("+id+", "+ w +", "+ h +")";
        }
    }

    public record Region(int id, TextureRegion r) {
        public String toString() { return "("+id+", "+ r +")"; }
    }


    private static final class Strip {
        Strip next;
        int x, y;
        int width;
        int height;
        int usedWidth;

        Strip(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        boolean tryAddRect(Region region, List<Region> list) {
            if (region.r.h > height) return false;
            int remaining = width - usedWidth;
            if (region.r.w <= remaining) {
                region.r.x = x + usedWidth;
                region.r.y = y;
                usedWidth += region.r.w;
                list.add(region);
                return true;
            } if (next == null) return false;
            return next.tryAddRect(region,list);
        }

        Strip addStrip(Strip strip) {
            // If the incoming strip is taller than the current head,
            // it immediately becomes the new head of the list.
            if (strip.height > this.height) {
                strip.next = this;
                return strip;
            }
            // Otherwise, walk the list iteratively to find its sorted destination
            Strip current = this;
            while (current.next != null && current.next.height >= strip.height) {
                current = current.next;
            }
            // Insert the new strip smoothly into its position in the chain
            strip.next = current.next;
            current.next = strip;
            // The original head remains unchanged, so return 'this'
            return this;
        }
    }

    private static final class RegionStack {
        private final Region[] items;
        private int size;

        public RegionStack(int capacity) {
            this.items = new Region[Math.max(capacity, 1)];
        }
        public void push(Region item) {
            if (item == null) {
                throw new IllegalArgumentException("Cannot push a null Region.");
            } if (size == items.length) {
                throw new IndexOutOfBoundsException("Stack overflow: Fixed capacity of " + items.length + " exceeded.");
            } items[size++] = item;
        }
        public Region pop() {
            if (size == 0) {
                throw new NoSuchElementException("Stack underflow: cannot pop from an empty stack.");
            } Region item = items[--size];
            items[size] = null;
            return item;
        }
        public Region peek() {
            if (size == 0) {
                throw new NoSuchElementException("Cannot peek at an empty stack.");
            } return items[size - 1];
        }
        public int size() {
            return size;
        }
        public boolean isEmpty() {
            return size == 0;
        }
        public void clear() {
            while (size > 0) {
                items[--size] = null;
            }
        }
    }


}
