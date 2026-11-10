package blockbranch.cubic;
import java.io.IOException;
import java.nio.file.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
/** Refuse existing unmarked saves before loading any levels or interpreting packed positions. */
public final class ColumnWorldFormat {
    private static final String CONTENT="blockbranch-columns=2\nblock-position-bits=22,20,22\n";
    private static final String PREVIOUS="blockbranch-columns=1\nblock-position-bits=22,20,22\n";
    // Reuse the marker older releases already reject, rather than add an ignored sidecar.
    private static final String MOSS="blockbranch-columns=3\nblock-position-bits=22,20,22\n";
    public static void check(MinecraftServer server) {
        boolean moss=server.getWorldGenSettings().dimensions().dimensions().values().stream()
            .anyMatch(stem->stem.generator() instanceof blockbranch.realistic.RealisticGenerator g&&(g.litCaveGrowthDefinition()>0||g.dripstoneDefinition()==2));
        check(server.getWorldPath(LevelResource.ROOT),server.getWorldData().overworldData().isInitialized(),moss,
            ()->{if(moss)persistMossSettings(server);});
    }
    public static void check(Path root,boolean initialized,boolean moss) {
        check(root,initialized,moss,()->{});
    }
    private static void persistMossSettings(MinecraftServer server) {
        // Older binaries can rewrite metadata during shutdown after a marker rejection.
        // Their existing generator codec must reject schema83 before constructing a server.
        var settings=server.getWorldGenSettings();
        settings.setDirty();server.getDataStorage().saveAndJoin();
        var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,server.registryAccess());
        var expected=net.minecraft.world.level.levelgen.WorldGenSettings.CODEC.encodeStart(ops,settings).getOrThrow();
        try {
            var saved=net.minecraft.nbt.NbtIo.readCompressed(server.getWorldPath(LevelResource.ROOT).resolve("data/minecraft/world_gen_settings.dat"),net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            if(!saved.getCompoundOrEmpty("data").equals(expected))throw new IOException("Generator compatibility barrier was not persisted");
        } catch(IOException failure) {throw new IllegalStateException("Cannot establish moss save compatibility before loading levels",failure);}
    }
    private static void check(Path root,boolean initialized,boolean moss,Runnable beforeCommit) {
        Path marker=root.resolve("blockbranch-columns.txt");
        String required=moss?MOSS:CONTENT;
        try {
            if(Files.exists(marker)) {
                var previous=Files.readString(marker);
                if(previous.equals(MOSS)&&!moss)throw new IllegalStateException("Versioned-decoration marker requires its saved generator settings; restoring older settings is unsupported");
                if(!previous.equals(PREVIOUS)&&!previous.equals(CONTENT)&&!previous.equals(MOSS))throw new IllegalStateException("Unsupported Blockbranch world format: "+marker);
                beforeCommit.run();
                if(!previous.equals(required)) {
                    Path temporary=root.resolve("blockbranch-columns.upgrading");
                    Files.writeString(temporary,required);
                    try { Files.move(temporary,marker,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
                    catch(AtomicMoveNotSupportedException unsupported) { Files.move(temporary,marker,StandardCopyOption.REPLACE_EXISTING); }
                }
            } else {
                if(initialized) throw new IllegalStateException("This save has no Blockbranch column format marker. Existing-world import is not supported; create a new world.");
                beforeCommit.run();
                Files.createDirectories(root);
                Files.writeString(marker,required,StandardOpenOption.CREATE_NEW);
            }
        } catch(IOException failure) { throw new IllegalStateException("Cannot validate Blockbranch world format",failure); }
    }
    private ColumnWorldFormat() {}
}
