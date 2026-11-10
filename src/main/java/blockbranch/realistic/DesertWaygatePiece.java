package blockbranch.realistic;

import blockbranch.Blockbranch;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

/** A clipped, fixed-height native template piece; no mutable spread outside its saved bounds. */
public final class DesertWaygatePiece extends TemplateStructurePiece {
 public static final StructurePieceType TYPE=Registry.register(BuiltInRegistries.STRUCTURE_PIECE,Blockbranch.id("desert_waygate_v1"),(StructurePieceType)DesertWaygatePiece::new);
 public static void initialize(){}
 public DesertWaygatePiece(StructureTemplateManager manager,DesertWaygates.Plan p){super(TYPE,0,manager,Blockbranch.id("ruined_portal/desert_waygate_v1_"+p.variant()),"blockbranch:ruined_portal/desert_waygate_v1_"+p.variant(),settings(p.turn()),p.origin());}
 private DesertWaygatePiece(StructurePieceSerializationContext context,CompoundTag tag){super(TYPE,tag,context.structureTemplateManager(),id->settings(Rotation.valueOf(tag.getStringOr("Rot","NONE"))));}
 private static StructurePlaceSettings settings(Rotation turn){return new StructurePlaceSettings().setRotation(turn).setRotationPivot(new BlockPos(6,0,6)).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);}
 @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag){super.addAdditionalSaveData(context,tag);tag.putString("Rot",getRotation().name());}
 @Override protected void handleDataMarker(String name,BlockPos pos,net.minecraft.world.level.ServerLevelAccessor level,net.minecraft.util.RandomSource random,BoundingBox bounds){
  if(name.equals("chest")&&bounds.isInside(pos)){var chestPos=pos.below();if(level.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)chest.setLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.RUINED_PORTAL,random.nextLong());}
 }
}
