package blockbranch.client.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.ColumnLimits;
import blockbranch.cubic.SparseColumn;
import blockbranch.cubic.TerrainProfile;
import blockbranch.realistic.TerrainShape;
import java.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/** Opt-in fixture coverage; never changes terrain or normal rendering traversal. */
final class FixtureSceneReadiness {
    private static long frames;
    private static boolean registered;
    FixtureSceneReadiness() {
        if (!registered) { LevelRenderEvents.END_MAIN.register(context -> frames++); registered = true; }
    }

    private record Surface(int min, int max, Set<Integer> tops) {}
    private final IdentityHashMap<TerrainProfile, Surface> surfaces = new IdentityHashMap<>();
    private final IdentityHashMap<TerrainProfile, Surface> seafloors = new IdentityHashMap<>();
    private final Set<Long> submitted = new HashSet<>();
    private String scene = "", diagnostic = "not checked";
    private net.minecraft.world.phys.Vec3 position;
    private float yaw, pitch;
    private long readyFrame = -1, revision, nextProgress;

    String diagnostic() { return diagnostic; }

    boolean ready(Minecraft client, String name) {
        return ready(client,name,false,false);
    }

    /** Open surface views do not expose buried cave boundaries through solid ground. */
    boolean readySurface(Minecraft client,String name) {
        return ready(client,name,true,false);
    }

    /** Open shore views include all in-window seabed elevations through water. */
    boolean readyShore(Minecraft client,String name) {
        return ready(client,name,true,true);
    }

