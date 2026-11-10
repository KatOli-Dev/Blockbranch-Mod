package blockbranch.wildlife;

import blockbranch.Blockbranch;
import java.nio.file.*;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.*;
import net.minecraft.util.ProblemReporter;

/** Explicit opt-in native checks; never runs in player worlds. */
final class WildlifeSmoke {
    private static boolean started, reload, retaliationObserved;
    private static int ticks;
    private static WildBoar breederA, breederB, defender;
    private static net.minecraft.world.entity.animal.wolf.Wolf assailant;
    private static long elapsed, maxTick, tickStart;
    private static final java.util.List<WildBoar> population = new java.util.ArrayList<>();
    private static final UUID WITNESS = UUID.fromString("c8c38f31-ded5-43a2-902e-690c390b1701");
    static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                var level = server.overworld();
                var boar = WildlifeRuntime.WILD_BOAR.create(level, EntitySpawnReason.COMMAND);
                var wolf = EntityTypes.WOLF.create(level, EntitySpawnReason.COMMAND);
                var dolphin = EntityTypes.DOLPHIN.create(level, EntitySpawnReason.COMMAND);
                var squid = EntityTypes.SQUID.create(level, EntitySpawnReason.COMMAND);
                check(WildlifeCombat.isAnimal(dolphin)&&WildlifeCombat.isAnimal(squid),"native ageable aquatic animal classification");
                var bat=EntityTypes.BAT.create(level,EntitySpawnReason.COMMAND);
                check(WildlifeCombat.isAnimal(bat),"native ambient bat classification");
                for (Difficulty difficulty : Difficulty.values()) {
                    bat=EntityTypes.BAT.create(level,EntitySpawnReason.COMMAND);
                    var sheep = EntityTypes.SHEEP.create(level, EntitySpawnReason.COMMAND);
                    var player = new net.minecraft.world.entity.player.Player(level, new GameProfile(UUID.randomUUID(), "WildlifeTest")) {
                        @Override public net.minecraft.world.level.GameType gameMode() { return net.minecraft.world.level.GameType.SURVIVAL; }
                    };
                    server.setDifficulty(difficulty, true);
                    boolean versusAnimal = difficulty != Difficulty.PEACEFUL;
                    boolean versusPlayer = difficulty == Difficulty.NORMAL || difficulty == Difficulty.HARD;
                    check(WildlifeCombat.permits(boar, sheep) == versusAnimal, "boar animal policy " + difficulty);
                    check(WildlifeCombat.permits(wolf, sheep) == versusAnimal, "vanilla wolf animal policy " + difficulty);
                    check(boar.canAttack(player) == versusPlayer, "boar target policy " + difficulty);
                    check(wolf.canAttack(player) == versusPlayer, "vanilla wolf target policy " + difficulty);
                    sheep.setInvulnerableTime(0); sheep.setHealth(sheep.getMaxHealth());
                    check(sheep.hurtServer(level, level.damageSources().mobAttack(wolf), 2) == versusAnimal, "native animal damage " + difficulty);
                    player.setInvulnerableTime(0); player.setHealth(player.getMaxHealth());
                    check(player.hurtServer(level, level.damageSources().mobAttack(boar), 2) == versusPlayer, "native player damage " + difficulty);
                    check(dolphin.canAttack(player)==versusPlayer,"native dolphin player target policy "+difficulty);
                    // Fresh receivers isolate native damageCooldownTime, distinct from
                    // Entity.invulnerableTime in this target version.
                    sheep=EntityTypes.SHEEP.create(level,EntitySpawnReason.COMMAND);
                    check(sheep.hurtServer(level,level.damageSources().mobAttack(dolphin),2)==versusAnimal,"native dolphin animal damage "+difficulty);
                    var dolphinPlayer=new net.minecraft.world.entity.player.Player(level,new GameProfile(UUID.randomUUID(),"DolphinPolicy")){
                        @Override public net.minecraft.world.level.GameType gameMode(){return net.minecraft.world.level.GameType.SURVIVAL;}
                    };
                    check(dolphinPlayer.hurtServer(level,level.damageSources().mobAttack(dolphin),2)==versusPlayer,"native dolphin player damage "+difficulty);
                    bat.setInvulnerableTime(0);bat.setHealth(bat.getMaxHealth());
                    check(bat.hurtServer(level,level.damageSources().mobAttack(boar),2)==versusAnimal,"native ambient bat victim damage "+difficulty);
                    Blockbranch.LOGGER.info("VANILLA_AQUATIC_AMBIENT_POLICY_PASS difficulty={} dolphinPlayerAttack={} animalDamage={} squidClassified=true batClassified=true",difficulty,versusPlayer,versusAnimal);
                }
                check(boar.isFood(new ItemStack(Items.CARROT)) && !boar.isFood(new ItemStack(Items.PORKCHOP)), "breeding food");
                boar.setAge(-2400); boar.setInLoveTime(200); boar.setPersistenceRequired();
                var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
                check(boar.save(output), "entity save");
                var copy = WildlifeRuntime.WILD_BOAR.create(level, EntitySpawnReason.LOAD);
                copy.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
                check(copy.getAge() == -2400 && copy.getInLoveTime() == 200 && copy.isPersistenceRequired(), "age/love/persistence roundtrip");
                boar.setAge(0); var child = boar.getBreedOffspring(level, boar);
                check(child != null && child.getType() == WildlifeRuntime.WILD_BOAR, "species offspring");
                check(boar.getLootTable().orElseThrow().identifier().toString().equals("blockbranch:entities/wild_boar"), "loot identity");
                var table = server.reloadableRegistries().getLootTable(boar.getLootTable().orElseThrow());
                for(boolean burning:new boolean[]{false,true}) {
                    boar.setRemainingFireTicks(burning?100:0);
                    var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, boar)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, boar.position())
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE, level.damageSources().generic())
                        .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
                    var drops = table.getRandomItems(params,42L);
                    check(drops.size()==1 && drops.getFirst().is(burning?Items.COOKED_PORKCHOP:Items.PORKCHOP)
                        && drops.getFirst().getCount()>=1 && drops.getFirst().getCount()<=3,"usable raw/cooked loot "+burning);
                }
                for(int cz=-1;cz<=1;cz++)for(int cx=-1;cx<=1;cx++)level.setChunkForced(cx,cz,true);
                level.setChunkForced(-2,-2,true);level.getChunk(-2,-2);
                level.getChunk(0,0);
                Path marker=Path.of(System.getProperty("blockbranch.wildlifeSmokeResult")).resolveSibling("witness-created.txt");
                reload=Files.exists(marker);
                if(!reload) {
                    var witness=WildlifeRuntime.WILD_BOAR.create(level,EntitySpawnReason.COMMAND);
                    witness.setUUID(WITNESS);witness.snapTo(8.5,-60,8.5,0,0);witness.setAge(-6000);
                    witness.setHealth(13.5F);witness.setNoAi(true);witness.setPersistenceRequired();
                    witness.setCustomName(net.minecraft.network.chat.Component.literal("Saved boar witness"));
                    check(level.addFreshEntity(witness),"add persisted witness");Files.writeString(marker,"Created\n");
                }
                for(int i=0;i<32;i++) {
                    var subject=WildlifeRuntime.WILD_BOAR.create(level,EntitySpawnReason.COMMAND);
                    subject.snapTo(1.5+i%6*2,-60,1.5+i/6*2,0,0);subject.setPersistenceRequired();
                    level.addFreshEntity(subject);population.add(subject);
                }
                breederA=WildlifeRuntime.WILD_BOAR.create(level,EntitySpawnReason.COMMAND);
                breederB=WildlifeRuntime.WILD_BOAR.create(level,EntitySpawnReason.COMMAND);
                breederA.snapTo(8.5,-60,14.5,0,0);breederB.snapTo(9.5,-60,14.5,0,0);
                level.addFreshEntity(breederA);level.addFreshEntity(breederB);
                var feeder=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"BoarFeeder"),ClientInformation.createDefault());
                feeder.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.CARROT,2));
                breederA.mobInteract(feeder,net.minecraft.world.InteractionHand.MAIN_HAND);
                breederB.mobInteract(feeder,net.minecraft.world.InteractionHand.MAIN_HAND);
                check(breederA.isInLove() && breederB.isInLove() && feeder.getMainHandItem().isEmpty(),"actual food interaction consumes carrots and enables breeding");
                var registry=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
                for(String id:new String[]{"minecraft:forest","minecraft:birch_forest","minecraft:dark_forest","blockbranch:dappled_oak_birch_woodland","blockbranch:warm_subhumid_oak_pine_woodland"}) {
                    var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.Identifier.parse(id));
                    var settings=registry.getOrThrow(key).value().getAttributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,net.minecraft.world.level.biome.MobSpawnSettings.EMPTY);
                    var entries=settings.getMobsToSpawn(MobCategory.CREATURE).unwrap().stream().filter(entry->entry.value().type()==WildlifeRuntime.WILD_BOAR).toList();
                    check(entries.size()==1 && entries.getFirst().weight()==4 && entries.getFirst().value().count().minInclusive()==2 && entries.getFirst().value().count().maxInclusive()==4,"native habitat entry unique, bounded group "+id);
                }
                defender=WildlifeRuntime.WILD_BOAR.create(level,EntitySpawnReason.COMMAND);
                assailant=EntityTypes.WOLF.create(level,EntitySpawnReason.COMMAND);
                // Keep individual retaliation outside the cohort's herd-alert radius.
                defender.snapTo(-24.5,-60,-24.5,0,0);assailant.snapTo(-20.5,-60,-24.5,0,0);
                assailant.setNoAi(true);level.addFreshEntity(defender);level.addFreshEntity(assailant);
                started=true;
                Blockbranch.LOGGER.info("WILDLIFE_SMOKE functional checks passed; testing active population and disk {}",reload?"RELOAD":"NEW");
            } catch (Throwable failure) { Blockbranch.LOGGER.error("WILDLIFE_SMOKE FAIL", failure);server.halt(false); }
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_SERVER_TICK.register(server -> tickStart=System.nanoTime());
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            if(!started)return;
            long duration=System.nanoTime()-tickStart;elapsed+=duration;maxTick=Math.max(maxTick,duration);
            ++ticks;
            if(ticks==20)check(defender.hurtServer(server.overworld(),server.overworld().damageSources().mobAttack(assailant),1),"native provocation after entity begins ticking");
            if(defender.getLastHurtMob()==assailant && assailant.getHealth()<assailant.getMaxHealth())retaliationObserved=true;
            if(ticks<120)return;
            try {
                var witness=server.overworld().getEntity(WITNESS);
                check(witness instanceof WildBoar,"actual stored entity loaded");
                var saved=(WildBoar)witness;
                check(saved.isPersistenceRequired() && saved.isNoAi() && saved.getAge()<0 && saved.getHealth()==13.5F
                    && saved.getCustomName().getString().equals("Saved boar witness"),"disk age/health/name/AI/persistence");
                for(var subject:population)check(subject.tickCount>=100,"population actively ticked count="+subject.tickCount+" pos="+subject.position()+" removed="+subject.isRemoved());
                check(breederA.getAge()>0 && breederB.getAge()>0 && !breederA.isInLove() && !breederB.isInLove(),"native breeding goal produced offspring and cooldown");
                check(retaliationObserved && assailant.getHealth()<assailant.getMaxHealth()
                    && defender.position().distanceToSqr(-24.5,-60,-24.5)>1,
                    "native retaliation navigation/melee landed defender="+defender.position()+" target="+defender.getTarget()+" lastHit="+defender.getLastHurtMob()+" wolfHealth="+assailant.getHealth());
                defender.discard();assailant.discard();breederA.discard();breederB.discard();
                for(var entity:com.google.common.collect.ImmutableList.copyOf(server.overworld().getAllEntities()))if(entity instanceof WildBoar && !entity.getUUID().equals(WITNESS))entity.discard();
                Files.writeString(Path.of(System.getProperty("blockbranch.wildlifeSmokeResult")),"PASS\n");
                Blockbranch.LOGGER.info("WILDLIFE_SMOKE PASS {} population=32+breedingPair ticks={} meanWholeTickMs={} maxWholeTickMs={} nativeDamage=true loot=true storedEntity=true nativeBreeding=true nativeRetaliation=true habitats=5",
                    reload?"RELOAD":"NEW",ticks,elapsed/1_000_000.0/ticks,maxTick/1_000_000.0);
            }catch(Throwable failure){Blockbranch.LOGGER.error("WILDLIFE_SMOKE FAIL",failure);}
            started=false;server.halt(false);
        });
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
