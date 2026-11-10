package blockbranch.realistic;
/** Independent truth table prevents older binaries accepting new feature combinations. */
public final class StructureSchemaQualification {
 public static void main(String[] args){
  int[][] modes={{0,0,0,88},{1,0,0,89},{1,1,0,90},{0,0,1,91},{1,0,1,92},{1,1,1,93}};int checks=0;
  for(var m:modes){int v=StructureGenerationSchema.versionFor(88,m[0],m[1],m[2]);if(v!=m[3])throw new AssertionError("Canonical barrier");for(int schema=82;schema<=94;schema++){
   boolean expected=m[3]==88?schema<=88:schema==m[3];if(StructureGenerationSchema.matches(schema,m[0],m[1],m[2])!=expected)throw new AssertionError("Combination changed at schema "+schema);checks++;
  }}
  for(int legacy=82;legacy<=88;legacy++)if(StructureGenerationSchema.versionFor(legacy,0,0,0)!=legacy)throw new AssertionError("Legacy schema upgraded");
  for(int[] m:new int[][]{{0,1,0},{0,1,1},{2,0,0},{0,-1,0},{0,0,2}}){if(StructureGenerationSchema.matches(93,m[0],m[1],m[2]))throw new AssertionError("Invalid flags accepted");try{StructureGenerationSchema.versionFor(88,m[0],m[1],m[2]);throw new AssertionError("Invalid writer accepted");}catch(IllegalArgumentException expected){}}
  if(StructureGenerationSchema.matches(91,1,0,1)||StructureGenerationSchema.matches(92,1,1,1))throw new AssertionError("Unsafe downgrade barrier");
  for(int huts=0;huts<=2;huts++)for(int treasure=0;treasure<=1;treasure++)for(int ship=0;ship<=1;ship++)for(int gate=0;gate<=1;gate++)for(int version=82;version<=95;version++){
   boolean expected=huts==0?StructureGenerationSchema.matches(version,treasure,ship,gate):huts==1&&version==94&&treasure==1&&ship==1&&gate==1;
   if(StructureGenerationSchema.matches(version,treasure,ship,gate,huts)!=expected)throw new AssertionError("Hut feature barrier");checks++;
  }
  if(StructureGenerationSchema.versionFor(88,1,1,1,1)!=94)throw new AssertionError("New-world hut schema");
  for(int snow=0;snow<=2;snow++)for(int huts=0;huts<=1;huts++)for(int treasure=0;treasure<=1;treasure++)for(int ship=0;ship<=1;ship++)for(int gate=0;gate<=1;gate++)for(int version=82;version<=96;version++){
   boolean expected=snow==0?StructureGenerationSchema.matches(version,treasure,ship,gate,huts):snow==1&&version==95&&huts==1&&treasure==1&&ship==1&&gate==1;
   if(StructureGenerationSchema.matches(version,treasure,ship,gate,huts,snow)!=expected)throw new AssertionError("Snow shelter feature barrier");checks++;
  }
  if(StructureGenerationSchema.versionFor(88,1,1,1,1,1)!=95)throw new AssertionError("New-world snow schema");
  for(int ruins=0;ruins<=2;ruins++)for(int snow=0;snow<=1;snow++)for(int huts=0;huts<=1;huts++)for(int treasure=0;treasure<=1;treasure++)for(int ship=0;ship<=1;ship++)for(int gate=0;gate<=1;gate++)for(int version=82;version<=97;version++){
   boolean expected=ruins==0?StructureGenerationSchema.matches(version,treasure,ship,gate,huts,snow):ruins==1&&version==96&&snow==1&&huts==1&&treasure==1&&ship==1&&gate==1;
   if(StructureGenerationSchema.matches(version,treasure,ship,gate,huts,snow,ruins)!=expected)throw new AssertionError("Warm ruin feature barrier");checks++;
  }
  if(StructureGenerationSchema.versionFor(88,1,1,1,1,1,1)!=96)throw new AssertionError("New-world warm ruin schema");
  for(int cold=0;cold<=2;cold++)for(int warm=0;warm<=1;warm++)for(int snow=0;snow<=1;snow++)for(int huts=0;huts<=1;huts++)for(int treasure=0;treasure<=1;treasure++)for(int ship=0;ship<=1;ship++)for(int gate=0;gate<=1;gate++)for(int version=82;version<=98;version++){
   boolean expected=cold==0?StructureGenerationSchema.matches(version,treasure,ship,gate,huts,snow,warm):cold==1&&version==97&&warm==1&&snow==1&&huts==1&&treasure==1&&ship==1&&gate==1;
   if(StructureGenerationSchema.matches(version,treasure,ship,gate,huts,snow,warm,cold)!=expected)throw new AssertionError("Cold ruin feature barrier");checks++;
  }
  if(StructureGenerationSchema.versionFor(88,1,1,1,1,1,1,1)!=97)throw new AssertionError("New-world cold ruin schema");
  for(int deep=0;deep<=2;deep++)for(int cold=0;cold<=1;cold++)for(int warm=0;warm<=1;warm++)for(int snow=0;snow<=1;snow++)for(int huts=0;huts<=1;huts++)for(int treasure=0;treasure<=1;treasure++)for(int ship=0;ship<=1;ship++)for(int gate=0;gate<=1;gate++)for(int version=82;version<=99;version++){
   boolean expected=deep==0?StructureGenerationSchema.matches(version,treasure,ship,gate,huts,snow,warm,cold):deep==1&&version==98&&cold==1&&warm==1&&snow==1&&huts==1&&treasure==1&&ship==1&&gate==1;
   if(StructureGenerationSchema.matches(version,treasure,ship,gate,huts,snow,warm,cold,deep)!=expected)throw new AssertionError("Deep cold ruin feature barrier");checks++;
  }
  if(StructureGenerationSchema.versionFor(88,1,1,1,1,1,1,1,1)!=98)throw new AssertionError("New-world deep cold ruin schema");
  for(int deepWarm=0;deepWarm<=2;deepWarm++)for(int deep=0;deep<=1;deep++)for(int cold=0;cold<=1;cold++)for(int warm=0;warm<=1;warm++)for(int snow=0;snow<=1;snow++)for(int huts=0;huts<=1;huts++)for(int treasure=0;treasure<=1;treasure++)for(int ship=0;ship<=1;ship++)for(int gate=0;gate<=1;gate++)for(int version=82;version<=100;version++){
   boolean expected=deepWarm==0?StructureGenerationSchema.matches(version,treasure,ship,gate,huts,snow,warm,cold,deep):deepWarm==1&&version==99&&deep==1&&cold==1&&warm==1&&snow==1&&huts==1&&treasure==1&&ship==1&&gate==1;
   if(StructureGenerationSchema.matches(version,treasure,ship,gate,huts,snow,warm,cold,deep,deepWarm)!=expected)throw new AssertionError("Deep warm feature barrier");checks++;
  }
  if(StructureGenerationSchema.versionFor(88,1,1,1,1,1,1,1,1,1)!=99)throw new AssertionError("New-world deep warm schema");
  if(StructureGenerationSchema.versionFor(88,1,1,1,1,1,1,1,1,1,1)!=100)throw new AssertionError("Native body schema");checks++;
  for(int omitted=0;omitted<10;omitted++){int[] f={1,1,1,1,1,1,1,1,1,1};f[omitted]=0;if(StructureGenerationSchema.matches(100,f[0],f[1],f[2],f[3],f[4],f[5],f[6],f[7],f[8],f[9]))throw new AssertionError("Incomplete native body schema");checks++;}
  if(StructureGenerationSchema.matches(99,1,1,1,1,1,1,1,1,1,1)||StructureGenerationSchema.matches(100,1,1,1,1,1,1,1,1,1,2))throw new AssertionError("Native body downgrade/unknown definition");checks+=2;
  if(StructureGenerationSchema.versionFor(88,1,1,1,1,1,1,1,1,1,1,1)!=101)throw new AssertionError("Native ordinary schema");checks++;
  for(int omitted=0;omitted<11;omitted++){int[] f={1,1,1,1,1,1,1,1,1,1,1};f[omitted]=0;if(StructureGenerationSchema.matches(101,f[0],f[1],f[2],f[3],f[4],f[5],f[6],f[7],f[8],f[9],f[10]))throw new AssertionError("Incomplete native ordinary schema");checks++;}
  if(StructureGenerationSchema.matches(100,1,1,1,1,1,1,1,1,1,1,1)||StructureGenerationSchema.matches(101,1,1,1,1,1,1,1,1,1,1,2))throw new AssertionError("Native ordinary downgrade/unknown definition");checks+=2;
  if(StructureGenerationSchema.versionFor(88,1,1,1,1,1,1,1,1,1,1,1,1)!=102)throw new AssertionError("Private reimagined barrier");checks++;
  for(int omitted=0;omitted<12;omitted++){int[] f={1,1,1,1,1,1,1,1,1,1,1,1};f[omitted]=0;if(StructureGenerationSchema.matches(102,f[0],f[1],f[2],f[3],f[4],f[5],f[6],f[7],f[8],f[9],f[10],f[11]))throw new AssertionError("Incomplete reimagined schema");checks++;}
  if(StructureGenerationSchema.matches(101,1,1,1,1,1,1,1,1,1,1,1,1)||StructureGenerationSchema.matches(102,1,1,1,1,1,1,1,1,1,1,1,2)||!StructureGenerationSchema.matches(101,1,1,1,1,1,1,1,1,1,1,1,0))throw new AssertionError("Reimagined downgrade/unknown/legacy optout");checks+=3;
  for(int layout=-1;layout<=10;layout++)for(int coverage=-1;coverage<=2;coverage++)for(int version=100;version<=253;version++){
   boolean valid=(layout>=0&&layout<=5||layout==7||layout==8||layout==9)&&(coverage==0||coverage==1);
   int expectedVersion=layout==9?240+coverage:layout==8?220+coverage:layout==7?200+coverage:layout==5?160+coverage:layout==4?130+coverage:layout==3?118+coverage:layout==2?(coverage==1?105:104):layout==0&&coverage==0?101:layout==1&&coverage==1?103:102;
   if(StructureGenerationSchema.matches(version,1,1,1,1,1,1,1,1,1,1,1,layout,coverage)!=(valid&&version==expectedVersion))throw new AssertionError("Combined schema flags "+layout+"/"+coverage+"/"+version);
   if(valid&&StructureGenerationSchema.versionFor(88,1,1,1,1,1,1,1,1,1,1,1,layout,coverage)!=expectedVersion)throw new AssertionError("Combined writer");checks++;
  }
  for(int coverage=0;coverage<=1;coverage++)for(int omitted=0;omitted<11;omitted++){
   int[] flags={1,1,1,1,1,1,1,1,1,1,1};flags[omitted]=0;
   if(StructureGenerationSchema.matches(coverage==1?105:104,flags[0],flags[1],flags[2],flags[3],flags[4],flags[5],flags[6],flags[7],flags[8],flags[9],flags[10],2,coverage))throw new AssertionError("Incomplete weathered feature combination");checks++;
  }
  for(int layout=-1;layout<=10;layout++)for(int coverage=-1;coverage<=2;coverage++)for(int wings=-1;wings<=2;wings++)for(int version=100;version<=253;version++){
   boolean expected=wings==0?StructureGenerationSchema.matches(version,1,1,1,1,1,1,1,1,1,1,1,layout,coverage):wings==1&&(layout>=1&&layout<=5||layout==7||layout==8||layout==9)&&coverage>=0&&coverage<=1&&version==(layout==9?242:layout==8?222:layout==7?202:layout==5?162:layout==4?132:layout==3?120:106+2*(layout-1))+coverage;
   if(StructureGenerationSchema.matches(version,1,1,1,1,1,1,1,1,1,1,1,layout,coverage,wings)!=expected)throw new AssertionError("Mansion independent feature matrix");checks++;
  }
  for(int layout:new int[]{1,2,3,4,5,7,8,9})for(int coverage=0;coverage<=1;coverage++){
   if(StructureGenerationSchema.versionFor(88,1,1,1,1,1,1,1,1,1,1,1,layout,coverage,1)!=(layout==9?242:layout==8?222:layout==7?202:layout==5?162:layout==4?132:layout==3?120:106+2*(layout-1))+coverage)throw new AssertionError("Mansion writer");checks++;
   for(int omitted=0;omitted<11;omitted++){int[] flags={1,1,1,1,1,1,1,1,1,1,1};flags[omitted]=0;if(StructureGenerationSchema.matches((layout==9?242:layout==8?222:layout==7?202:layout==5?162:layout==4?132:layout==3?120:106+2*(layout-1))+coverage,flags[0],flags[1],flags[2],flags[3],flags[4],flags[5],flags[6],flags[7],flags[8],flags[9],flags[10],layout,coverage,1))throw new AssertionError("Incomplete mansion feature combination");checks++;}
  }
  for(int base=82;base<=253;base++)for(int gallery=0;gallery<=1;gallery++){
   boolean valid=gallery==0||(base>=102&&base<=109)||(base>=118&&base<=121)||(base>=130&&base<=133)||(base>=160&&base<=163)||(base>=200&&base<=203)||(base>=220&&base<=223)||(base>=240&&base<=243);
   if(valid){int written=PyramidGallerySchema.schema(base,gallery);if(written!=(gallery==0?base:base+8)||PyramidGallerySchema.baseSchema(written,gallery)!=base)throw new AssertionError("Independent pyramid gallery barrier");}
   else try{PyramidGallerySchema.schema(base,gallery);throw new AssertionError("Incomplete gallery combination");}catch(IllegalArgumentException expected){}
   checks++;
  }
  for(int version=82;version<=253;version++)for(int gallery=-1;gallery<=2;gallery++){
   int expected=gallery==0?version:gallery==1&&((version>=110&&version<=117)||(version>=126&&version<=129)||(version>=138&&version<=141)||(version>=168&&version<=171)||(version>=208&&version<=211)||(version>=228&&version<=231)||(version>=248&&version<=251))?version-8:Integer.MAX_VALUE;
   if(PyramidGallerySchema.baseSchema(version,gallery)!=expected)throw new AssertionError("Gallery read barrier");checks++;
  }
  for(int saved=82;saved<=253;saved++)for(int crown=-1;crown<=2;crown++){
   int expected=crown==0?saved:crown==1&&saved==142?141:crown==1&&saved==172?171:crown==1&&saved==212?211:crown==1&&saved==232?231:crown==1&&saved==252?251:Integer.MAX_VALUE;
   if(BorealCrownSchema.baseSchema(saved,crown)!=expected)throw new AssertionError("Crown read barrier");checks++;
  }
  for(int base=82;base<=253;base++)for(int crown=0;crown<=1;crown++){
   boolean valid=crown==0||base==141||base==171||base==211||base==231||base==251;
   if(valid){int written=BorealCrownSchema.schema(base,crown);if(written!=(crown==0?base:base==141?142:base==171?172:base==211?212:base==231?232:252)||BorealCrownSchema.baseSchema(written,crown)!=base)throw new AssertionError("Crown roundtrip barrier");}
   else try{BorealCrownSchema.schema(base,crown);throw new AssertionError("Incomplete crown foundation");}catch(IllegalArgumentException expected){}
   checks++;
  }
  int[][] foundations={{1,102,103,106,107},{2,104,105,108,109},{3,118,119,120,121},{4,130,131,132,133},{5,160,161,162,163},{7,200,201,202,203},{8,220,221,222,223},{9,240,241,242,243}};
  for(int[] row:foundations)for(int coverage=0;coverage<=1;coverage++)for(int wings=0;wings<=1;wings++)for(int gallery=0;gallery<=1;gallery++){
   int expected=row[1+2*wings+coverage]+8*gallery;
   int written=PyramidGallerySchema.schema(StructureGenerationSchema.versionFor(88,1,1,1,1,1,1,1,1,1,1,1,row[0],coverage,wings),gallery);
   if(written!=expected||!StructureGenerationSchema.matches(PyramidGallerySchema.baseSchema(written,gallery),1,1,1,1,1,1,1,1,1,1,1,row[0],coverage,wings))throw new AssertionError("Historical/new composed mapping");checks++;
  }
  for(int layout:new int[]{6,7,8,9,10})for(int coverage=0;coverage<=1;coverage++)for(int wings=0;wings<=1;wings++)for(int omitted=-1;omitted<11;omitted++){
   int[] f={1,1,1,1,1,1,1,1,1,1,1};if(omitted>=0)f[omitted]=0;
   boolean valid=(layout==7||layout==8||layout==9)&&omitted==-1;
   int base=layout==9?240:layout==8?220:200;
   if(StructureGenerationSchema.matches(base+coverage+2*wings,f[0],f[1],f[2],f[3],f[4],f[5],f[6],f[7],f[8],f[9],f[10],layout,coverage,wings)!=valid)throw new AssertionError("Replacement prerequisites/reserved definition");
   try{int written=StructureGenerationSchema.versionFor(88,f[0],f[1],f[2],f[3],f[4],f[5],f[6],f[7],f[8],f[9],f[10],layout,coverage,wings);if(!valid||written!=base+coverage+2*wings)throw new AssertionError("Replacement writer accepted invalid flags");}catch(IllegalArgumentException expected){if(valid)throw new AssertionError("Replacement writer rejected complete foundation");}checks++;
  }
  for(int saved=81;saved<=255;saved++)for(int relief=-1;relief<=2;relief++){
   int expected=relief==0&&saved>=82&&saved<=252?saved:relief==1&&saved==213?212:relief==1&&saved==233?232:relief==1&&saved==253?252:Integer.MAX_VALUE;
   if(GravelReliefSchema.baseSchema(saved,relief)!=expected||GravelReliefSchema.matches(saved,relief)!=(expected!=Integer.MAX_VALUE))throw new AssertionError("Gravel relief read barrier");checks++;
  }
  for(int foundation=82;foundation<=255;foundation++)for(int relief=-1;relief<=2;relief++){
   boolean valid=relief==0||relief==1&&(foundation==212||foundation==232||foundation==252);
   try{int written=GravelReliefSchema.schema(foundation,relief);if(!valid||written!=(relief==0?foundation:foundation==212?213:foundation==232?233:253))throw new AssertionError("Gravel relief writer accepted incomplete/future foundation");}
   catch(IllegalArgumentException expected){if(valid)throw new AssertionError("Gravel relief writer rejected supported foundation");}checks++;
  }
  int unwrapped=GravelReliefSchema.baseSchema(213,1);
  if(!StructureGenerationSchema.matches(PyramidGallerySchema.baseSchema(BorealCrownSchema.baseSchema(unwrapped,1),1),1,1,1,1,1,1,1,1,1,1,1,7,1,1))throw new AssertionError("Relief wrapper must retain complete jungle/gallery/crown foundation");checks++;
  for(int layout:new int[]{6,7,8,9,10})for(int coverage=0;coverage<=1;coverage++)for(int wings=0;wings<=1;wings++)for(int gallery=0;gallery<=1;gallery++)for(int crown=0;crown<=1;crown++)for(int relief=-1;relief<=2;relief++)for(int saved=200;saved<=255;saved++){
   int foundation=(layout==7?200:layout==8?220:240)+coverage+2*wings+8*gallery;
   boolean complete=coverage==1&&wings==1&&gallery==1;
   boolean valid=(layout==7||layout==8||layout==9)&&(crown==0||complete)&&(relief==0||relief==1&&crown==1&&complete);
   int expected=foundation+(crown==1?1:0)+(relief==1?1:0);
   int base=GravelReliefSchema.baseSchema(saved,relief);
   boolean accepted=GravelReliefSchema.matches(saved,relief)&&StructureGenerationSchema.matches(PyramidGallerySchema.baseSchema(BorealCrownSchema.baseSchema(base,crown),gallery),1,1,1,1,1,1,1,1,1,1,1,layout,coverage,wings);
   if(accepted!=(valid&&saved==expected))throw new AssertionError("Composed gravel/crown/gallery feature matrix");checks++;
  }
  for(int saved=0;saved<=280;saved++)for(int sanctuary=-1;sanctuary<=2;sanctuary++){
   int expected=sanctuary==0&&saved>=82&&saved<=253?saved:sanctuary==1&&saved==234?233:sanctuary==1&&saved==254?253:Integer.MAX_VALUE;
   if(SanctuaryReplacementSchema.baseSchema(saved,sanctuary)!=expected||SanctuaryReplacementSchema.matches(saved,sanctuary)!=(expected!=Integer.MAX_VALUE))throw new AssertionError("Independent sanctuary read barrier");checks++;
   try{int written=SanctuaryReplacementSchema.schema(saved,sanctuary);if(written!=(sanctuary==0?saved:saved==233?234:254)||sanctuary!=0&&!(sanctuary==1&&(saved==233||saved==253)))throw new AssertionError("Independent sanctuary writer");}catch(IllegalArgumentException expectedFailure){if(sanctuary==0||sanctuary==1&&(saved==233||saved==253))throw new AssertionError("Supported sanctuary writer rejected");}checks++;
  }
  for(int layout:new int[]{1,2,3,4,5,6,7,8,9})for(int coverage=0;coverage<=1;coverage++)for(int wings=0;wings<=1;wings++)for(int gallery=0;gallery<=1;gallery++)for(int crown=0;crown<=1;crown++)for(int relief=0;relief<=1;relief++)for(int omitted=-1;omitted<11;omitted++){
   int[] f={1,1,1,1,1,1,1,1,1,1,1};if(omitted>=0)f[omitted]=0;
   int base=PyramidGallerySchema.baseSchema(BorealCrownSchema.baseSchema(GravelReliefSchema.baseSchema(SanctuaryReplacementSchema.baseSchema(234,1),relief),crown),gallery);
   boolean valid=layout==8&&coverage==1&&wings==1&&gallery==1&&crown==1&&relief==1&&omitted==-1;
   if(StructureGenerationSchema.matches(base,f[0],f[1],f[2],f[3],f[4],f[5],f[6],f[7],f[8],f[9],f[10],layout,coverage,wings)!=valid)throw new AssertionError("Independent sanctuary complete published foundation");checks++;
  }
  for(int layout:new int[]{1,2,3,4,5,6,7,8,9})for(int coverage=0;coverage<=1;coverage++)for(int wings=0;wings<=1;wings++)for(int gallery=0;gallery<=1;gallery++)for(int crown=0;crown<=1;crown++)for(int relief=0;relief<=1;relief++)for(int omitted=-1;omitted<11;omitted++){
   int[] f={1,1,1,1,1,1,1,1,1,1,1};if(omitted>=0)f[omitted]=0;
   int base=PyramidGallerySchema.baseSchema(BorealCrownSchema.baseSchema(GravelReliefSchema.baseSchema(SanctuaryReplacementSchema.baseSchema(254,1),relief),crown),gallery);
   boolean valid=layout==9&&coverage==1&&wings==1&&gallery==1&&crown==1&&relief==1&&omitted==-1;
   if(StructureGenerationSchema.matches(base,f[0],f[1],f[2],f[3],f[4],f[5],f[6],f[7],f[8],f[9],f[10],layout,coverage,wings)!=valid)throw new AssertionError("Independent sanctuary complete crypt foundation");checks++;
  }
  for(int saved=0;saved<=280;saved++)for(int mansion=-1;mansion<=2;mansion++){
   int expected=mansion==0&&saved>=82&&saved<=254?saved:mansion==1&&saved==255?254:Integer.MAX_VALUE;
   if(MansionReplacementSchema.baseSchema(saved,mansion)!=expected||MansionReplacementSchema.matches(saved,mansion)!=(expected!=Integer.MAX_VALUE))throw new AssertionError("Independent mansion read barrier");checks++;
   try{int written=MansionReplacementSchema.schema(saved,mansion);if(written!=(mansion==0?saved:255)||mansion!=0&&!(mansion==1&&saved==254))throw new AssertionError("Independent mansion writer");}catch(IllegalArgumentException expectedFailure){if(mansion==0||mansion==1&&saved==254)throw new AssertionError("Supported mansion writer rejected");}checks++;
  }
  for(int layout=0;layout<=10;layout++)for(int coverage=0;coverage<=1;coverage++)for(int wings=0;wings<=1;wings++)for(int gallery=0;gallery<=1;gallery++)for(int crown=0;crown<=1;crown++)for(int relief=0;relief<=1;relief++)for(int sanctuary=0;sanctuary<=1;sanctuary++)for(int omitted=-1;omitted<11;omitted++){
   int[] f={1,1,1,1,1,1,1,1,1,1,1};if(omitted>=0)f[omitted]=0;
   int base=PyramidGallerySchema.baseSchema(BorealCrownSchema.baseSchema(GravelReliefSchema.baseSchema(SanctuaryReplacementSchema.baseSchema(MansionReplacementSchema.baseSchema(255,1),sanctuary),relief),crown),gallery);
   boolean valid=layout==9&&coverage==1&&wings==1&&gallery==1&&crown==1&&relief==1&&sanctuary==1&&omitted==-1;
   if(StructureGenerationSchema.matches(base,f[0],f[1],f[2],f[3],f[4],f[5],f[6],f[7],f[8],f[9],f[10],layout,coverage,wings)!=valid)throw new AssertionError("Independent mansion complete published foundation");checks++;
  }
  System.out.println("PASS explicit saved-feature truth table checks="+checks+"; legacy defaults and new downgrade barriers");
 }
}
