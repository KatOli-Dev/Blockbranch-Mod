package blockbranch.mixin.qa;

import blockbranch.realistic.AdmissionWorkTrace;
import blockbranch.realistic.RealisticGenerator;
import blockbranch.cubic.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;
import java.util.*;

@Mixin(RealisticGenerator.class)
abstract class AdmissionWorkMixin {
    @WrapMethod(method="profile")
    private TerrainProfile profile(ChunkAccess chunk, RandomState random, Operation<TerrainProfile> original) {
        var s=AdmissionWorkTrace.beginProfile(chunk,random.seed()); boolean ok=false;
        try {blockbranch.realistic.AdmissionPreparationChecks.injectFailure(chunk);var value=original.call(chunk,random);ok=true;return value;} finally {AdmissionWorkTrace.end(s,ok);}
    }
    @WrapMethod(method="canopy(Lblockbranch/realistic/RealisticGenerator$TerrainState;IIZZILjava/util/function/BiPredicate;Ljava/util/function/BiPredicate;Ljava/util/function/BiPredicate;Z)Lblockbranch/cubic/CanopyLayer;")
    private static CanopyLayer canopy(@Coerce Object state,int x,int z,boolean icebergs,boolean whorled,int transitions,java.util.function.BiPredicate<Integer,Integer> rockland,java.util.function.BiPredicate<Integer,Integer> dryForest,java.util.function.BiPredicate<Integer,Integer> summerDry,boolean abyssalTraces,Operation<CanopyLayer> original) {
        var s=AdmissionWorkTrace.begin("canopy",x>>4,z>>4);boolean ok=false;
        try {var value=original.call(state,x,z,icebergs,whorled,transitions,rockland,dryForest,summerDry,abyssalTraces);ok=true;return value;} finally {AdmissionWorkTrace.end(s,ok);}
    }
    @WrapOperation(method="prepareCaveProfile",at=@At(value="INVOKE",target="Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object preparedGet(Map<?,?> map,Object key,Operation<Object> original) {
        var value=original.call(map,key);AdmissionWorkTrace.cache(value==null?"prepared.get.miss":"prepared.get.nonNull",key);return value;
    }
    @WrapOperation(method="prepareCaveProfile",at=@At(value="INVOKE",target="Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object preparedPut(Map<?,?> map,Object key,Object value,Operation<Object> original) {
        var old=original.call(map,key,value);AdmissionWorkTrace.cache("prepared.put",key);return old;
    }
    @WrapOperation(method="prepareCaveProfile",at=@At(value="INVOKE",target="Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object preparedEvict(Map<?,?> map,Object key,Operation<Object> original) {
        var old=original.call(map,key);AdmissionWorkTrace.cache("prepared.evict",key);return old;
    }
    @WrapOperation(method="profileForBuild",at=@At(value="INVOKE",target="Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object preparedConsume(Map<?,?> map,Object key,Operation<Object> original) {
        var old=original.call(map,key);AdmissionWorkTrace.cache(old==null?"prepared.consume.miss":"prepared.consume.nonNull",key);return old;
    }
    @WrapOperation(method="canopy(Lblockbranch/realistic/RealisticGenerator$TerrainState;IIZZILjava/util/function/BiPredicate;Ljava/util/function/BiPredicate;Ljava/util/function/BiPredicate;Z)Lblockbranch/cubic/CanopyLayer;",at=@At(value="INVOKE",target="Ljava/util/LinkedHashMap;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object canopyGet(LinkedHashMap<?,?> map,Object key,Operation<Object> original) {
        var old=original.call(map,key);AdmissionWorkTrace.cache(old==null?"canopy.get.miss":"canopy.get.hit",key);return old;
    }
    @WrapOperation(method="canopy(Lblockbranch/realistic/RealisticGenerator$TerrainState;IIZZILjava/util/function/BiPredicate;Ljava/util/function/BiPredicate;Ljava/util/function/BiPredicate;Z)Lblockbranch/cubic/CanopyLayer;",at=@At(value="INVOKE",target="Ljava/util/LinkedHashMap;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object canopyPut(LinkedHashMap<?,?> map,Object key,Object value,Operation<Object> original) {
        var old=original.call(map,key,value);AdmissionWorkTrace.cache(old==null?"canopy.put.new":"canopy.put.replace",key);return old;
    }
    @WrapOperation(method="canopy(Lblockbranch/realistic/RealisticGenerator$TerrainState;IIZZILjava/util/function/BiPredicate;Ljava/util/function/BiPredicate;Ljava/util/function/BiPredicate;Z)Lblockbranch/cubic/CanopyLayer;",at=@At(value="INVOKE",target="Ljava/util/LinkedHashMap;remove(Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object canopyEvict(LinkedHashMap<?,?> map,Object key,Operation<Object> original) {
        var old=original.call(map,key);AdmissionWorkTrace.cache("canopy.evict",key);return old;
    }
}
