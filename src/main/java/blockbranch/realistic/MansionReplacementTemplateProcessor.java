package blockbranch.realistic;

import blockbranch.Blockbranch;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/** Pure state treatment inside the original native template/clip. No world or RNG access. */
public final class MansionReplacementTemplateProcessor implements StructureProcessor {
    public static final String DEFINITION_TAG="MansionReplacement",FLOOR_TAG="MansionReplacementFloor";
    public static final MapCodec<MansionReplacementTemplateProcessor> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            Codec.STRING.validate(name->validTemplate(name)?DataResult.success(name):DataResult.error(()->"Invalid mansion template name")).fieldOf("template").forGetter(p->p.template),
            Codec.intRange(TerrainShape.FLOOR+64,TerrainShape.CEILING-160).fieldOf("floor").forGetter(p->p.floor)
    ).apply(i,MansionReplacementTemplateProcessor::new));
    private static boolean initialized;
    private final String template;
    private final int floor;

    public static synchronized void initialize() {
        if(initialized)return;
        Registry.register(BuiltInRegistries.STRUCTURE_PROCESSOR,Blockbranch.id("mansion_replacement_template_v1"),CODEC);
        initialized=true;
    }
    public MansionReplacementTemplateProcessor(String template,int floor) {
        if(!validTemplate(template))throw new IllegalArgumentException("Invalid mansion template name");
        validateFloor(floor);
        this.template=template;this.floor=floor;
    }
    public static int readDefinition(CompoundTag tag) {
        if(tag.contains(DEFINITION_TAG)&&!(tag.get(DEFINITION_TAG) instanceof IntTag))throw new IllegalArgumentException("Invalid mansion replacement tag");
        int definition=tag.getIntOr(DEFINITION_TAG,0);
        if(definition<0||definition>1)throw new IllegalArgumentException("Unknown mansion replacement definition");
        return definition;
    }
    public static int readFloor(CompoundTag tag,int templateY) {
        if(!(tag.get(FLOOR_TAG) instanceof IntTag))throw new IllegalArgumentException("Missing or invalid mansion replacement floor");
        int floor=tag.getIntOr(FLOOR_TAG,0);validateConfiguration(1,floor,templateY);return floor;
    }
    public static void validateConfiguration(int definition,int floor,int templateY) {
        if(definition<0||definition>1)throw new IllegalArgumentException("Unknown mansion replacement definition");
        if(definition==0)return;
        validateFloor(floor);
        long delta=(long)templateY-floor;
        if(delta<0||delta>64)throw new IllegalArgumentException("Mansion replacement floor is not near native template");
    }
    private static void validateFloor(int floor) {
        if(floor<TerrainShape.FLOOR+64||floor>TerrainShape.CEILING-160)throw new IllegalArgumentException("Mansion replacement floor outside admitted world");
    }
    private static boolean validTemplate(String name){return name!=null&&!name.isEmpty()&&name.length()<=256;}
    @Override public StructureBlockInfo processBlock(LevelReader world,BlockPos origin,BlockPos pivot,BlockPos local,StructureBlockInfo info,StructurePlaceSettings settings) {
        if(info.nbt()!=null)return info;
        // Positions are already transformed, but native placement mirrors/rotates states later.
        var state=MansionTemplateArchitecture.apply(template,local,info.state(),info.pos().getY(),floor);
        return state==info.state()?info:new StructureBlockInfo(info.pos(),state,null);
    }
    @Override public MapCodec<? extends StructureProcessor> codec(){return CODEC;}
}
