package dev.abyssalport;

import java.util.*;
import dev.abyssalport.content.*;
import dev.abyssalport.item.DreadiumArmorEffects;
import dev.abyssalport.research.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(AbyssalPort.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DreadiumEquipmentGameTests {
    private DreadiumEquipmentGameTests() { }
    @GameTest(template="empty",batch="dreadium_armor_crafting")
    public static void NativeFourPieceArmorCraftsRepairsAndKeepsSourceMaterialValues(GameTestHelper helper) {
        var level=helper.getLevel(); var player=helper.makeMockServerPlayerInLevel();
        var pieces=List.of(DreadProcessingContent.HELMET.get(),DreadProcessingContent.CHESTPLATE.get(),DreadProcessingContent.LEGGINGS.get(),DreadProcessingContent.BOOTS.get());
        var patterns=List.of(List.of("III","I I"),List.of("I I","III","III"),List.of("III","I I","I I"),List.of("I I","I I"));
        for(int index=0;index<pieces.size();index++) {
            var pattern=patterns.get(index); var stacks=new ArrayList<ItemStack>();
            for(String row:pattern) for(char c:row.toCharArray()) stacks.add(c=='I'?new ItemStack(DreadProcessingContent.INGOT.get()):ItemStack.EMPTY);
            var input=CraftingInput.of(3,pattern.size(),stacks); var result=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level).orElseThrow().value().assemble(input,level.registryAccess());
            var armor=pieces.get(index); helper.assertTrue(result.is(armor) && result.getCount()==1,"Dreadium armour lacks a real ingot recipe");
            helper.assertTrue(result.getMaxDamage()==armor.getType().getDurability(40) && armor.getToughness()==1 && armor.getEnchantmentValue()==15,"Dreadium armor material differs from source");
            result.setDamageValue(300); var anvil=new AnvilMenu(1,player.getInventory()); anvil.getSlot(0).set(result); anvil.getSlot(1).set(new ItemStack(DreadProcessingContent.INGOT.get()));
            helper.assertTrue(!anvil.getSlot(2).getItem().isEmpty() && anvil.getSlot(2).getItem().getDamageValue()<300,"Native anvil cannot repair the new armor");
        }
        helper.assertTrue(pieces.get(0).getDefense()==3 && pieces.get(1).getDefense()==8 && pieces.get(2).getDefense()==6 && pieces.get(3).getDefense()==3,"Armor defense values differ");
        helper.succeed();
    }
    @GameTest(template="empty",batch="dreadium_equipped_effects")
    public static void OnlyEquippedChestAndBootsRefreshFiniteEffectsWithoutDiseaseImmunity(GameTestHelper helper) {
        var player=helper.makeMockServerPlayerInLevel(); var other=helper.makeMockServerPlayerInLevel();
        player.getInventory().setItem(0,new ItemStack(DreadProcessingContent.CHESTPLATE.get())); DreadiumArmorEffects.apply(player);
        helper.assertTrue(!player.hasEffect(MobEffects.DAMAGE_RESISTANCE),"Carried armor activates an effect");
        player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(DreadProcessingContent.HELMET.get()));
        player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(DreadProcessingContent.CHESTPLATE.get()));
        player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(DreadProcessingContent.LEGGINGS.get()));
        player.setItemSlot(EquipmentSlot.FEET,new ItemStack(DreadProcessingContent.BOOTS.get())); DreadiumArmorEffects.apply(player);
        helper.assertTrue(player.hasEffect(MobEffects.DAMAGE_RESISTANCE) && player.hasEffect(MobEffects.MOVEMENT_SPEED)
                && player.getEffect(MobEffects.DAMAGE_RESISTANCE).getDuration()==20 && !player.hasEffect(MobEffects.WATER_BREATHING)
                && !other.hasEffect(MobEffects.DAMAGE_RESISTANCE) && !dev.abyssalport.effect.DreadPlagueEffect.immune(player),"Equipped set invents a buff, immunity or leaks across players");
        helper.assertTrue(WastelandProgressionResearch.discover(player,new ItemStack(DreadProcessingContent.HELMET.get()))
                && EarlyResearch.has(player,EarlyResearch.DREADIUM) && !EarlyResearch.has(other,EarlyResearch.DREADIUM),"Actual armor fails player-scoped research");
        player.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY); player.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);
        for(int tick=0;tick<25;tick++) {
            if(tick%10==0) DreadiumArmorEffects.apply(player);
            for(var effect:List.of(MobEffects.DAMAGE_RESISTANCE,MobEffects.MOVEMENT_SPEED)) {
                var current=player.getEffect(effect); if(current!=null && !current.tick(player,()->{})) player.removeEffect(effect);
            }
        }
        helper.assertTrue(player.getActiveEffects().isEmpty(),"Unequipped armor keeps permanent effects"); helper.succeed();
    }
    @GameTest(template="empty",batch="equipment_native_enchantments")
    public static void EveryReachableGearTierAcceptsNativeAppropriateEnchantments(GameTestHelper helper) {
        var enchantments=helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        var unbreaking=enchantments.getHolderOrThrow(Enchantments.UNBREAKING).value();
        var protection=enchantments.getHolderOrThrow(Enchantments.PROTECTION).value();
        var efficiency=enchantments.getHolderOrThrow(Enchantments.EFFICIENCY).value();
        var sharpness=enchantments.getHolderOrThrow(Enchantments.SHARPNESS).value();
        var player=new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"GearEnchantAudit"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var all=List.of(AbyssalniteContent.HELMET.get(),AbyssalniteContent.CHESTPLATE.get(),AbyssalniteContent.LEGGINGS.get(),AbyssalniteContent.BOOTS.get(),
                WastelandMineralsContent.HELMET.get(),WastelandMineralsContent.CHESTPLATE.get(),WastelandMineralsContent.LEGGINGS.get(),WastelandMineralsContent.BOOTS.get(),
                DreadProcessingContent.HELMET.get(),DreadProcessingContent.CHESTPLATE.get(),DreadProcessingContent.LEGGINGS.get(),DreadProcessingContent.BOOTS.get(),
                AbyssalniteContent.PICKAXE.get(),AbyssalniteContent.AXE.get(),AbyssalniteContent.SHOVEL.get(),AbyssalniteContent.SWORD.get(),AbyssalniteContent.HOE.get(),
                WastelandMineralsContent.PICKAXE.get(),WastelandMineralsContent.AXE.get(),WastelandMineralsContent.SHOVEL.get(),WastelandMineralsContent.SWORD.get(),WastelandMineralsContent.HOE.get(),
                DreadProcessingContent.PICKAXE.get(),DreadProcessingContent.AXE.get(),DreadProcessingContent.SHOVEL.get(),DreadProcessingContent.SWORD.get(),DreadProcessingContent.HOE.get());
        for(var item:all) {
            var stack=new ItemStack(item);
            helper.assertTrue(unbreaking.isSupportedItem(stack),"Native Unbreaking tag is missing for "+item);
            if(item instanceof ArmorItem) helper.assertTrue(protection.isSupportedItem(stack) && !efficiency.isSupportedItem(stack),"Native armor enchantment tags differ for "+item);
            else if(item instanceof SwordItem) helper.assertTrue(sharpness.isSupportedItem(stack) && !protection.isSupportedItem(stack),"Native sword enchantment tags differ for "+item);
            else helper.assertTrue(efficiency.isSupportedItem(stack) && !protection.isSupportedItem(stack),"Native mining enchantment tags differ for "+item);
            var anvil=new AnvilMenu(1,player.getInventory());
            var holder=enchantments.getHolderOrThrow(Enchantments.UNBREAKING);
            anvil.getSlot(0).set(stack); anvil.getSlot(1).set(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(holder,1)));
            var result=anvil.getSlot(2).getItem();
            helper.assertTrue(result.is(item) && result.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).getLevel(holder)==1
                    && stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).isEmpty(),"Survival anvil cannot apply a real enchantment book: "+item);
        }
        helper.succeed();
    }
}
