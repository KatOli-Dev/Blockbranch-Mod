import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/**
 * Original 16x tool sprites and pixel armor on the native humanoid UVs for every metal equipment set.
 * No existing texture is read. Committed sprites are approved assets: run this only to add a new set, and
 * write elsewhere first to compare.
 *
 * <pre>java scripts/assets/GenerateEquipmentArt.java [--out DIR] [metal ...]</pre>
 * DIR defaults to {@code src/main/resources/assets/blockbranch/textures}; metals default to all.
 */
public class GenerateEquipmentArt {
    /**
     * One equipment set. Tool art uses dark/metal/light; armor plates use plateLight/plateDark edges. Item and
     * equipment names are the published registry paths.
     */
    record Metal(String name, String items, String equipment, int dark, int metal, int light, int plateLight, int plateDark,
                 boolean pickaxe, int armorPieces) {}

    static final List<Metal> METALS = List.of(
        new Metal("bronze", "bronze", "bronze", 0x704421, 0xb6753d, 0xe0aa61, 0xd99e56, 0x8a532e, false, 4),
        new Metal("steel", "steel", "steel", 0x35414c, 0x89949f, 0xcbd4dc, 0xcbd4dc, 0x515c68, false, 4),
        new Metal("stainless", "stainless_steel", "stainless_steel", 0x465965, 0xb5c5cd, 0xedf4f6, 0xedf4f6, 0x697b88, false, 4),
        new Metal("silver", "silver", "silver", 0x655f77, 0xc4c0d4, 0xf5f1ff, 0xf5f1ff, 0x8b819f, false, 4),
        new Metal("platinum", "platinum_alloy", "platinum_alloy", 0x817b75, 0xd2cbbc, 0xf8f0df, 0xf8f0df, 0xa6a0b8, false, 4),
        // Titanium alloy also has a pickaxe; its boots predate this template and are not drawn here.
        new Metal("titanium", "titanium_alloy", "titanium_alloy_gear", 0x6c8088, 0xb7c6cc, 0xedf5f4, 0xedf5f4, 0x9bafc2, true, 3));

