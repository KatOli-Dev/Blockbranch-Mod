package blockbranch.client.wildlife;
import blockbranch.Blockbranch;
import blockbranch.wildlife.BrownBear;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
final class BearRenderer extends MobRenderer<BrownBear,BearRenderer.State,BearModel> {
    static final class State extends LivingEntityRenderState {int activity;float attack;}
    BearRenderer(EntityRendererProvider.Context context){super(context,new BearModel(BearModel.layer().bakeRoot()),.7F);}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State state){return Blockbranch.id("textures/entity/brown_bear"+(state.isBaby?"_cub":"")+".png");}
    @Override public void extractRenderState(BrownBear bear,State state,float partialTick){super.extractRenderState(bear,state,partialTick);state.activity=bear.activity();state.attack=bear.getSwingAnimation(partialTick);}
    @Override protected void scale(State state,com.mojang.blaze3d.vertex.PoseStack pose){pose.scale(state.ageScale,state.ageScale,state.ageScale);}
}
