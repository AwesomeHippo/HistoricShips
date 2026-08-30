package com.awesomehippo.historicships.recipe;

import com.awesomehippo.historicships.NapoleonShipMod;
import com.awesomehippo.historicships.ShipsConfig;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

public final class ShipAssemblyRecipe {
    public static final int INPUT_SLOTS = 18;
    public static final int RESULT_SLOT = 18;
    public static final int TOTAL_SLOTS = 19;

    public final Component title;
    public final Supplier<ItemStack> result;
    public final List<CountedNeed> needs;
    public final BooleanSupplier enabled;

    public ShipAssemblyRecipe(Component title, Supplier<ItemStack> result, List<CountedNeed> needs, BooleanSupplier enabled) {
        this.title = title;
        this.result = result;
        this.needs = List.copyOf(needs);
        this.enabled = enabled;
    }

    public ItemStack resultStack() {
        return this.result.get().copy();
    }

    public boolean matches(Container container) {
        for (CountedNeed need : this.needs) {
            if (countMatching(container, need) < need.count) {
                return false;
            }
        }
        return true;
    }

    // any matching ingredient (but not eenough for a full craft)
    public boolean isPartial(Container container) {
        if (this.matches(container)) {
            return false;
        }
        for (CountedNeed need : this.needs) {
            if (countMatching(container, need) > 0) {
                return true;
            }
        }
        return false;
    }

    public static int countMatching(Container container, CountedNeed need) {
        int total = 0;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && need.test(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public void consume(Container container) {
        for (CountedNeed need : this.needs) {
            int remaining = need.count;
            for (int i = 0; i < INPUT_SLOTS && remaining > 0; i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty() || !need.test(stack)) {
                    continue;
                }
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                if (stack.isEmpty()) {
                    container.setItem(i, ItemStack.EMPTY);
                }
                remaining -= take;
            }
        }
    }

    public boolean canPlaceResult(Container container) {
        ItemStack out = container.getItem(RESULT_SLOT);
        if (out.isEmpty()) {
            return true;
        }
        ItemStack want = this.resultStack();
        return ItemStack.isSameItemSameComponents(out, want) && out.getCount() + want.getCount() <= out.getMaxStackSize();
    }

    public static List<ShipAssemblyRecipe> all() {
        return RECIPES.stream().filter(recipe -> recipe.enabled.getAsBoolean()).toList();
    }

    public static ShipAssemblyRecipe byIndex(int index) {
        if (index < 0 || index >= RECIPES.size()) {
            return null;
        }
        ShipAssemblyRecipe recipe = RECIPES.get(index);
        if (!recipe.enabled.getAsBoolean()) {
            return null;
        }
        return recipe;
    }

    public int index() {
        return RECIPES.indexOf(this);
    }

    private static final List<ShipAssemblyRecipe> RECIPES = List.of(
            new ShipAssemblyRecipe(Component.translatable("gui.historicships.recipe.drakkar"), () -> new ItemStack(NapoleonShipMod.DRAKKAR_ITEM.get()), List.of(tag(ItemTags.LOGS, 48), item(Items.STICK, 8), item(Items.WHITE_WOOL, 4), item(Items.STRING, 4), item(Items.LEATHER, 2)), ShipsConfig.DRAKKAR),
            new ShipAssemblyRecipe(Component.translatable("gui.historicships.recipe.quinquereme"), () -> new ItemStack(NapoleonShipMod.QUINQUEREME_ITEM.get()), List.of(tag(ItemTags.LOGS, 64), item(Items.STICK, 16), item(Items.RED_WOOL, 6), item(Items.COPPER_INGOT, 12), item(Items.IRON_INGOT, 4), item(Items.STRING, 6), item(Items.GOLD_NUGGET, 8)), ShipsConfig.QUINQUEREME),
            new ShipAssemblyRecipe(Component.translatable("gui.historicships.recipe.napoleon_ship"), () -> new ItemStack(NapoleonShipMod.NAPOLEON_SHIP_ITEM.get()), List.of(tag(ItemTags.LOGS, 80), item(Items.IRON_INGOT, 32), item(Items.IRON_BLOCK, 4), item(Items.BLAST_FURNACE, 1), item(Items.REDSTONE, 16), item(Items.COPPER_INGOT, 16), item(Items.WHITE_WOOL, 8), item(Items.BLUE_DYE, 2), item(Items.RED_DYE, 2), item(Items.WHITE_DYE, 2), item(Items.COAL_BLOCK, 4)), ShipsConfig.NAPOLEON_SHIP));

    private static CountedNeed item(ItemLike item, int count) {
        Item resolved = item.asItem();
        return new CountedNeed(stack -> stack.is(resolved), count, Component.translatable(resolved.getDescriptionId()));
    }

    private static CountedNeed tag(TagKey<Item> tag, int count) {
        return new CountedNeed(stack -> stack.is(tag), count, Component.translatable("gui.historicships.need." + tag.location().getPath()));
    }

    public record CountedNeed(Predicate<ItemStack> predicate, int count, Component label) {
        public boolean test(ItemStack stack) {
            return this.predicate.test(stack);
        }
    }
}