    public static void main(String[] args) throws Exception {
        var out = Path.of("src/main/resources/assets/blockbranch/textures");
        var selected = new ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--out")) out = Path.of(args[++i]);
            else selected.add(args[i]);
        }
        for (var metal : METALS) {
            if (!selected.isEmpty() && !selected.contains(metal.name())) continue;
            tools(metal, out);
            armor(metal, out);
        }
    }

    private static void p(BufferedImage image, int x, int y, int color) {
        if (x >= 0 && x < 16 && y >= 0 && y < 16) image.setRGB(x, y, 0xff000000 | color);
    }

    static void tools(Metal m, Path out) throws Exception {
        int dark = m.dark(), metal = m.metal(), light = m.light();
        var types = m.pickaxe() ? new String[]{"pickaxe", "sword", "axe", "shovel", "hoe"} : new String[]{"sword", "axe", "shovel", "hoe"};
        for (String type : types) {
            var image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int i = 0; i < 10; i++) { p(image, 3 + i, 13 - i, 0x543a23); p(image, 4 + i, 13 - i, 0x987043); }
            switch (type) {
                case "pickaxe" -> {
                    for (int x = 3; x <= 12; x++) { p(image, x, 2, dark); p(image, x, 3, x < 6 ? metal : light); p(image, x, 4, metal); p(image, x, 5, dark); }
                    for (int y = 4; y <= 7; y++) { p(image, 12, y, light); p(image, 13, y, dark); }
                    p(image, 12, 8, dark);
                }
                case "sword" -> {
                    // Short grip, crossguard and a tapered diagonal blade.
                    for (int y = 0; y < 10; y++) for (int x = 7; x < 16; x++) image.setRGB(x, y, 0);
                    for (int i = 0; i < 8; i++) { int x = 6 + i, y = 9 - i; p(image, x, y, dark); p(image, x + 1, y, light); p(image, x + 1, y + 1, metal); }
                    for (int i = 0; i < 5; i++) { p(image, 3 + i, 8 + i, dark); p(image, 4 + i, 8 + i, light); }
                    p(image, 2, 14, dark); p(image, 3, 14, metal);
                }
                case "axe" -> {
                    for (int y = 2; y <= 8; y++) for (int x = 8; x <= 14; x++) {
                        if ((y == 2 && x > 12) || (y == 8 && x < 11) || (x == 8 && y > 5)) continue;
                        p(image, x, y, x == 14 || y == 8 ? dark : y < 4 || x == 13 ? light : metal);
                    }
                }
                case "shovel" -> {
                    for (int y = 1; y <= 7; y++) for (int x = 8; x <= 14; x++) {
                        int d = Math.abs(x - 11) + Math.abs(y - 4);
                        if (d > 4) continue;
                        p(image, x, y, d == 4 ? dark : x + y < 15 ? light : metal);
                    }
                }
                case "hoe" -> {
                    for (int x = 3; x <= 12; x++) { p(image, x, 3, dark); p(image, x, 4, light); p(image, x, 5, metal); }
                    for (int y = 4; y <= 8; y++) { p(image, 3, y, light); p(image, 4, y, dark); }
                }
                default -> throw new IllegalArgumentException(type);
            }
            save(image, out, "item/" + m.items() + "_" + type);
        }
    }

    private static final String[][] ARMOR_SHAPES = {
        {"  BBBBBBBBBB  ", " BBBBBBBBBBBB ", "BBB        BBB", "BBB        BBB", "BBB        BBB", "BBB        BBB", "BBB        BBB", "BBB        BBB"},
        {"BBB      BBB", "BBBB    BBBB", " BBBBBBBBBB ", " BBBBBBBBBB ", "  BBBBBBBB  ", "  BBBBBBBB  ", "  BBBBBBBB  ", "  BBBBBBBB  ", "  BBBBBBBB  ", "  BBBBBBBB  "},
        {" BBBBBBBBBB ", " BBBBBBBBBB ", " BBBB  BBBB ", " BBBB  BBBB ", " BBBB  BBBB ", " BBBB  BBBB ", " BBBB  BBBB ", " BBBB  BBBB ", " BBBB  BBBB ", " BBBB  BBBB "},
        {" BBBB  BBBB ", " BBBB  BBBB ", " BBBB  BBBB ", " BBBB  BBBB ", " BBBB  BBBB ", "BBBBB  BBBBB", "BBBBB  BBBBB", "BBBBB  BBBBB"}
    };
    private static final String[] ARMOR_NAMES = {"helmet", "chestplate", "leggings", "boots"};

    static void armor(Metal m, Path out) throws Exception {
        for (int i = 0; i < m.armorPieces(); i++) {
            var icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            var shape = ARMOR_SHAPES[i];
            int ox = (16 - shape[0].length()) / 2, oy = (16 - shape.length) / 2;
            for (int y = 0; y < shape.length; y++) for (int x = 0; x < shape[y].length(); x++) if (shape[y].charAt(x) == 'B') {
                boolean edge = x == 0 || y == 0 || x + 1 == shape[y].length() || y + 1 == shape.length || shape[y].charAt(x - 1) == ' ' || shape[y].charAt(x + 1) == ' ';
                icon.setRGB(ox + x, oy + y, 0xff000000 | (edge ? m.dark() : y < 2 ? m.light() : m.metal()));
            }
            save(icon, out, "item/" + m.items() + "_" + ARMOR_NAMES[i]);
        }
        var outer = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        rect(m, outer, 0, 0, 32, 16); plate(m, outer, 8, 8, 8, 8); clear(outer, 9, 10, 6, 6); clear(outer, 8, 14, 8, 2);
        rect(m, outer, 16, 16, 24, 16); plate(m, outer, 20, 20, 8, 12); plate(m, outer, 32, 20, 8, 12); clear(outer, 22, 20, 4, 2);
        rect(m, outer, 40, 16, 16, 10); plate(m, outer, 44, 20, 4, 6); plate(m, outer, 52, 20, 4, 6);
        rect(m, outer, 0, 27, 16, 5); plate(m, outer, 4, 27, 4, 5); plate(m, outer, 12, 27, 4, 5);
        save(outer, out, "entity/equipment/humanoid/" + m.equipment());
        var inner = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        rect(m, inner, 0, 16, 16, 16); plate(m, inner, 4, 20, 4, 12); plate(m, inner, 12, 20, 4, 12); rect(m, inner, 16, 29, 24, 3);
        save(inner, out, "entity/equipment/humanoid_leggings/" + m.equipment());
        // Native 26.3 baby armor has a separate 64x64 atlas and separate limb islands.
        var baby = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        rect(m, baby, 0, 0, 34, 16); plate(m, baby, 8, 8, 9, 8); clear(baby, 9, 10, 7, 6); clear(baby, 8, 14, 9, 2);
        rect(m, baby, 0, 17, 18, 8); plate(m, baby, 3, 20, 6, 5); rect(m, baby, 0, 36, 18, 5);
        rect(m, baby, 30, 17, 10, 5); plate(m, baby, 33, 20, 2, 2); rect(m, baby, 30, 25, 10, 5); plate(m, baby, 33, 28, 2, 2);
        rect(m, baby, 18, 17, 12, 7); plate(m, baby, 21, 20, 3, 4); rect(m, baby, 18, 24, 12, 7); plate(m, baby, 21, 27, 3, 4);
        rect(m, baby, 0, 25, 12, 4); rect(m, baby, 0, 29, 12, 4);
        save(baby, out, "entity/equipment/humanoid_baby/" + m.equipment());
    }

    private static void rect(Metal m, BufferedImage image, int x, int y, int w, int h) {
        for (int v = y; v < y + h; v++) for (int u = x; u < x + w; u++) image.setRGB(u, v, 0xff000000 | m.metal());
    }
    private static void clear(BufferedImage image, int x, int y, int w, int h) {
        for (int v = y; v < y + h; v++) for (int u = x; u < x + w; u++) image.setRGB(u, v, 0);
    }
    private static void plate(Metal m, BufferedImage image, int x, int y, int w, int h) {
        for (int v = y; v < y + h; v++) for (int u = x; u < x + w; u++)
            image.setRGB(u, v, 0xff000000 | (v == y || u == x ? m.plateLight() : v == y + h - 1 || u == x + w - 1 ? m.plateDark() : m.metal()));
    }
    private static void save(BufferedImage image, Path out, String name) throws Exception {
        var path = out.resolve(name + ".png");
        Files.createDirectories(path.getParent());
        ImageIO.write(image, "png", path.toFile());
    }
}
