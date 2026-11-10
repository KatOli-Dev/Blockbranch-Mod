package blockbranch.realistic;
import net.minecraft.resources.*;import net.minecraft.core.registries.Registries;import net.minecraft.world.level.levelgen.structure.*;
public final class ReimaginedStructures {
 private ReimaginedStructures(){}
 public static ResourceKey<Structure> key(int kind){return switch(kind){case 1->ResourceKey.create(Registries.STRUCTURE,blockbranch.Blockbranch.id("reimagined_crypt"));case 2->BuiltinStructures.DESERT_PYRAMID;case 3->BuiltinStructures.JUNGLE_TEMPLE;case 4->BuiltinStructures.WOODLAND_MANSION;case 5->BuiltinStructures.OCEAN_MONUMENT;default->throw new IllegalArgumentException("Unknown layout kind");};}
}