    private boolean ready(Minecraft client,String name,boolean surfaceOnly,boolean shore) {
        if (client.level == null || client.player == null || client.levelRenderer.viewArea() == null) return false;
        var area = client.levelRenderer.viewArea();
        var now = client.player.position();
        boolean moved = position == null || now.distanceToSqr(position) > .000001
            || Math.abs(net.minecraft.util.Mth.wrapDegrees(client.player.getYRot() - yaw)) > .01
            || Math.abs(client.player.getXRot() - pitch) > .01;
        if (!scene.equals(name) || moved) {
            scene = name; position = now; yaw = client.player.getYRot(); pitch = client.player.getXRot();
            readyFrame = -1; surfaces.clear(); seafloors.clear(); submitted.clear();
        }
        if (revision != ColumnReceiveBatch.received) { revision = ColumnReceiveBatch.received; readyFrame = -1; }
        var frustum = client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.cullFrustum;
        int radius = area.getViewDistance(), cameraX = client.player.blockPosition().getX() >> 4,
            cameraZ = client.player.blockPosition().getZ() >> 4;
        int columns = 0, loaded = 0, sections = 0, compiled = 0, empty = 0, regionsPending = 0;
        var missing = new ArrayList<BlockPos>();
        var examples = new ArrayList<String>();
        for (int dz = -radius; dz <= radius; dz++) for (int dx = -radius; dx <= radius; dx++) {
            if (dx * dx + dz * dz > radius * radius) continue;
            int cx = cameraX + dx, cz = cameraZ + dz;
            if (!frustum.isVisible(new AABB(cx * 16, TerrainShape.FLOOR, cz * 16,
                    cx * 16 + 16, TerrainShape.CEILING, cz * 16 + 16))) continue;
            columns++;
            var chunk = client.level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
            var terrain = chunk == null ? null : ((SparseColumn) chunk).blockbranch$terrain();
            if (terrain == null) { example(examples, cx + "," + cz + ":column"); continue; }
            loaded++;
            var base = terrain.base();
            var surface = surfaces.computeIfAbsent(base, FixtureSceneReadiness::surface);
            var seafloor = shore ? seafloors.computeIfAbsent(base,b -> surface(b,Heightmap.Types.OCEAN_FLOOR)) : null;
            // Include every surface elevation, intervening cliff sections, and
            // profile/canopy boundaries below foliage rather than one center Y.
            var required = surfaceOnly ? new TreeSet<Integer>() : new TreeSet<>(base.boundaries());
            if(surfaceOnly)required.addAll(base.canopy().sections());
            for (int sy = surface.min; sy <= surface.max; sy++) required.add(sy);
            // Buried cave boundaries are not shore surfaces. Seabeds are real
            // visible surfaces, bounded by the renderer's actual vertical window.
            if(seafloor!=null)for(int sy=Math.max(seafloor.min,area.minSectionY());sy<=Math.min(seafloor.max,area.maxSectionY());sy++)required.add(sy);
            for (int sy : required) {
                if (!frustum.isVisible(new AABB(cx * 16, sy * 16, cz * 16,
                        cx * 16 + 16, sy * 16 + 16, cz * 16 + 16))) continue;
                sections++;
                var pos = new BlockPos(cx * 16 + 8, sy * 16 + 8, cz * 16 + 8);
                var section = area.getRenderSectionAt(pos);
                String reason = null;
                if (sy < area.minSectionY() || sy > area.maxSectionY()) reason = "outside-window";
                else if (section == null || section.getSectionNode() != SectionPos.asLong(cx, sy, cz)) reason = "slot";
                else {
                    var mesh = section.getSectionMesh();
                    if (mesh == CompiledSectionMesh.UNCOMPILED) {
                        reason = submitted.contains(SectionPos.asLong(cx, sy, cz)) ? "uncompiled-after-submit" : "uncompiled";
                        missing.add(pos);
                    }
                    else if (!mesh.hasRenderableLayers() && (surface.tops.contains(sy) || base.canopy().sections().contains(sy) || seafloor!=null&&seafloor.tops.contains(sy))) reason = "empty-surface";
                    else { compiled++; if (!mesh.hasRenderableLayers()) empty++; }
                }
                if (reason != null) example(examples, cx + "," + sy + "," + cz + ":" + reason);
            }
        }
        // Existing collateral fixtures already use native compileSync for
        // required sections omitted by visibility traversal. Keep this bounded,
        // explicit and fixture-only; readiness still observes published meshes
        // on subsequent rendered frames, not merely submitted compile work.
        if (loaded == columns && columns > 0 && !moved) {
            var cache = new RenderRegionCache(); int count = 0;
            for (var pos : missing) {
                long node = SectionPos.asLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
                if (submitted.contains(node)) continue;
                var region = cache.createRegion(client.level, node);
                if (region == null) { regionsPending++; continue; }
                area.getRenderSectionAt(pos).compileSync(region); submitted.add(node);
                if (++count == 2) break;
            }
        }
        boolean queueEmpty = client.levelRenderer.hasRenderedAllSections();
        diagnostic = "scene=" + name + " columns=" + loaded + "/" + columns + " sections=" + compiled + "/" + sections
            + " emptyInterior=" + empty + " fixtureCompiles=" + submitted.size() + " regionsPending=" + regionsPending + " queueEmpty=" + queueEmpty
            + " pose=" + now + "," + yaw + "," + pitch + " window=" + area.minSectionY() + ".." + area.maxSectionY()
            + " frame=" + frames + " readyFrame=" + readyFrame + " missing=" + examples;
        boolean complete = columns > 0 && loaded == columns && sections > 0 && compiled == sections && queueEmpty;
        if (!complete) readyFrame = -1;
        else if (readyFrame < 0) readyFrame = frames;
        if (System.nanoTime() > nextProgress) {
            nextProgress = System.nanoTime() + 10_000_000_000L;
            Blockbranch.LOGGER.info("FIXTURE_CAPTURE_READY {}", diagnostic);
        }
        return complete && frames - readyFrame >= 2;
    }

    private static void example(List<String> examples, String value) { if (examples.size() < 8) examples.add(value); }
    private static Surface surface(TerrainProfile base) { return surface(base,Heightmap.Types.WORLD_SURFACE); }
    private static Surface surface(TerrainProfile base,Heightmap.Types heightmap) {
        var tops = new HashSet<Integer>(); int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            int y = base.top(heightmap.isOpaque(), x, z, ColumnLimits.MAX_Y - 1) - 1, sy = y >> 4;
            // Snow can fully occlude the lower floor section while rendering in
            // the next one. Only exposed floor tops demand nonempty meshes.
            if(heightmap!=Heightmap.Types.OCEAN_FLOOR||net.minecraft.world.level.block.Block.shouldRenderFace(
                base.state(x,y,z),base.state(x,y+1,z),net.minecraft.core.Direction.UP))tops.add(sy);
            min = Math.min(min, sy); max = Math.max(max, sy);
        }
        return new Surface(min, max, Set.copyOf(tops));
    }
}
