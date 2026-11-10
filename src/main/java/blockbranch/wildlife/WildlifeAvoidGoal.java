package blockbranch.wildlife;
import java.util.Comparator;
import java.util.function.Predicate;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;

/** Fear is not an attack permission: passive animals flee on every difficulty. */
final class WildlifeAvoidGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
    WildlifeAvoidGoal(PathfinderMob mob,Class<T> type,float radius,double walk,double sprint) {
        super(mob,type,radius,walk,sprint);
    }
    WildlifeAvoidGoal(PathfinderMob mob,Class<T> type,float radius,double walk,double sprint,Predicate<LivingEntity> predicate) {
        super(mob,type,radius,walk,sprint,predicate);
    }
    @Override public boolean canUse() {
        toAvoid=mob.level().getEntitiesOfClass(avoidClass,mob.getBoundingBox().inflate(maxDist,3,maxDist),
            candidate->candidate.isAlive()&&avoidPredicate.test(candidate)&&predicateOnAvoidEntity.test(candidate)
                &&mob.getSensing().hasLineOfSight(candidate)).stream()
            .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        if(toAvoid==null)return false;
        var away=DefaultRandomPos.getPosAway(mob,16,7,toAvoid.position());
        if(away==null||toAvoid.distanceToSqr(away)<toAvoid.distanceToSqr(mob))return false;
        path=pathNav.createPath(away.x,away.y,away.z,0);
        return path!=null;
    }
}
