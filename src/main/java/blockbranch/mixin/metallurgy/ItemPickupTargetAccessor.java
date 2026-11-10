package blockbranch.mixin.metallurgy;
import java.util.UUID;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Read the same recipient and delay used by native ItemEntity.playerTouch. */
@Mixin(ItemEntity.class)
public interface ItemPickupTargetAccessor {
    @Accessor("pickupDelay") int blockbranch$pickupDelay();
    @Accessor("target") UUID blockbranch$pickupTarget();
}
