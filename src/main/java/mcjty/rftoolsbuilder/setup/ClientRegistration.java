package mcjty.rftoolsbuilder.setup;


import mcjty.lib.gui.GenericGuiContainer;
import mcjty.rftoolsbuilder.RFToolsBuilder;
import mcjty.rftoolsbuilder.modules.builder.BuilderSetup;
import mcjty.rftoolsbuilder.modules.builder.client.GuiBuilder;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = RFToolsBuilder.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientRegistration {

    @SubscribeEvent
    public static void init(FMLClientSetupEvent event) {
        GenericGuiContainer.register(BuilderSetup.CONTAINER_BUILDER, GuiBuilder::new);
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> sounds) {
    }

//    @SubscribeEvent
//    public static void onTextureStitch(TextureStitchEvent.Pre event) {
//        Minecraft mc = Minecraft.getInstance();
//        for (SideType value : SideType.VALUES) {
//            if (value.getSideTexture() != null) {
//                for (Tier tier : Tier.VALUES) {
//                    ResourceLocation location = new ResourceLocation(value.getSideTexture() + tier.getSuffix());
//                    System.out.println("location = " + location);
//                    event.getMap().func_215244_a(mc.textureManager, mc.getResourceManager(),
//                            location, mc);
//                }
//            }
//            if (value.getUpDownTexture() != null) {
//                event.getMap().func_215244_a(mc.textureManager, mc.getResourceManager(),
//                        new ResourceLocation(value.getUpDownTexture()), mc);
//            }
//        }
//    }

    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
//        PowerCellBakedModel model = new PowerCellBakedModel(DefaultVertexFormats.BLOCK);
//        Lists.newArrayList(ModBlocks.CELL1, ModBlocks.CELL2, ModBlocks.CELL3).stream()
//                .forEach(block -> {
//                    event.getModelRegistry().put(new ModelResourceLocation(block.getRegistryName(), ""), model);
//                    event.getModelRegistry().put(new ModelResourceLocation(block.getRegistryName(), "lower=false,upper=false"), model);
//                    event.getModelRegistry().put(new ModelResourceLocation(block.getRegistryName(), "lower=false,upper=true"), model);
//                    event.getModelRegistry().put(new ModelResourceLocation(block.getRegistryName(), "lower=true,upper=false"), model);
//                    event.getModelRegistry().put(new ModelResourceLocation(block.getRegistryName(), "lower=true,upper=true"), model);
//                });
    }
}