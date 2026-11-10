package blockbranch.realistic;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.shapes.*;
/** Standalone installed-asset/declared-intent smoke check; no game/world edits. */
public final class MansionNativeOracleCheck {
 public static void main(String[]args)throws Exception{
  net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();var oracle=new MansionNativeOracle(Path.of(args[0]),Path.of(args[1]));int checked=0,palette=0;
  for(var e:oracle.expected.entrySet()){var at=e.getKey();if(oracle.roofCommands.containsKey(at))continue;var old=oracle.baseline.get(at).state();var next=e.getValue();if(old==next)continue;palette++;
   if(Shapes.joinIsNotEmpty(old.getCollisionShape(EmptyBlockGetter.INSTANCE,at),next.getCollisionShape(EmptyBlockGetter.INSTANCE,at),BooleanOp.NOT_SAME)||Shapes.joinIsNotEmpty(old.getOcclusionShape(),next.getOcclusionShape(),BooleanOp.NOT_SAME)||old.getFluidState()!=next.getFluidState()||old.getLightEmission()!=next.getLightEmission()||old.getLightDampening()!=next.getLightDampening()||old.propagatesSkylightDown()!=next.propagatesSkylightDown()||old.canOcclude()!=next.canOcclude()||old.useShapeForLightOcclusion()!=next.useShapeForLightOcclusion())throw new AssertionError("Independent declared native physical state "+at+" "+old+" -> "+next);
   int h=at.getY()-oracle.floor;if(h==0||h==8||h==19||old.isAir()||old.hasBlockEntity()||old.getLightEmission()>0)throw new AssertionError("Intended palette writes protected floor/free/feature "+at);checked++;
  }
  if(oracle.expected.size()!=149389||oracle.baseline.size()!=473473||oracle.originalNativePieces.size()!=602||oracle.roofCommands.size()!=JsonParser.parseString(Files.readString(Path.of(args[1]).resolveSibling("provenance.json"))).getAsJsonObject().get("commands").getAsInt()||checked==0)throw new AssertionError("Native fixed-site oracle coverage");
  // Removing a nonroof air/protection position must never turn it into a roof-write exception.
  int protectedCommands=0;for(var p:oracle.roofCommands.keySet())if(oracle.nativeProtection.contains(p))protectedCommands++;if(protectedCommands!=0)throw new AssertionError("Nonroof protection command overlap");
  System.out.println("PASS independent native states="+oracle.expected.size()+" paletteChanges="+palette+" equivalentPhysicalMappings="+checked+" nativeWalkingFeet="+oracle.nativeWalkFeet.size()+" nonroofProtection="+oracle.nativeProtection.size()+" exactFrozenRoofCommands="+oracle.roofCommands.size());
 }
}
