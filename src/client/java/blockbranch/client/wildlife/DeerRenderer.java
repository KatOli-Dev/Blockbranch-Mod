package blockbranch.client.wildlife;
import blockbranch.Blockbranch;
import blockbranch.wildlife.RedDeer;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
final class DeerRenderer extends MobRenderer<RedDeer,DeerRenderer.State,DeerModel> {
    static final class State extends LivingEntityRenderState { boolean stag;int activity; }
    DeerRenderer(EntityRendererProvider.Context context){super(context,new DeerModel(DeerModel.layer().bakeRoot()),.55F);}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State state){return Blockbranch.id("textures/entity/red_deer_"+(state.isBaby?"calf":state.stag?"stag":"hind")+".png");}
    @Override public void extractRenderState(RedDeer deer,State state,float partialTick){super.extractRenderState(deer,state,partialTick);state.stag=deer.hasAntlers();state.activity=deer.activity();}
    @Override protected void scale(State state,com.mojang.blaze3d.vertex.PoseStack pose){pose.scale(state.ageScale,state.ageScale,state.ageScale);}
}
