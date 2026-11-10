package blockbranch.realistic;
import blockbranch.Blockbranch;import blockbranch.mixin.cubic.ReimaginedPieceCoordinates;import net.minecraft.core.*;import net.minecraft.core.registries.*;import net.minecraft.nbt.CompoundTag;import net.minecraft.world.level.*;import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.level.chunk.ChunkGenerator;import net.minecraft.world.level.levelgen.structure.*;import net.minecraft.world.level.levelgen.structure.pieces.*;import net.minecraft.util.RandomSource;

/** Immutable local architectural commands clipped to native chunk ownership; no additional rewards/residents. */
public final class ReimaginedLayoutPiece extends StructurePiece {
 public static final StructurePieceType TYPE=Registry.register(BuiltInRegistries.STRUCTURE_PIECE,Blockbranch.id("reimagined_layout_v1"),(StructurePieceType)ReimaginedLayoutPiece::new);
 public static void initialize(){}
 private final int kind,ox,oy,oz,xx,xz,zx,zz,width,depth,caveDirection,entryHeight;private int stonework=1,pyramidGallery,cryptStonework,jungleReplacement,pyramidReplacement,cryptReplacement;private final long seed;private boolean nativeCrypt;
 private ReimaginedLayoutPiece(int kind,BlockPos origin,int xx,int xz,int zx,int zz,int width,int depth,int caveDirection,long seed,int entryHeight){super(TYPE,0,bounds(kind,origin,xx,xz,zx,zz,width,depth));this.kind=kind;ox=origin.getX();oy=origin.getY();oz=origin.getZ();this.xx=xx;this.xz=xz;this.zx=zx;this.zz=zz;this.width=width;this.depth=depth;this.caveDirection=caveDirection;this.seed=seed;this.entryHeight=entryHeight;}
 private ReimaginedLayoutPiece(StructurePieceSerializationContext c,CompoundTag t){super(TYPE,t);if(t.getIntOr("LayoutDefinition",0)!=1)throw new IllegalArgumentException("Unknown reimagined layout");stonework=t.getIntOr("JungleStonework",1);if(stonework<1||stonework>2)throw new IllegalArgumentException("Unknown jungle stonework");pyramidGallery=t.contains("PyramidGallery")?t.getIntOr("PyramidGallery",-1):0;kind=t.getIntOr("Kind",0);if(t.contains("PyramidReplacement")&&!(t.get("PyramidReplacement") instanceof net.minecraft.nbt.IntTag))throw new IllegalArgumentException("Invalid pyramid replacement tag");pyramidReplacement=t.getIntOr("PyramidReplacement",0);if(pyramidReplacement<0||pyramidReplacement>1||pyramidReplacement>0&&kind!=2)throw new IllegalArgumentException("Invalid pyramid replacement definition");if(t.contains("JungleReplacement")&&!(t.get("JungleReplacement") instanceof net.minecraft.nbt.IntTag))throw new IllegalArgumentException("Invalid jungle replacement tag");jungleReplacement=t.getIntOr("JungleReplacement",0);if(jungleReplacement<0||jungleReplacement>1||jungleReplacement>0&&kind!=3)throw new IllegalArgumentException("Invalid jungle replacement definition");if(t.contains("CryptStonework")&&!(t.get("CryptStonework") instanceof net.minecraft.nbt.IntTag))throw new IllegalArgumentException("Invalid crypt stonework tag");cryptStonework=t.getIntOr("CryptStonework",0);if(cryptStonework<0||cryptStonework>1||cryptStonework>0&&kind!=1)throw new IllegalArgumentException("Invalid crypt stonework definition");if(pyramidGallery<0||pyramidGallery>1||pyramidGallery>0&&kind!=2)throw new IllegalArgumentException("Invalid pyramid gallery definition");ox=t.getIntOr("OX",0);oy=t.getIntOr("OY",0);oz=t.getIntOr("OZ",0);xx=t.getIntOr("XX",0);xz=t.getIntOr("XZ",0);zx=t.getIntOr("ZX",0);zz=t.getIntOr("ZZ",0);width=t.getIntOr("Width",0);depth=t.getIntOr("Depth",0);caveDirection=t.getIntOr("CaveDirection",0);seed=t.getLongOr("LayoutSeed",0);nativeCrypt=t.getBooleanOr("NativeCrypt",false);if(t.contains("CryptReplacement")&&!(t.get("CryptReplacement") instanceof net.minecraft.nbt.IntTag))throw new IllegalArgumentException("Invalid crypt replacement tag");cryptReplacement=t.getIntOr("CryptReplacement",0);if(cryptReplacement<0||cryptReplacement>1||cryptReplacement>0&&kind!=1)throw new IllegalArgumentException("Invalid crypt replacement definition");entryHeight=t.getIntOr("EntryHeight",18);if(kind<1||kind>3||entryHeight<0||entryHeight>32||pyramidReplacement==1&&(entryHeight<6||entryHeight>16)||Math.abs(xx)+Math.abs(xz)!=1||Math.abs(zx)+Math.abs(zz)!=1||xx*zx+xz*zz!=0||width<1||width>160||depth<1||depth>160||caveDirection<0||caveDirection>3||!getBoundingBox().equals(bounds(kind,new BlockPos(ox,oy,oz),xx,xz,zx,zz,width,depth)))throw new IllegalArgumentException("Invalid saved reimagined geometry");}
 public static ReimaginedLayoutPiece crypt(ReimaginedSites.Plan p){return new ReimaginedLayoutPiece(p.kind(),p.center(),1,0,0,1,1,1,p.caveDirection(),p.seed(),0);}
 public static ReimaginedLayoutPiece crypt(ReimaginedSites.Plan p,RealisticGenerator g){var result=crypt(p);result.cryptStonework=g.reimaginedDefinition()>=4?1:0;result.cryptReplacement=g.reimaginedDefinition()==9?1:0;return result;}
 public static ReimaginedLayoutPiece around(ReimaginedSites.Plan p,StructurePiece core,RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState random){var a=(ReimaginedPieceCoordinates)core;var entrance=a.blockbranch$local((g.reimaginedDefinition()==8||g.reimaginedDefinition()==9)?10:4,0,-24).immutable();int entryHeight=p.kind()==2?g.hutPlanningTerrain(random).drainage().sample(entrance.getX(),entrance.getZ()).height()-p.y():0;var o=a.blockbranch$local(0,0,0).immutable();var x=a.blockbranch$local(1,0,0).immutable();var z=a.blockbranch$local(0,0,1).immutable();var result=new ReimaginedLayoutPiece(p.kind(),o,x.getX()-o.getX(),x.getZ()-o.getZ(),z.getX()-o.getX(),z.getZ()-o.getZ(),p.kind()==2?21:12,p.kind()==2?21:15,p.caveDirection(),p.seed(),entryHeight);if(p.kind()==3){result.stonework=Math.min(2,g.reimaginedDefinition());result.jungleReplacement=g.reimaginedDefinition()==7||g.reimaginedDefinition()==8||g.reimaginedDefinition()==9?1:0;}if(p.kind()==2){result.pyramidGallery=g.pyramidGalleryDefinition();result.pyramidReplacement=g.reimaginedDefinition()==8||g.reimaginedDefinition()==9?1:0;}return result;}
 private static BoundingBox bounds(int kind,BlockPos o,int xx,int xz,int zx,int zz,int w,int d){int lo=kind==1?-32:kind==2?-26:-14,hiX=kind==1?32:kind==3?w+16:w+14,hiZ=kind==1?32:d+14;int minX=Integer.MAX_VALUE,minZ=minX,maxX=Integer.MIN_VALUE,maxZ=maxX;for(int x:new int[]{lo,hiX})for(int z:new int[]{lo,hiZ}){int wx=o.getX()+x*xx+z*zx,wz=o.getZ()+x*xz+z*zz;minX=Math.min(minX,wx);maxX=Math.max(maxX,wx);minZ=Math.min(minZ,wz);maxZ=Math.max(maxZ,wz);}return new BoundingBox(minX,o.getY()-20,minZ,maxX,o.getY()+40,maxZ);}
 @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag t){t.putInt("LayoutDefinition",1);if(kind==3&&stonework>1)t.putInt("JungleStonework",stonework);if(kind==3&&jungleReplacement>0)t.putInt("JungleReplacement",jungleReplacement);if(kind==2&&pyramidGallery>0)t.putInt("PyramidGallery",pyramidGallery);if(kind==2&&pyramidReplacement>0)t.putInt("PyramidReplacement",pyramidReplacement);if(kind==1&&cryptStonework>0)t.putInt("CryptStonework",cryptStonework);if(kind==1&&cryptReplacement>0)t.putInt("CryptReplacement",cryptReplacement);t.putInt("Kind",kind);t.putInt("OX",ox);t.putInt("OY",oy);t.putInt("OZ",oz);t.putInt("XX",xx);t.putInt("XZ",xz);t.putInt("ZX",zx);t.putInt("ZZ",zz);t.putInt("Width",width);t.putInt("Depth",depth);t.putInt("CaveDirection",caveDirection);t.putLong("LayoutSeed",seed);t.putBoolean("NativeCrypt",nativeCrypt);t.putInt("EntryHeight",entryHeight);}
 public int kind(){return kind;}public BlockPos local(int x,int y,int z){return new BlockPos(ox+x*xx+z*zx,oy+y,oz+x*xz+z*zz);}
 @Override public void postProcess(WorldGenLevel world,StructureManager structures,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){var w=new Writer(world,clip);switch(kind){case 1->{if(cryptReplacement==1)CryptReplacement.generate(new CryptReplacement.Writer(){public void set(int x,int y,int z,BlockState state){var p=local(x,y,z);if(clip.isInside(p)&&getBoundingBox().isInside(p))world.setBlock(p,state,2);}public Direction direction(int dx,int dz){int x=dx*xx+dz*zx,z=dx*xz+dz*zz;return x>0?Direction.EAST:x<0?Direction.WEST:z>0?Direction.SOUTH:Direction.NORTH;}},caveDirection,seed);else{crypt(w);if(cryptStonework==1)w.decorateCrypt();}}case 2->{if(pyramidReplacement==1)PyramidReplacement.generate(new PyramidReplacement.Writer(){public void set(int x,int y,int z,BlockState state){var p=local(x,y,z);if(clip.isInside(p)&&getBoundingBox().isInside(p))world.setBlock(p,state,2);}public BlockState get(int x,int y,int z){var p=local(x,y,z);return clip.isInside(p)&&getBoundingBox().isInside(p)?world.getBlockState(p):null;}public Direction direction(int dx,int dz){int x=dx*xx+dz*zx,z=dx*xz+dz*zz;return x>0?Direction.EAST:x<0?Direction.WEST:z>0?Direction.SOUTH:Direction.NORTH;}},entryHeight,seed);else pyramid(w);}case 3->{if(jungleReplacement==1)JungleReplacement.generate(new JungleReplacement.Writer(){public void set(int x,int y,int z,BlockState state){var p=local(x,y,z);if(clip.isInside(p)&&getBoundingBox().isInside(p))world.setBlock(p,state,2);}public BlockState get(int x,int y,int z){var p=local(x,y,z);return clip.isInside(p)&&getBoundingBox().isInside(p)?world.getBlockState(p):null;}public Direction direction(int dx,int dz){int x=dx*xx+dz*zx,z=dx*xz+dz*zz;return x>0?Direction.EAST:x<0?Direction.WEST:z>0?Direction.SOUTH:Direction.NORTH;}},seed);else jungle(w);}default->throw new IllegalStateException("Unqualified layout kind");}if(kind==1&&!nativeCrypt&&chunk.x()==ox>>4&&chunk.z()==oz>>4){var feature=world.registryAccess().lookupOrThrow(Registries.FEATURE).getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.FEATURE,net.minecraft.resources.Identifier.parse("minecraft:monster_room"))).value();nativeCrypt=feature.place(world,generator,RandomSource.create(seed),new BlockPos(ox,oy,oz));if(!nativeCrypt)Blockbranch.LOGGER.error("Prepared native crypt feature rejected at {}",new BlockPos(ox,oy,oz));}}
 private final class Writer{final WorldGenLevel world;final BoundingBox clip;java.util.Map<BlockPos,BlockState> cryptShell=kind==1&&cryptStonework==1?new java.util.HashMap<>():null;Writer(WorldGenLevel w,BoundingBox c){world=w;clip=c;}void block(int x,int y,int z,Block b){state(x,y,z,b.defaultBlockState());}boolean protectCore=kind==2;void state(int x,int y,int z,BlockState s){if(protectCore&&x>=0&&x<=20&&z>=0&&z<=20)return;var p=local(x,y,z);if(clip.isInside(p)){world.setBlock(p,s,2);if(cryptShell!=null)cryptShell.put(new BlockPos(x,y,z),s);}}void decorateCrypt(){var shell=cryptShell;cryptShell=null;CryptStonework.apply(shell,this::state,seed,xx,xz,zx,zz);}void box(int x1,int y1,int z1,int x2,int y2,int z2,Block b){for(int z=z1;z<=z2;z++)for(int x=x1;x<=x2;x++)for(int y=y1;y<=y2;y++)block(x,y,z,b);}void room(int x1,int y,int z1,int x2,int height,int z2,Block b){box(x1,y-1,z1,x2,y+height,z2,b);box(x1+1,y,z1+1,x2-1,y+height-1,z2-1,Blocks.CAVE_AIR);}void archX(int x,int y,int z,Block b){box(x,y,z-2,x,y+3,z-2,b);box(x,y,z+2,x,y+3,z+2,b);box(x,y+4,z-1,x,y+4,z+1,b);}void archZ(int x,int y,int z,Block b){box(x-2,y,z,x-2,y+3,z,b);box(x+2,y,z,x+2,y+3,z,b);box(x-1,y+4,z,x+1,y+4,z,b);}void lamp(int x,int y,int z,Block b){block(x,y-1,z,b);block(x,y,z,Blocks.TORCH);}}
 private void crypt(Writer w){
  // Four-sided ambulatory and unequal burial chambers surround the original native room.
  w.room(-13,0,-13,13,5,13,Blocks.STONE_BRICKS);w.box(-7,0,-7,7,5,7,Blocks.STONE_BRICKS);
  w.room(-20,0,-7,-12,5,4,Blocks.MOSSY_STONE_BRICKS);w.room(12,0,5,22,6,13,Blocks.CRACKED_STONE_BRICKS);w.room(-5,0,-21,7,4,-12,Blocks.STONE_BRICKS);
  w.box(-16,0,-2,-7,3,1,Blocks.CAVE_AIR);w.box(7,0,7,17,3,10,Blocks.CAVE_AIR);w.box(-2,0,-17,1,3,-7,Blocks.CAVE_AIR);
  for(int a=-10;a<=10;a+=5){w.archX(a,0,-10,Blocks.COBBLESTONE);w.archX(a,0,10,Blocks.COBBLESTONE);w.archZ(-10,0,a,Blocks.COBBLESTONE);w.archZ(10,0,a,Blocks.COBBLESTONE);}
  for(int z:new int[]{-5,1}){w.box(-19,0,z,-16,0,z+1,Blocks.CHISELED_STONE_BRICKS);w.box(-19,1,z,-16,1,z+1,Blocks.STONE_BRICK_SLAB);}for(int x:new int[]{-3,3}){w.box(x,0,-20,x+1,0,-17,Blocks.STONE_BRICKS);w.box(x,1,-20,x+1,1,-17,Blocks.STONE_BRICK_SLAB);}
  // The selected passage ends in actual natural cave air, proven by the immutable planner.
  int dx=caveDirection==0?1:caveDirection==1?-1:0,dz=caveDirection==2?1:caveDirection==3?-1:0;
  for(int t=10;t<=25;t++){int x=dx*t,z=dz*t;for(int side=-2;side<=2;side++){int px=x+dz*side,pz=z+dx*side;w.block(px,-1,pz,Blocks.COBBLESTONE);boolean outside=px< -20||px>22||pz< -21||pz>13;for(int y=0;y<4;y++)if(Math.abs(side)<2||outside)w.block(px,y,pz,Math.abs(side)==2?Blocks.COBBLESTONE:Blocks.CAVE_AIR);if(outside)w.block(px,4,pz,Blocks.COBBLESTONE);}if(t%6==0){if(dx!=0)w.archX(x,0,z,Blocks.MOSSY_STONE_BRICKS);else w.archZ(x,0,z,Blocks.MOSSY_STONE_BRICKS);}}
  // Collapsed dead-end, attached rubble rather than floating fragments; the loop remains passable.
  w.box(18,0,6,21,1,8,Blocks.COBBLESTONE);w.box(20,2,7,21,2,8,Blocks.MOSSY_COBBLESTONE);w.box(-20,0,-7,-18,1,-6,Blocks.COBBLESTONE);
  for(int[] p:new int[][]{{-10,-8},{10,8},{-3,-16}})w.lamp(p[0],1,p[1],Blocks.CHISELED_STONE_BRICKS);
  // Entire feature footprint stays in the start chunk. One prepared east opening works for both native room widths.
  w.box(-5,-1,-5,5,4,5,Blocks.STONE);w.box(-2,0,-2,2,3,2,Blocks.CAVE_AIR);w.box(2,0,0,10,2,0,Blocks.CAVE_AIR);w.box(6,-1,0,10,-1,0,Blocks.STONE_BRICKS);
 }
 private void pyramid(Writer w){
  // Four buried galleries wrap the original core; its archaeology, four chests and TNT are protected.
  w.box(-10,-12,-10,30,-7,30,Blocks.SANDSTONE);
  w.room(-10,-5,-10,30,5,30,Blocks.SANDSTONE);w.box(-5,-5,-5,25,0,25,Blocks.SANDSTONE);
  w.room(-16,-5,-5,-10,5,5,Blocks.CUT_SANDSTONE);w.room(30,-5,15,34,6,27,Blocks.SANDSTONE);
  w.box(-12,-5,-1,-6,-2,1,Blocks.CAVE_AIR);w.box(26,-5,18,31,-2,22,Blocks.CAVE_AIR);
  for(int t=-7;t<=27;t+=6){w.archX(t,-5,-7,Blocks.CUT_SANDSTONE);w.archX(t,-5,27,Blocks.CUT_SANDSTONE);w.archZ(-7,-5,t,Blocks.CUT_SANDSTONE);w.archZ(27,-5,t,Blocks.CUT_SANDSTONE);}
  // Sand-filled side chamber has an open upper gallery and a visibly collapsed ceiling edge.
  w.box(29,-5,16,33,-4,19,Blocks.SAND);w.box(31,-3,17,33,-3,19,Blocks.SAND);w.box(32,-2,18,33,-2,19,Blocks.SANDSTONE);
  for(int z:new int[]{-4,3}){w.box(-15,-5,z,-11,-5,z+1,Blocks.CHISELED_SANDSTONE);w.box(-15,-4,z,-11,-4,z+1,Blocks.CUT_SANDSTONE_SLAB);}
  for(int[] p:new int[][]{{-7,-5},{27,8},{14,27}})w.lamp(p[0],-4,p[1],Blocks.CHISELED_SANDSTONE);
  // Two flights reach above the maximum admitted surface relief, then descend into the buried circuit.
  var inward=zx>0?Direction.WEST:zx<0?Direction.EAST:zz>0?Direction.NORTH:Direction.SOUTH;
  var outward=inward.getOpposite();
  for(int i=0;i<=12;i++){int z=-24+i,y=entryHeight-i;w.box(1,-12,z,7,y,z,Blocks.SANDSTONE);w.box(2,y+1,z,6,y+4,z,Blocks.CAVE_AIR);for(int x=2;x<=6;x++)w.state(x,y,z,Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,inward));w.box(1,y+1,z,1,y+2,z,Blocks.CUT_SANDSTONE);w.box(7,y+1,z,7,y+2,z,Blocks.CUT_SANDSTONE);
   int rz=-12-i,ry=Math.max(-6,entryHeight-12-i);w.box(9,-12,rz,15,ry,rz,Blocks.SANDSTONE);w.box(10,ry+1,rz,14,ry+4,rz,Blocks.CAVE_AIR);for(int x=10;x<=14;x++)w.state(x,ry,rz,Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,outward));w.box(9,ry+1,rz,9,ry+2,rz,Blocks.CUT_SANDSTONE);w.box(15,ry+1,rz,15,ry+2,rz,Blocks.CUT_SANDSTONE);}
  w.box(2,-20,-12,14,entryHeight-12,-10,Blocks.SANDSTONE);w.box(2,entryHeight-11,-12,14,entryHeight-8,-10,Blocks.CAVE_AIR);
  w.box(10,-6,-26,19,-6,-25,Blocks.SANDSTONE);w.box(10,-5,-26,19,-2,-25,Blocks.CAVE_AIR);w.box(17,-6,-25,19,-6,-6,Blocks.SANDSTONE);w.box(17,-5,-25,19,-2,-6,Blocks.CAVE_AIR);
  // A west observation passage exposes the existing trap shaft; original plate/TNT/chests remain below.
  w.protectCore=false;w.box(-7,-5,9,8,-3,11,Blocks.CAVE_AIR);w.box(-7,-6,9,8,-6,11,Blocks.CUT_SANDSTONE);w.box(8,-5,9,8,-4,11,Blocks.IRON_BARS);w.box(6,-6,9,7,-6,11,Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.ORANGE));w.protectCore=true;
  if(pyramidGallery==1)pyramidGallery(w);
 }
 /** Saved gallery definition 1. Replace solid wall skins; existing apertures and native archaeology remain clear. */
 private void pyramidGallery(Writer w){
  for(int along=-10;along<=30;along++)for(int side:new int[]{-10,30})for(int y=-5;y<0;y++){
   galleryWall(w,along,y,side,along);galleryWall(w,side,y,along,along);
  }
  for(int along=-5;along<=25;along++)for(int side:new int[]{-5,25})for(int y=-5;y<0;y++){
   galleryWall(w,along,y,side,along);galleryWall(w,side,y,along,along);
  }
  // Ceiling panels change material without lowering the existing walking volume.
  for(int x=-9;x<=29;x++)for(int z=-9;z<=29;z++)if((x< -5||x>25||z< -5||z>25)&&Math.floorMod(x+z,6)<2)pyramidDetail(w,x,0,z,Blocks.SMOOTH_SANDSTONE);
  for(int along:new int[]{-2,10,26}){
   galleryTorch(w,along,-2,-9,0,1);galleryTorch(w,along,-2,29,0,-1);
   galleryTorch(w,-9,-2,along,1,0);galleryTorch(w,29,-2,along,-1,0);
  }
  // Light the custom observation passage from outside the protected native core.
  galleryTorch(w,-4,-3,9,0,1);galleryTorch(w,-1,-3,9,0,1);
  // A five-wide landing meets the original first stair; the lintel leaves three full blocks above it.
  for(int z=-26;z<=-25;z++){w.box(2,-12,z,6,entryHeight,z,Blocks.SANDSTONE);w.box(2,entryHeight+1,z,6,entryHeight+3,z,Blocks.CAVE_AIR);}
  for(int x:new int[]{1,7})for(int z=-25;z<=-23;z++){
   w.box(x,-12,z,x,entryHeight+3,z,Blocks.CUT_SANDSTONE);
   w.block(x,entryHeight+2,z,Blocks.CHISELED_SANDSTONE);
  }
  w.box(1,entryHeight+4,-25,7,entryHeight+4,-25,Blocks.CUT_SANDSTONE);
  for(int x:new int[]{3,5})w.block(x,entryHeight+4,-25,Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.ORANGE));
  w.block(4,entryHeight+4,-25,Blocks.CHISELED_SANDSTONE);
  w.box(0,entryHeight+5,-25,8,entryHeight+5,-25,Blocks.SMOOTH_SANDSTONE_SLAB);
  galleryTorch(w,2,entryHeight+2,-24,1,0);galleryTorch(w,6,entryHeight+2,-24,-1,0);
 }
 private void galleryWall(Writer w,int x,int y,int z,int along){
  int phase=Math.floorMod(along+1,6);
  Block material=y==-5||y==-1?Blocks.SMOOTH_SANDSTONE:Blocks.CUT_SANDSTONE;
  if(y==-3)material=phase==0?Blocks.CHISELED_SANDSTONE:phase==2||phase==3?Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.ORANGE):Blocks.CUT_SANDSTONE;
  pyramidDetail(w,x,y,z,material);
 }
 private void pyramidDetail(Writer w,int x,int y,int z,Block block){
  var pos=local(x,y,z);if(!w.clip.isInside(pos))return;
  var current=w.world.getBlockState(pos);
  if(current.is(Blocks.SANDSTONE)||current.is(Blocks.CUT_SANDSTONE)||current.is(Blocks.CHISELED_SANDSTONE))w.block(x,y,z,block);
 }
 private void galleryTorch(Writer w,int x,int y,int z,int dx,int dz){
  var pos=local(x,y,z);if(!w.clip.isInside(pos)||!w.world.getBlockState(pos).isAir())return;
  int wx=dx*xx+dz*zx,wz=dx*xz+dz*zz;
  var facing=wx>0?Direction.EAST:wx<0?Direction.WEST:wz>0?Direction.SOUTH:Direction.NORTH;
  w.state(x,y,z,Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING,facing));
 }
 private void jungle(Writer w){
  // Unequal stair towers connect the native temple roof to a high rear circuit.
  tower(w,-13,-4,21);tower(w,16,13,27);
  for(int x=-5;x<=0;x++)if(x!=-2)bridge(w,x,9,1,x,3);
  for(int x=11;x<=16;x++)if(x!=14)bridge(w,x,9,12,x,14);
  bridge(w,-10,18,4,-8,21);bridge(w,-8,18,19,16,21);
  bridge(w,16,9,10,21,12);w.box(19,10,12,21,12,13,Blocks.AIR);w.box(-5,10,1,-5,12,3,Blocks.AIR);w.box(16,19,19,16,21,21,Blocks.AIR);
  // Gate piers and short broken bridge ends are tied to towers/temple, with a readable one-block gap.
  w.box(-1,-12,2,-1,8,2,Blocks.MOSSY_COBBLESTONE);w.box(12,-12,13,12,8,13,Blocks.MOSSY_COBBLESTONE);
  w.box(-5,9,1,-4,10,1,Blocks.CHISELED_STONE_BRICKS);w.box(-1,9,3,0,10,3,Blocks.MOSSY_COBBLESTONE);
  // Lower routes remain open around the original tripwire/piston and loot core.
  w.box(-13,-12,-8,24,-1,-5,Blocks.COBBLESTONE);w.box(-13,0,-8,24,0,-5,Blocks.MOSSY_COBBLESTONE);
  w.box(-13,1,-8,24,4,-5,Blocks.AIR);w.box(-13,1,-8,-13,2,-5,Blocks.COBBLESTONE);w.box(24,1,-8,24,2,-5,Blocks.COBBLESTONE);
  // Contained high cistern spills down a walled shaft into an exterior rear channel.
  w.box(18,24,15,27,24,19,Blocks.STONE_BRICKS);w.box(18,25,15,27,25,19,Blocks.MOSSY_STONE_BRICKS);w.box(19,25,16,26,25,18,Blocks.AIR);
  w.box(25,0,16,27,24,18,Blocks.STONE_BRICKS);w.box(26,1,17,26,25,17,Blocks.AIR);
  w.box(13,0,16,26,0,18,Blocks.STONE_BRICKS);w.box(13,1,16,25,2,16,Blocks.MOSSY_COBBLESTONE);w.box(13,1,18,25,2,18,Blocks.MOSSY_COBBLESTONE);w.box(13,1,17,26,2,17,Blocks.AIR);
  w.box(11,0,16,15,0,21,Blocks.STONE_BRICKS);w.box(11,1,16,15,1,21,Blocks.MOSSY_COBBLESTONE);w.box(12,1,17,14,1,20,Blocks.AIR);w.block(15,1,17,Blocks.AIR);w.block(26,25,17,Blocks.WATER);
  if(stonework==2)templeEntrance(w);
  for(int[] p:new int[][]{{-12,-7},{22,-7},{-12,3},{17,20}})w.lamp(p[0],1,p[1],Blocks.MOSSY_COBBLESTONE);
 }
 private void tower(Writer w,int x,int z,int height){
  w.box(x,-12,z,x+8,-1,z+8,Blocks.COBBLESTONE);w.room(x,0,z,x+8,height,z+8,Blocks.MOSSY_COBBLESTONE);
  if(stonework==1)for(int h=5;h<height;h+=6){w.box(x,h,z,x+8,h,z,Blocks.CHISELED_STONE_BRICKS);w.box(x,h,z+8,x+8,h,z+8,Blocks.STONE_BRICKS);w.box(x,h,z,x,h,z+8,Blocks.STONE_BRICKS);w.box(x+8,h,z,x+8,h,z+8,Blocks.STONE_BRICKS);}
  for(int h=2;h<height-2;h+=6){w.box(x+3,h,z,x+5,h+2,z,Blocks.AIR);w.box(x+8,h,z+3,x+8,h+2,z+5,Blocks.AIR);}
  w.box(x+3,0,z,x+5,3,z,Blocks.AIR);w.box(x+3,0,z+8,x+5,3,z+8,Blocks.AIR);
  // A continuous perimeter stair, with a central light well and two bridge landings.
  int[][] ring={{1,1},{2,1},{3,1},{4,1},{5,1},{6,1},{7,1},{7,2},{7,3},{7,4},{7,5},{7,6},{7,7},{6,7},{5,7},{4,7},{3,7},{2,7},{1,7},{1,6},{1,5},{1,4},{1,3},{1,2}};
  for(int h=0;h<height;h++){var p=ring[h%ring.length];var n=ring[(h+1)%ring.length];int dx=n[0]-p[0],dz=n[1]-p[1],wx=dx*xx+dz*zx,wz=dx*xz+dz*zz;var facing=wx>0?Direction.EAST:wx<0?Direction.WEST:wz>0?Direction.SOUTH:Direction.NORTH;w.state(x+p[0],h,z+p[1],Blocks.MOSSY_COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,facing));}
  for(int h:new int[]{9,18})if(h<height){w.box(x+1,h,z+1,x+7,h,z+7,Blocks.MOSSY_COBBLESTONE_SLAB);w.box(x+3,h+1,z,x+5,h+3,z,Blocks.AIR);w.box(x+3,h+1,z+8,x+5,h+3,z+8,Blocks.AIR);w.box(x,h+1,z+3,x,h+3,z+5,Blocks.AIR);w.box(x+8,h+1,z+3,x+8,h+3,z+5,Blocks.AIR);}
  for(int h=0;h<height;h++){var p=ring[h%ring.length];var n=ring[(h+1)%ring.length];w.box(x+p[0],h+1,z+p[1],x+p[0],h+3,z+p[1],Blocks.AIR);w.block(x+p[0],h-1,z+p[1],Blocks.MOSSY_COBBLESTONE);int dx=n[0]-p[0],dz=n[1]-p[1],wx=dx*xx+dz*zx,wz=dx*xz+dz*zz;var facing=wx>0?Direction.EAST:wx<0?Direction.WEST:wz>0?Direction.SOUTH:Direction.NORTH;w.state(x+p[0],h,z+p[1],Blocks.MOSSY_COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,facing));}
  // Broken crown stays supported on the intact walls; missing roof corners expose the stairwell.
  w.box(x+2,height,z+2,x+6,height,z+6,Blocks.AIR);if(stonework==2){weatheredTower(w,x,z,height);climbingGrowth(w,x,z,height);return;}for(int[] p:new int[][]{{0,0},{0,8},{8,0},{8,8}})w.box(x+p[0],height+1,z+p[1],x+p[0],height+2,z+p[1],Blocks.CHISELED_STONE_BRICKS);
 }
 /** Exterior replacement only: openings and the complete stair/landing volume retain their states. */
 private void weatheredTower(Writer w,int x,int z,int height){
  for(int h=0;h<=height;h++)for(int a=0;a<=8;a++)for(int b=0;b<=8;b++){
   if(a!=0&&a!=8&&b!=0&&b!=8)continue;
   var at=local(x+a,h,z+b);if(!w.clip.isInside(at))continue;var existing=w.world.getBlockState(at);
   // Do not read neighboring chunks: the current native clip owns every observation and write.
   if(!existing.is(Blocks.MOSSY_COBBLESTONE))continue;
   double damp=Math.sin((a+x)*.51+(seed&255))*.32+Math.cos((b+z)*.49+h*.28)*.30+(.62-h/(double)height)*.75;
   w.block(x+a,h,z+b,damp>.12?Blocks.MOSSY_COBBLESTONE:Blocks.COBBLESTONE);
  }
  // Paired engaged piers widen the foot without extending beyond the admitted one-block margin.
  for(int a:new int[]{0,8})for(int b:new int[]{0,8}){
   for(int h=0;h<=height;h++)w.block(x+a,h,z+b,h<4?Blocks.MOSSY_STONE_BRICKS:Blocks.STONE_BRICKS);
   w.box(x+a+(a==0?-1:1),-12,z+b,x+a+(a==0?-1:1),-1,z+b,Blocks.COBBLESTONE);
   w.box(x+a,-12,z+b+(b==0?-1:1),x+a,-1,z+b+(b==0?-1:1),Blocks.COBBLESTONE);
   for(int h=0;h<4;h++){
    w.block(x+a+(a==0?-1:1),h,z+b,h<2?Blocks.MOSSY_STONE_BRICKS:Blocks.STONE_BRICKS);
    w.block(x+a,h,z+b+(b==0?-1:1),h<2?Blocks.MOSSY_STONE_BRICKS:Blocks.STONE_BRICKS);
   }
  }
  // A projecting stepped cornice replaces the four castle-like spires; the open light well remains.
  for(int a=-1;a<=9;a++)for(int b=-1;b<=9;b++)if(a==-1||a==9||b==-1||b==9){
   // The short tower exits onto the y18 bridge here; its exterior needs two clear blocks.
   if(height==21&&b==9&&a>=3&&a<=5)continue;
   w.block(x+a,height-1,z+b,Blocks.MOSSY_STONE_BRICK_SLAB);
  }
  for(int a=0;a<=8;a++)for(int b=0;b<=8;b++)if(a==0||a==8||b==0||b==8){
   w.block(x+a,height,z+b,Blocks.STONE_BRICKS);
   if((a+b+height)%7!=0)w.block(x+a,height+1,z+b,Blocks.MOSSY_STONE_BRICK_SLAB);
  }
  // Short lintels and side jambs articulate the existing apertures, never fill their air cells.
  for(int h=2;h<height-2;h+=6){
   for(int side:new int[]{2,6}){for(int dy=0;dy<3;dy++){w.block(x+side,h+dy,z,Blocks.STONE_BRICKS);w.block(x+8,h+dy,z+side,Blocks.STONE_BRICKS);}}
   for(int a=2;a<=6;a++){stoneDetail(w,x+a,h+3,z,a==4?Blocks.CHISELED_STONE_BRICKS:Blocks.MOSSY_STONE_BRICKS);stoneDetail(w,x+8,h+3,z+a,a==4?Blocks.CHISELED_STONE_BRICKS:Blocks.MOSSY_STONE_BRICKS);}
  }
 }
 private void templeEntrance(Writer w){
  // The added portico frames the native shrine without writing into its room/trap footprint.
  w.box(-1,-12,-4,12,-1,-1,Blocks.COBBLESTONE);
  w.box(-1,0,-4,12,0,-1,Blocks.MOSSY_STONE_BRICK_SLAB);
  for(int x:new int[]{1,10}){
   w.box(x,-12,-6,x+1,0,-5,Blocks.COBBLESTONE);
   w.box(x,1,-6,x+1,5,-5,Blocks.MOSSY_STONE_BRICKS);
   w.box(x,4,-6,x+1,4,-6,Blocks.CHISELED_STONE_BRICKS);
  }
  for(int tier=0;tier<4;tier++)w.box(1+tier,6+tier,-6,11-tier,6+tier,-5,tier==0?Blocks.MOSSY_STONE_BRICKS:Blocks.STONE_BRICKS);
  w.box(5,7,-6,7,7,-6,Blocks.CHISELED_STONE_BRICKS);
  w.box(2,7,-6,3,7,-6,Blocks.MOSSY_STONE_BRICKS);w.block(8,8,-6,Blocks.MOSSY_STONE_BRICKS);
  w.block(4,9,-6,Blocks.CRACKED_STONE_BRICKS);w.block(5,9,-6,Blocks.MOSSY_STONE_BRICKS);
 }
 private void climbingGrowth(Writer w,int x,int z,int height){
  // Attached vanilla vines stay off all window and bridge apertures.
  var towardSouth=zx>0?Direction.EAST:zx<0?Direction.WEST:zz>0?Direction.SOUTH:Direction.NORTH;
  var towardEast=xx>0?Direction.EAST:xx<0?Direction.WEST:xz>0?Direction.SOUTH:Direction.NORTH;
  for(int h=3;h<height-5;h++)if(h!=11)w.state(x+2,h,z-1,Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(towardSouth),true));
  for(int h=7;h<height-2;h++)if(h!=height-7&&h!=height-6)w.state(x+6,h,z-1,Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(towardSouth),true));
  for(int h=1;h<height/2;h++)w.state(x-1,h,z+2,Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(towardEast),true));
 }
 private void stoneDetail(Writer w,int x,int y,int z,Block block){var at=local(x,y,z);if(w.clip.isInside(at)&&!w.world.getBlockState(at).isAir())w.block(x,y,z,block);}
 private void bridge(Writer w,int x1,int y,int z1,int x2,int z2){w.box(x1,y,z1,x2,y,z2,Blocks.MOSSY_COBBLESTONE);if(x1==x2){w.box(x1,y+1,z1,x2,y+1,z1,Blocks.COBBLESTONE_WALL);w.box(x1,y+1,z2,x2,y+1,z2,Blocks.COBBLESTONE_WALL);}else if(z2-z1==2){w.box(x1,y+1,z1,x2,y+1,z1,Blocks.COBBLESTONE_WALL);w.box(x1,y+1,z2,x2,y+1,z2,Blocks.COBBLESTONE_WALL);}}

}
