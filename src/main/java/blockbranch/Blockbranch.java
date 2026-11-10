package blockbranch;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Blockbranch implements ModInitializer {
	public static final String MOD_ID = "blockbranch";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
        blockbranch.time.DaylightCycle.initialize();
        blockbranch.metallurgy.MetallurgyContent.initialize();
        blockbranch.metallurgy.ZincBrassContent.initialize();
        blockbranch.metallurgy.SteelContent.initialize();
        blockbranch.metallurgy.TungstenContent.initialize();
        blockbranch.metallurgy.StainlessContent.initialize();
        blockbranch.metallurgy.LeadContent.initialize();
        blockbranch.metallurgy.FoolsDiamondContent.initialize();
        blockbranch.metallurgy.PreciousContent.initialize();
        blockbranch.metallurgy.SilverEquipment.initialize();
        blockbranch.metallurgy.PlatinumAlloyContent.initialize();
        blockbranch.metallurgy.PlatinumEquipment.initialize();
        blockbranch.metallurgy.TitaniumContent.initialize();
        blockbranch.metallurgy.TitaniumEquipment.initialize();
        blockbranch.metallurgy.MagnetiteContent.initialize();
        blockbranch.metallurgy.ResoniteContent.initialize();
        blockbranch.metallurgy.UraniumContent.initialize();
        blockbranch.metallurgy.BismuthContent.initialize();
		blockbranch.realistic.DesertWaygatePiece.initialize();
        blockbranch.realistic.ReimaginedCryptStructure.initialize();
		blockbranch.cubic.ColumnRuntime.initialize();
		blockbranch.realistic.ProgressiveMapDrawing.initialize();
        blockbranch.realistic.DeferredMapPreview.initialize();
		blockbranch.wildlife.WildlifeRuntime.initialize();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
