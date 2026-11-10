#!/usr/bin/env python3
"""Only new egg item resources and missing cooking unlocks; never rewrites animal art."""
from pathlib import Path
import json,subprocess,sys
R=Path(__file__).resolve().parents[2];A=R/'src/main/resources/assets/blockbranch';D=R/'src/main/resources/data/blockbranch'
SPECIES={
 'wild_boar':'Wild Boar','red_deer':'Red Deer','american_bison':'American Bison',
 'brown_bear':'Brown Bear','mallard':'Mallard','eurasian_beaver':'Eurasian Beaver',
 'european_perch':'European Perch','harbor_seal':'Harbor Seal'}
def write(path,obj):path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(obj,indent=2)+'\n')
lang=json.loads((A/'lang/en_us.json').read_text())
# The art generator owns only the eight icon PNGs; never regenerate entity coats.
subprocess.run([sys.executable,str(R/'scripts/assets/generate-wildlife-spawn-eggs.py')],check=True)
for species,name in SPECIES.items():
 item=species+'_spawn_egg'
 write(A/f'items/{item}.json',{'model':{'type':'minecraft:model','model':f'blockbranch:item/{item}'}})
 write(A/f'models/item/{item}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'blockbranch:item/{item}'}})
 lang['item.blockbranch.'+item]=name+' Spawn Egg'
write(A/'lang/en_us.json',lang)
for raw in ['duck','perch']:
 for method in ['smelting','smoking','campfire_cooking']:
  key=f'cooked_{raw}_from_{method}';recipe='blockbranch:'+key
  write(D/f'advancement/recipes/food/{key}.json',{'parent':'minecraft:recipes/root','criteria':{
    'has_'+raw:{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':'blockbranch:'+raw}]}},
    'has_the_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipes':recipe}}},
    'requirements':[['has_the_recipe','has_'+raw]],'rewards':{'recipes':[recipe]}})
