package blockbranch.realistic;
import blockbranch.Blockbranch;import com.mojang.serialization.MapCodec;import net.minecraft.core.*;import net.minecraft.core.registries.*;import net.minecraft.world.level.levelgen.structure.*;import java.util.*;
public final class ReimaginedCryptStructure extends Structure {
 public static final MapCodec<ReimaginedCryptStructure> CODEC=simpleCodec(ReimaginedCryptStructure::new);
 public static final StructureType<ReimaginedCryptStructure> TYPE=Registry.register(BuiltInRegistries.STRUCTURE_TYPE,Blockbranch.id("reimagined_crypt_v1"),()->CODEC);
 public static void initialize(){ReimaginedLayoutPiece.initialize();ReimaginedTreeClearPiece.initialize();ReimaginedMansionPiece.initialize();ReimaginedSanctuaryPiece.initialize();}
 public ReimaginedCryptStructure(StructureSettings settings){super(settings);}
 @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext c){if(!(c.chunkGenerator() instanceof RealisticGenerator g)||g.reimaginedDefinition()==0)return Optional.empty();return g.reimagined(c.randomState()).forStart(ReimaginedSites.CRYPT,c.chunkPos().x(),c.chunkPos().z()).map(p->new GenerationStub(p.center(),b->b.addPiece(ReimaginedLayoutPiece.crypt(p,g))));}
 @Override public StructureType<?> type(){return TYPE;}
}
