package blockbranch.client.wildlife;

import blockbranch.Blockbranch;
import blockbranch.wildlife.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Mob;

/** Measures actual renderer transforms and baked cuboids in world blocks. */
public final class WildlifeScaleAudit {
    public static void measure(Minecraft client,Mob animal) {
        boolean boar=animal instanceof WildBoar;
        ModelPart root;LivingEntityRenderState state;var pose=new PoseStack();
        if(boar){
            var renderer=(BoarRenderer)client.getEntityRenderDispatcher().getRenderer((WildBoar)animal);
            var s=renderer.createRenderState();renderer.extractRenderState((WildBoar)animal,s,0);s.yRot=0;s.xRot=0;s.activity=0;
            root=BoarModel.layer().bakeRoot();new BoarModel(root).setupAnim(s);state=s;
            pose.scale(s.scale,s.scale,s.scale);pose.scale(-1,-1,1);renderer.scale(s,pose);
        }else{
            var renderer=(DeerRenderer)client.getEntityRenderDispatcher().getRenderer((RedDeer)animal);
            var s=renderer.createRenderState();renderer.extractRenderState((RedDeer)animal,s,0);s.yRot=0;s.xRot=0;s.activity=0;
            root=DeerModel.layer().bakeRoot();new DeerModel(root).setupAnim(s);state=s;
            pose.scale(s.scale,s.scale,s.scale);pose.scale(-1,-1,1);renderer.scale(s,pose);
        }
        pose.translate(0,-1.501F,0);
        float[] bounds={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        float[] shoulder={Float.NEGATIVE_INFINITY};
        root.visit(pose,(matrix,path,index,cube)->{
            if(boar&&state.isBaby&&path.contains("tusks"))return;
            if(!boar&&path.contains("antler")&&(state.isBaby||!((RedDeer)animal).hasAntlers()))return;
            for(float x:new float[]{cube.minX,cube.maxX})for(float y:new float[]{cube.minY,cube.maxY})for(float z:new float[]{cube.minZ,cube.maxZ}){
                var point=matrix.pose().transformPosition(new org.joml.Vector3f(x/16,y/16,z/16));
                for(int axis=0;axis<3;axis++){bounds[axis]=Math.min(bounds[axis],point.get(axis));bounds[axis+3]=Math.max(bounds[axis+3],point.get(axis));}
                if(path.equals("/body"))shoulder[0]=Math.max(shoulder[0],point.y);
            }
        });
        float age=animal.isBaby()?.5F:1;
        if(Math.abs(state.ageScale-age)>.00001||Math.abs(state.scale-1)>.00001)throw new AssertionError("Unexpected native wildlife scale");
        if(Math.abs(bounds[1]-.001F*age)>.00001)throw new AssertionError("Wildlife feet shifted from native model floor "+bounds[1]);
        if(boar&&Math.abs((bounds[4]-bounds[1])-age)>.0001)throw new AssertionError("Boar rendered height conversion changed");
        Blockbranch.LOGGER.info("WILDLIFE_SCALE name={} baby={} modelUnit=1/16 attributeScale={} ageScale={} renderedBounds={} shoulder={} hitboxWidth={} hitboxHeight={} feetY={}",
            animal.getName().getString(),animal.isBaby(),state.scale,state.ageScale,java.util.Arrays.toString(bounds),shoulder[0],animal.getBbWidth(),animal.getBbHeight(),animal.getY());
    }
    private WildlifeScaleAudit(){}
}
