package io.github.mooy1.gridfoundation.implementation.generators;

import io.github.mooy1.gridfoundation.implementation.grid.AbstractGridContainer;
import io.github.mooy1.gridfoundation.implementation.grid.GridGenerator;
import io.github.mooy1.gridfoundation.implementation.grid.PowerGrid;
import io.github.mooy1.gridfoundation.implementation.upgrades.UpgradeManager;
import io.github.mooy1.gridfoundation.setup.Categories;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractGridGenerator extends AbstractGridContainer {

    private final Map<Location, GridGenerator> generators = new HashMap<>();
    private final Map<Location, PowerGrid> grids = new HashMap<>();
    
    public AbstractGridGenerator(SlimefunItemStack item, ItemStack[] recipe, int statusSlot) {
        super(Categories.GENERATORS, item, statusSlot, RecipeType.ENHANCED_CRAFTING_TABLE, recipe);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void onBreak(Player p, Block b, BlockMenu menu, PowerGrid grid) {
        @Nullable GridGenerator generator = this.generators.remove(b.getLocation());
        if (generator != null) {
            grid.getGenerators().remove(generator);
        }
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void onNewInstance(@Nonnull BlockMenu menu, @Nonnull Block b, @Nonnull PowerGrid grid) {
        GridGenerator gen = new GridGenerator(b.getLocation().hashCode());
        this.generators.put(b.getLocation(), gen);
        grid.getGenerators().add(gen);
        menu.replaceExistingItem(this.statusSlot, grid.getStatusItem(true, 0));
    }

    @Override
    public final void tick(@Nonnull Block block, @Nonnull BlockMenu blockMenu, @Nonnull PowerGrid grid) {
        @Nullable GridGenerator generator = this.generators.get(block.getLocation());
        if (generator != null) {
            int generation = UpgradeManager.getType(block.getLocation()).level * getGeneration(blockMenu, block);
            generator.setGeneration(generation);
            if (blockMenu.hasViewer()) {
                blockMenu.replaceExistingItem(this.statusSlot, grid.getStatusItem(true, generation));
            }
        }
    }
    
    public abstract int getGeneration(@Nonnull BlockMenu menu, @Nonnull Block b);

}