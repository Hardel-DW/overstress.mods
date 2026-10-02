package fr.hardel.overstress.fakeplayer.client;

import fr.hardel.overstress.fakeplayer.BotStanding;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Unit;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

record BotSave(Vec3 position, float yRot, BotStanding standing) {
    private static final String INVENTORY = "Inventory";
    private static final String GAME_MODE = "playerGameType";
    private static final List<ItemStackWithSlot> KIT = List.of(
        new ItemStackWithSlot(0, new ItemStack(Items.DIAMOND_PICKAXE)),
        new ItemStackWithSlot(1, new ItemStack(Items.DIAMOND_SHOVEL)),
        new ItemStackWithSlot(2, new ItemStack(Items.DIAMOND_SWORD)),
        new ItemStackWithSlot(3, new ItemStack(Items.COOKED_BEEF, Items.COOKED_BEEF.getDefaultMaxStackSize())),
        new ItemStackWithSlot(4, new ItemStack(Items.ZOMBIE_SPAWN_EGG, Items.ZOMBIE_SPAWN_EGG.getDefaultMaxStackSize())),
        new ItemStackWithSlot(9, new ItemStack(Items.DIAMOND_HELMET)),
        new ItemStackWithSlot(10, new ItemStack(Items.DIAMOND_LEGGINGS)),
        new ItemStackWithSlot(11, new ItemStack(Items.DIAMOND_BOOTS)));

    void write(MinecraftServer server, UUID id) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, server.registryAccess());
        NbtUtils.addCurrentDataVersion(output);
        output.store(ServerPlayer.SavedPosition.MAP_CODEC, new ServerPlayer.SavedPosition(Optional.of(Level.OVERWORLD), Optional.of(position), Optional.of(new Vec2(yRot, 0))));
        output.putBoolean(Entity.TAG_INVULNERABLE, standing.invulnerable());
        output.putInt(GAME_MODE, standing.gameMode().getId());
        output.store(LivingEntity.TAG_EQUIPMENT, EntityEquipment.CODEC, equipment());
        ValueOutput.TypedOutputList<ItemStackWithSlot> inventory = output.list(INVENTORY, ItemStackWithSlot.CODEC);
        KIT.forEach(inventory::add);
        Path directory = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        writeFile(output, directory, directory.resolve("%s.dat".formatted(id)));
    }

    private static EntityEquipment equipment() {
        ItemStack elytra = new ItemStack(Items.ELYTRA);
        elytra.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        EntityEquipment equipment = new EntityEquipment();
        equipment.set(EquipmentSlot.CHEST, elytra);
        return equipment;
    }

    private static void writeFile(TagValueOutput output, Path directory, Path file) {
        try {
            Files.createDirectories(directory);
            NbtIo.writeCompressed(output.buildResult(), file);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
