package blockbranch.client.wildlife;
import blockbranch.Blockbranch;
import blockbranch.wildlife.Mallard;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
final class MallardRenderer extends MobRenderer<Mallard,MallardRenderer.State,MallardModel> {
    static final class State extends LivingEntityRenderState {boolean drake;int activity;}
    MallardRenderer(EntityRendererProvider.Context context){super(context,new MallardModel(MallardModel.layer().bakeRoot()),.3F);}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State state){return Blockbranch.id("textures/entity/mallard_"+(state.isBaby?"duckling":state.drake?"drake":"hen")+".png");}
    @Override public void extractRenderState(Mallard animal,State state,float partialTick){super.extractRenderState(animal,state,partialTick);state.drake=animal.isDrake();state.activity=animal.activity();}
    @Override protected void scale(State state,com.mojang.blaze3d.vertex.PoseStack pose){pose.scale(state.ageScale*.65F,state.ageScale*.65F,state.ageScale*.65F);}
}
