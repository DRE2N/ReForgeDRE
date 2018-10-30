/*
 * Copyright (C) 2017-2018 Daniel Saukel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.erethon.reforgedre.crafting;

import de.erethon.reforgedre.ReConfig;
import de.erethon.reforgedre.ReForgeDRE;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * @author Daniel Saukel
 */
public class AnvilListener implements Listener {

    private ReForgeDRE plugin;

    public AnvilListener(ReForgeDRE plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAnvilInteract(PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        if (clicked == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!AdvancedWorkbench.isAdvancedWorkbench(clicked)) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().openInventory(new AdvancedWorkbench(event.getPlayer()).getGUI());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity player = event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        for (final AdvancedWorkbench anvil : AdvancedWorkbench.cache) {
            if (event.getInventory() != null && anvil.getGUI().equals(event.getInventory())) {
                if (AdvancedWorkbench.SWITCH.equals(clicked)) {
                    event.setCancelled(true);
                    ((Player) player).playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                    for (ItemStack stack : event.getInventory().getContents()) {
                        if (stack != null && !AdvancedWorkbench.PLACEHOLDER.equals(stack)
                                && !AdvancedWorkbench.SWITCH.equals(stack) && !stack.equals(anvil.getGUI().getItem(AdvancedWorkbench.RESULT_SLOT))) {
                            player.getWorld().dropItem(player.getLocation(), stack);
                        }
                    }
                    anvil.nextType();
                } else if (AdvancedWorkbench.PLACEHOLDER.equals(clicked)) {
                    event.setCancelled(true);
                } else if (event.getSlot() == AdvancedWorkbench.RESULT_SLOT) {
                    event.setCancelled(true);
                    if (anvil.getResult() != null) {
                        new ForgingGame(plugin, (Player) player, anvil.getResult(), anvil.getMaterialType(), anvil.getAccessory()).start();
                    }
                } else {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            anvil.checkRecipes();
                        }
                    }.runTaskLater(plugin, 1);
                }
                break;
            }
        }
        for (ForgingGame game : ForgingGame.cache) {
            if (event.getInventory() != null && game.getGUI().equals(event.getInventory())) {
                if (ForgingGame.FURNACE.equals(clicked) || ForgingGame.WATER.equals(clicked)) {
                    event.setCancelled(true);
                } else if (clicked != null && clicked.getType() != Material.AIR) {
                    event.setCancelled(true);
                    game.finish(event.getCurrentItem());
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        for (final AdvancedWorkbench anvil : AdvancedWorkbench.cache) {
            if (event.getInventory() != null && anvil.getGUI().equals(event.getInventory())) {
                if (AdvancedWorkbench.PLACEHOLDER.equals(event.getCursor())) {
                    event.setCancelled(true);
                } else if (event.getRawSlots().contains(AdvancedWorkbench.RESULT_SLOT)) {
                    event.setCancelled(true);
                    if (anvil.getResult() != null) {
                        new ForgingGame(plugin, (Player) event.getWhoClicked(), anvil.getResult(), anvil.getMaterialType(), anvil.getAccessory()).start();
                    }
                } else {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            anvil.checkRecipes();
                        }
                    }.runTaskLater(plugin, 1);
                }
                break;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (event.getPlayer().getOpenInventory().getTitle().equals(ReConfig.ANVIL_FORGING)) {
                    return;
                }
                for (AdvancedWorkbench anvil : AdvancedWorkbench.cache) {
                    if (anvil.getGUI().equals(event.getInventory())) {
                        for (ItemStack stack : event.getInventory().getContents()) {
                            if (stack != null && !AdvancedWorkbench.PLACEHOLDER.equals(stack)
                                    && !AdvancedWorkbench.SWITCH.equals(stack) && !stack.equals(anvil.getGUI().getItem(AdvancedWorkbench.RESULT_SLOT))) {
                                event.getPlayer().getWorld().dropItem(event.getPlayer().getLocation(), stack);
                            }
                        }
                        AdvancedWorkbench.cache.remove(anvil);
                        break;
                    }
                }
            }
        }.runTaskLater(plugin, 1L);
    }

}
