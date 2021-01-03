package io.github.mooy1.gridfoundation.implementation.consumers.presses;

import io.github.mooy1.infinitylib.PluginUtils;
import io.github.mooy1.infinitylib.filter.ItemFilter;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import me.mrCookieSlime.Slimefun.cscorelib2.collections.Pair;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PlatePress extends AbstractPress {

    public static final Pair<Map<ItemFilter, Pair<ItemStack, Integer>>, List<ItemStack>> plateRecipes = new Pair<>(new HashMap<>(), new ArrayList<>());
    private static final SlimefunItemStack ITEM = new SlimefunItemStack(

    );
    public static final RecipeType TYPE = new RecipeType(PluginUtils.getKey("plate_press"), ITEM);
    
    public PlatePress() {
        super(ITEM, 8, plateRecipes, new ItemStack[] {

        });
    }
    
}
