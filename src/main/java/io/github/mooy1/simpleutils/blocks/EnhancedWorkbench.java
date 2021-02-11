package io.github.mooy1.simpleutils.blocks;

import io.github.mooy1.infinitylib.PluginUtils;
import io.github.mooy1.infinitylib.abstracts.AbstractContainer;
import io.github.mooy1.infinitylib.filter.FilterType;
import io.github.mooy1.infinitylib.filter.MultiFilter;
import io.github.mooy1.infinitylib.filter.RecipeFilter;
import io.github.mooy1.infinitylib.presets.MenuPreset;
import io.github.mooy1.infinitylib.presets.RecipePreset;
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import me.mrCookieSlime.Slimefun.cscorelib2.item.CustomItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EnhancedWorkbench extends AbstractContainer implements Listener {
    
    private static final int[] INPUT_SLOTS = MenuPreset.craftingInput;
    private static final int OUTPUT_SLOT = 24;
    
    private static final ItemStack NO_OUTPUT = new CustomItem(Material.BARRIER, " ");
    public static final SlimefunItemStack ITEM = new SlimefunItemStack(
            "ENHANCED_WORKBENCH",
            Material.CRAFTING_TABLE,
            "&6Enhanced Workbench",
            "&7Can craft both vanilla and slimefun recipes"
    );
    
    private final Map<MultiFilter, ItemStack> recipes = new HashMap<>();
    private final Map<UUID, BlockMenu> openMenus = new HashMap<>();
    
    public EnhancedWorkbench(Category category) {
        super(category, ITEM, RecipeType.ENHANCED_CRAFTING_TABLE, RecipePreset.firstItem(new ItemStack(Material.CRAFTING_TABLE)));
        
        PluginUtils.registerListener(this);
        
        SlimefunPlugin.getMinecraftRecipeService().subscribe(recipeSnapshot -> {
            for (ShapedRecipe recipe : recipeSnapshot.getRecipes(ShapedRecipe.class)) {
                for (RecipeFilter filter : RecipeFilter.fromRecipe(recipe)) {
                    this.recipes.put(filter, recipe.getResult());
                }
            }
            for (ShapelessRecipe recipe : recipeSnapshot.getRecipes(ShapelessRecipe.class)) {
                for (RecipeFilter filter : RecipeFilter.fromRecipe(recipe)) {
                    this.recipes.put(filter, recipe.getResult());
                }
            }
        });
        
        PluginUtils.runSync(() -> {
            RecipeType[] types = {RecipeType.ENHANCED_CRAFTING_TABLE, RecipeType.ARMOR_FORGE, RecipeType.MAGIC_WORKBENCH};
            for (RecipeType type : types) {
                List<ItemStack[]> list = ((MultiBlockMachine) type.getMachine()).getRecipes();
                for (int i = 0 ; i < list.size() ; i += 2) {
                    ItemStack[] recipe = list.get(i);
                    if (recipe.length != 9) {
                        continue;
                    }
                    for (int j = 0 ; j < 9 ; j++) {
                        if (recipe[j] != null) {
                            this.recipes.put(new MultiFilter(FilterType.IGNORE_AMOUNT, recipe), list.get(i + 1)[0]);
                            break;
                        }
                    }
                }
            }
        }, 100);
    }

    @Override
    protected void onBreak(@Nonnull BlockBreakEvent e, @Nonnull BlockMenu menu, @Nonnull Location l) {
        menu.dropItems(l, INPUT_SLOTS);
    }
    
    private void craft(Player p, BlockMenu menu, boolean max) { 
        MultiFilter input = MultiFilter.fromMenu(FilterType.IGNORE_AMOUNT, menu, INPUT_SLOTS);
        ItemStack output = this.recipes.get(input);
        if (output == null) {
            return;
        }
        int amount = 65;
        // find smallest amount greater than 0
        for (int i : input.getAmounts()) {
            if (i > 0 && i < amount) {
                amount = i;
            }
        }
        if (amount == 65) {
            // this would only happen if there was a empty registered recipe
            return;
        }
        if (max) {
            // calc amounts
            int total = output.getAmount() * amount;
            int fullStacks = total / output.getMaxStackSize();
            int partialStack = total % output.getMaxStackSize();
            
            // create array of items
            ItemStack[] arr;
            if (partialStack == 0) {
                arr = new ItemStack[fullStacks];
            } else {
                arr = new ItemStack[fullStacks + 1];
                arr[fullStacks] = new CustomItem(output, amount);
            }
            
            // fill with full stacks
            while (fullStacks-- != 0) {
                arr[fullStacks] = new CustomItem(output, output.getMaxStackSize());
            }
            
            // output and drop remaining
            Map<Integer, ItemStack> remaining = p.getInventory().addItem(arr);
            for (ItemStack stack : remaining.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), stack);
            }
            
            // refresh
            refreshOutput(menu);
            
        } else {
            // output and drop remaining
            Map<Integer, ItemStack> remaining = p.getInventory().addItem(output.clone());
            for (ItemStack stack : remaining.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), stack);
            }
            
            // refresh if 1 item will run out
            if (amount == 1) {
                refreshOutput(menu);
            }
            
            amount = 1;
        }
        // consume
        for (int i = 0 ; i < 9 ; i++) {
            if (input.getAmounts()[i] != 0) {
                menu.consumeItem(INPUT_SLOTS[i], amount, true);
            }
        }
    }
    
    private void refreshOutput(@Nonnull BlockMenu menu) {
        PluginUtils.runSync(() -> {
            ItemStack output = this.recipes.get(MultiFilter.fromMenu(FilterType.IGNORE_AMOUNT, menu, INPUT_SLOTS));
            if (output == null) {
                menu.replaceExistingItem(OUTPUT_SLOT, NO_OUTPUT);
            } else {
                menu.replaceExistingItem(OUTPUT_SLOT, output);
            }
        });
    }

    @Override
    protected void setupMenu(@Nonnull BlockMenuPreset blockMenuPreset) {
        blockMenuPreset.drawBackground(MenuPreset.borderItemInput, MenuPreset.craftingInputBorder);
        blockMenuPreset.drawBackground(MenuPreset.borderItemOutput, new int[] {14, 15, 16, 23, 25, 32, 33, 34});
        blockMenuPreset.drawBackground(new int[] {5, 6, 7, 8, 17, 26, 35, 41, 42, 43, 44});
    }

    @Override
    protected void onNewInstance(@Nonnull BlockMenu menu, @Nonnull Block b) {
        menu.addMenuOpeningHandler(p -> this.openMenus.put(p.getUniqueId(), menu));
        menu.addMenuCloseHandler(p -> this.openMenus.remove(p.getUniqueId()));
        menu.addPlayerInventoryClickHandler((p, slot, item, action) -> {
            if (action.isShiftClicked()) {
                refreshOutput(menu);
            }
            return true;
        });
        for (int i : INPUT_SLOTS) {
            menu.addMenuClickHandler(i, (p, slot, item, action) -> {
                refreshOutput(menu);
                return true;
            });
        }
        menu.addMenuClickHandler(OUTPUT_SLOT, (p, slot, item, action) -> {
            craft(p, menu, action.isShiftClicked());
            return false;
        });
        refreshOutput(menu);
    }
    
    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        BlockMenu menu = this.openMenus.get(e.getWhoClicked().getUniqueId());
        if (menu != null) {
            refreshOutput(menu);
        }
    }

    @Nonnull
    @Override
    protected int[] getTransportSlots(@Nonnull DirtyChestMenu dirtyChestMenu, @Nonnull ItemTransportFlow itemTransportFlow, ItemStack itemStack) {
        return new int[0];
    }
    
}
