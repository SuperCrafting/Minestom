package net.minestom.codegen;

import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class Generators {

    public static void main(String[] args) {

        Path source = null;
        if (args.length < 1) {
            System.err.println("Usage: <target folder>");
            return;
        }
        Path outputFolder = Path.of(args[0]);
        if(args.length >= 2) {
            source = Path.of(args[1]);
        }

        // Special generators
        new DyeColorGenerator(resource("dye_colors.json", source), outputFolder).generate();
        new ParticleGenerator(resource("particle.json", source), outputFolder).generate();
        new ConstantsGenerator(resource("constants.json", source), outputFolder).generate();
        new RecipeTypeGenerator(resource("recipe_types.json", source), outputFolder).generate();
        new GenericEnumGenerator("net.minestom.server.recipe.display", "RecipeDisplayType",
                resource("recipe_display_types.json", source), outputFolder).generate();
        new GenericEnumGenerator("net.minestom.server.recipe.display", "SlotDisplayType",
                resource("slot_display_types.json", source), outputFolder).generate();
        new GenericEnumGenerator("net.minestom.server.recipe", "RecipeBookCategory",
                resource("recipe_book_categories.json", source), outputFolder).generate();
        new GenericEnumGenerator("net.minestom.server.item.component", "ConsumeEffectType",
                resource("consume_effects.json", source), outputFolder).packagePrivate().generate();
        new GenericEnumGenerator("net.minestom.server.command", "ArgumentParserType",
                resource("command_arguments.json", source), outputFolder).generate();
        new GenericEnumGenerator("net.minestom.server.entity", "VillagerType",
                resource("villager_types.json", source), outputFolder).generate();
        new WorldEventGenerator("net.minestom.server.worldevent", "WorldEvent",
                resource("world_events.json", source), outputFolder).generate();
        new BlockPropertyGenerator("net.minestom.server.instance.block", "BlockProperties",
                resource("block_property.json", source), outputFolder).generate();

        var generator = new RegistryGenerator(outputFolder);

        // Static registries
        generator.generate(resource("block.json", source), "net.minestom.server.instance.block", "Block", "BlockImpl", "Blocks");
        generator.generate(resource("item.json", source), "net.minestom.server.item", "Material", "MaterialImpl", "Materials");
        generator.generate(resource("entity_type.json", source), "net.minestom.server.entity", "EntityType", "EntityTypeImpl", "EntityTypes");
        generator.generate(resource("potion_effect.json", source), "net.minestom.server.potion", "PotionEffect", "PotionEffectImpl", "PotionEffects");
        generator.generate(resource("potion_type.json", source), "net.minestom.server.potion", "PotionType", "PotionTypeImpl", "PotionTypes");
        generator.generate(resource("sound_event.json", source), "net.minestom.server.sound", "SoundEvent", "BuiltinSoundEvent", "SoundEvents");
        generator.generate(resource("custom_statistics.json", source), "net.minestom.server.statistic", "StatisticType", "StatisticTypeImpl", "StatisticTypes");
        generator.generate(resource("attribute.json", source), "net.minestom.server.entity.attribute", "Attribute", "AttributeImpl", "Attributes");
        generator.generate(resource("feature_flag.json", source), "net.minestom.server", "FeatureFlag", "FeatureFlagImpl", "FeatureFlags");
        generator.generate(resource("fluid.json", source), "net.minestom.server.instance.fluid", "Fluid", "FluidImpl", "Fluids");
        generator.generate(resource("villager_profession.json", source), "net.minestom.server.entity", "VillagerProfession", "VillagerProfessionImpl", "VillagerProfessions");
        generator.generate(resource("game_event.json", source), "net.minestom.server.game", "GameEvent", "GameEventImpl", "GameEvents");
        generator.generate(resource("block_sound_type.json", source), "net.minestom.server.instance.block", "BlockSoundType", "BlockSoundImpl", "BlockSoundTypes");
        generator.generate(resource("block_entity_types.json", source), "net.minestom.server.instance.block", "BlockEntityType", "BlockEntityTypeImpl", "BlockEntityTypes");

        // Dynamic registries
        generator.generateKeys(resource("chat_type.json", source), "net.minestom.server.message", "ChatType");
        generator.generateKeys(resource("dimension_type.json", source), "net.minestom.server.world", "DimensionType");
        generator.generateKeys(resource("damage_type.json", source), "net.minestom.server.entity.damage", "DamageType");
        generator.generateKeys(resource("trim_material.json", source), "net.minestom.server.item.armor", "TrimMaterial");
        generator.generateKeys(resource("trim_pattern.json", source), "net.minestom.server.item.armor", "TrimPattern");
        generator.generateKeys(resource("banner_pattern.json", source), "net.minestom.server.instance.block.banner", "BannerPattern");
        generator.generateKeys(resource("enchantment.json", source), "net.minestom.server.item.enchant", "Enchantment");
        generator.generateKeys(resource("painting_variant.json", source), "net.minestom.server.entity.metadata.other", "PaintingVariant");
        generator.generateKeys(resource("jukebox_song.json", source), "net.minestom.server.instance.block.jukebox", "JukeboxSong");
        generator.generateKeys(resource("instrument.json", source), "net.minestom.server.item.instrument", "Instrument");
        generator.generateKeys(resource("wolf_variant.json", source), "net.minestom.server.entity.metadata.animal.tameable", "WolfVariant");
        generator.generateKeys(resource("wolf_sound_variant.json", source), "net.minestom.server.entity.metadata.animal.tameable", "WolfSoundVariant");
        generator.generateKeys(resource("cat_variant.json", source), "net.minestom.server.entity.metadata.animal.tameable", "CatVariant");
        generator.generateKeys(resource("chicken_variant.json", source), "net.minestom.server.entity.metadata.animal", "ChickenVariant");
        generator.generateKeys(resource("cow_variant.json", source), "net.minestom.server.entity.metadata.animal", "CowVariant");
        generator.generateKeys(resource("frog_variant.json", source), "net.minestom.server.entity.metadata.animal", "FrogVariant");
        generator.generateKeys(resource("pig_variant.json", source), "net.minestom.server.entity.metadata.animal", "PigVariant");
        generator.generateKeys(resource("worldgen/biome.json", source), "net.minestom.server.world.biome", "Biome");

        System.out.println("Finished generating code");
    }

    private static InputStream resource(String name, @Nullable Path source) {
        if(source == null)
            return Objects.requireNonNull(Generators.class.getResourceAsStream("/" + name), "Cannot find resource: %s".formatted(name));
        try {
            return Files.newInputStream(source.resolve(name));
        } catch (Exception e) {
            throw new RuntimeException("Cannot find resource: %s".formatted(name), e);
        }
    }
}
