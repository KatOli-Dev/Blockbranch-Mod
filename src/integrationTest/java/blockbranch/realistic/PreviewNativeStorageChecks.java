package blockbranch.realistic;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.maps.*;
import net.minecraft.world.level.storage.SavedDataStorage;
/** Actual native storage load/error/save path, isolated disposable data folder. */
final class PreviewNativeStorageChecks {
 static void check(ServerLevel level)throws Exception{
  Path root=Files.createTempDirectory(DeferredPreviewQualification.dir(),"map-codec-");var ops=level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
  var data=MapItemSavedData.createFresh(0,0,(byte)1,true,true,level.dimension());data.setColor(4,8,(byte)52);DeferredMapPreview.mark(data,true);((PendingMapPreview)data).blockbranch$previewRows(19);
  var source=(CompoundTag)MapItemSavedData.CODEC.encodeStart(ops,data).getOrThrow();var before=new ArrayList<byte[]>();
  for(int i=0;i<10;i++){
   var record=source.copy();if(i==0)record.remove("blockbranch_pending_preview");if(i==2)record.putInt("blockbranch_pending_preview",2);if(i==3)record.putString("blockbranch_pending_preview","broken");if(i==4)record.putDouble("blockbranch_pending_preview",1.5);if(i==5)record.putLong("blockbranch_pending_preview",4294967297L);
   if(i==6)record.putInt("blockbranch_preview_rows",-1);if(i==7)record.putInt("blockbranch_preview_rows",128);if(i==8)record.putDouble("blockbranch_preview_rows",7.5);if(i==9)record.putString("blockbranch_preview_rows","bad");
   var tag=new CompoundTag();tag.put("data",record);NbtUtils.addCurrentDataVersion(tag);var path=root.resolve("minecraft/maps/"+i+".dat");Files.createDirectories(path.getParent());NbtIo.writeCompressed(tag,path);before.add(Files.readAllBytes(path));
  }
  try(var storage=new SavedDataStorage(root,net.minecraft.util.datafix.DataFixers.getDataFixer(),level.registryAccess())){
   for(int i=0;i<10;i++){
    var read=storage.get(MapItemSavedData.type(new MapId(i)));
    if(i<2)require(read!=null&&DeferredMapPreview.pending(read)==(i==1)&&read.colors[4+8*128]==52&&((PendingMapPreview)read).blockbranch$previewRows()==(i==1?19:0),"Native legacy/pending load");
    else{require(read==null,"Native unsupported/malformed map fails closed "+i);try{storage.computeIfAbsent(MapItemSavedData.type(new MapId(i)));throw new AssertionError("Native empty replacement unexpectedly created");}catch(IllegalStateException expected){}}
   }
   storage.saveAndJoin();
  }
  for(int i=0;i<10;i++)require(Arrays.equals(before.get(i),Files.readAllBytes(root.resolve("minecraft/maps/"+i+".dat"))),"Native save did not overwrite rejected original "+i);
  // Offline repair and a new native storage owner can read the original pixels again.
  Files.write(root.resolve("minecraft/maps/2.dat"),before.get(1));
  try(var reopened=new SavedDataStorage(root,net.minecraft.util.datafix.DataFixers.getDataFixer(),level.registryAccess())){require(DeferredMapPreview.pending(reopened.get(MapItemSavedData.type(new MapId(2)))),"Offline marker repair plus reopening recovers");}
  Files.writeString(DeferredPreviewQualification.dir().resolve("native-storage.txt"),"PASS native legacy/pending map load; future, malformed, fractional and overflowing marker and malformed progress rejection; no empty replacement or overwrite; offline repair/reopen\n");
 }
 private static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
}
