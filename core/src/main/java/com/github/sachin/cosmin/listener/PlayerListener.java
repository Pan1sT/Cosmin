package com.github.sachin.cosmin.listener;

import java.util.Arrays;

import com.github.sachin.cosmin.Cosmin;
import com.github.sachin.cosmin.database.PlayerData;
import com.github.sachin.cosmin.player.CosminPlayer;
import com.github.sachin.cosmin.utils.CItemSlot;
import com.github.sachin.cosmin.utils.CosminConstants;
import com.github.sachin.cosmin.utils.InventoryUtils;
import com.github.sachin.cosmin.utils.ItemBuilder;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;



public class PlayerListener implements Listener{

    private Cosmin plugin;

    public PlayerListener(Cosmin plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent e){
        Player player = e.getPlayer();
        plugin.getEntityIdMap().put(player.getEntityId(), player);
        new BukkitRunnable(){
            @Override
            public void run() {
                if(plugin.getConfigUtils().isMySQLEnabled()){
                    if(plugin.MySQL().isConnected()){
                        PlayerData playerData = new PlayerData(player.getUniqueId());
                        if(playerData.playerExists()){
                            CosminPlayer cosminPlayer = new CosminPlayer(player.getUniqueId(),Arrays.asList(InventoryUtils.base64ToItemStackArray(playerData.getPlayerData())));
                            cosminPlayer.setPurchasedItems(playerData.getPurchasedItems("PurchasedItems"));
                            cosminPlayer.setPurchasedSets(playerData.getPurchasedItems("PurchasedSets"));
                            cosminPlayer.clearNonExsistantArmorItems();
                            cosminPlayer.computeAndPutEquipmentPairList();
                            plugin.getPlayerManager().addPlayer(cosminPlayer);
                            cosminPlayer.setFakeSlotItems();
                            return;
                        }
                    }
                }
                if(plugin.getPlayerManager().containsPlayer(player)){
                    CosminPlayer cosminPlayer = plugin.getPlayerManager().getPlayer(player);
                    cosminPlayer.setFakeSlotItems();
                    cosminPlayer.sendPacketWithinRange(60);
                }
            }
        }.runTaskLater(plugin, 40);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e){
        Player player = e.getEntity();
        if(!plugin.getConfig().getBoolean("drop-items-on-death",true)) return;
        if(player.hasPermission("cosmin.deathdrops.bypass")) return;
        if(plugin.getPlayerManager().containsPlayer(player)){
            CosminPlayer cPlayer = plugin.getPlayerManager().getPlayer(player);
            for(int i = 0; i< cPlayer.getCosminInvContents().size();i++){
                if(CosminConstants.COSMIN_ARMOR_SLOTS.contains(i)){
                    ItemStack item = cPlayer.getCosminInvContents().get(i);
                    if(item == null) continue;
                    if(!ItemBuilder.isHatItem(item)){
                        e.getDrops().add(item);
                        cPlayer.removeItem(i);
                    }
                }
            }
            cPlayer.computeAndPutEquipmentPairList();
            cPlayer.sendPacketWithinRange(60);
            cPlayer.setFakeSlotItems();
        }
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent e){
        Player player = e.getPlayer();
        plugin.getEntityIdMap().remove(player.getEntityId());
        if(plugin.getConfigUtils().isMySQLEnabled() && plugin.getPlayerManager().containsPlayer(player)){
            PlayerData playerData = new PlayerData(player.getUniqueId());
            CosminPlayer cPlayer = plugin.getPlayerManager().getPlayer(player);
            String data = InventoryUtils.itemStackListToBase64(cPlayer.getCosminInvContents());
            playerData.updatePlayerData(data,cPlayer.getPurchasedItems(),cPlayer.getPurchasedSets());
            plugin.getPlayerManager().removePlayer(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST,ignoreCancelled = true)
    public void playerSwapItemEvent(PlayerSwapHandItemsEvent e){
        Player player = e.getPlayer();
        if(plugin.getPlayerManager().containsPlayer(player)){
            CosminPlayer cosminPlayer = plugin.getPlayerManager().getPlayer(player);
            if(!cosminPlayer.isEnabled(CItemSlot.OFFHAND)){
                cosminPlayer.setSlotItem(CItemSlot.OFFHAND, e.getOffHandItem());

            }
        }
    }

    @EventHandler
    public void playerExpChangeEvent(PlayerExpChangeEvent e){
        Player player = e.getPlayer();
        if(plugin.getPlayerManager().containsPlayer(player)){
            CosminPlayer cosminPlayer = plugin.getPlayerManager().getPlayer(player);
            cosminPlayer.setFakeSlotItems();
        }
    }

    @EventHandler
    public void onEat(PlayerItemConsumeEvent e){
        Player player = e.getPlayer();
        if(plugin.getPlayerManager().containsPlayer(player)){
            new BukkitRunnable(){
                public void run() {
                    if(player.isOnline()){
                        CosminPlayer cosminPlayer = plugin.getPlayerManager().getPlayer(player);
                        cosminPlayer.setFakeSlotItems();
                    }
                };
            }.runTaskLater(plugin, 1);
        }
    }

    @EventHandler
    public void playerEquipArmorEvent(PlayerInteractEvent e){
        if(e.getItem() == null || e.getHand() != EquipmentSlot.HAND) return;
        Player player = e.getPlayer();
        if(!plugin.getPlayerManager().containsPlayer(player)) return;
        CosminPlayer cPlayer = plugin.getPlayerManager().getPlayer(player);
        ItemStack item = e.getItem();
        String itemName = item.getType().name();
        for(String s: Arrays.asList("HELMET","CHESTPLATE","LEGGINGS","BOOTS","ELYTRA")){
            if(itemName.endsWith(s)){
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        cPlayer.computeAndPutEquipmentPairList();
                        cPlayer.setFakeSlotItems();
                    }
                }.runTaskLater(plugin, 1);
                break;
            }
        }

    }


    @EventHandler
    public void onDimensionChange(PlayerPortalEvent e){
        Player player = e.getPlayer();
        
        CosminPlayer cPlayer = plugin.getPlayerManager().getPlayer(player);
        if(cPlayer != null && player.getGameMode() != GameMode.CREATIVE){
            new BukkitRunnable(){
                
                public void run() {cPlayer.computeAndPutEquipmentPairList();cPlayer.setFakeSlotItems();};
            }.runTaskLater(plugin, 10);
        }
        
    }
    
    @EventHandler
    public void onTeleport(PlayerTeleportEvent e){
        Player player = e.getPlayer();
        CosminPlayer cPlayer = plugin.getPlayerManager().getPlayer(player);
        if(cPlayer != null){
            new BukkitRunnable() {
                @Override
                public void run() {
                    cPlayer.computeAndPutEquipmentPairList();
                    cPlayer.setFakeSlotItems();
                    cPlayer.sendPacketWithinRange(60, player);
                
                }
            }.runTaskLater(plugin,1);
        }
    }


    @EventHandler
    public void playerGameModeChangeEvent(PlayerGameModeChangeEvent e){
        Player player = e.getPlayer();
        if(!plugin.getPlayerManager().containsPlayer(player)) return;
        CosminPlayer cPlayer = plugin.getPlayerManager().getPlayer(player);
        GameMode oldgm = player.getGameMode();
        GameMode newgm = e.getNewGameMode();
        if(oldgm == GameMode.SURVIVAL && (newgm == GameMode.CREATIVE || newgm == GameMode.SPECTATOR)){
            cPlayer.equipOrignalArmor();
        }
        else if((oldgm == GameMode.CREATIVE || oldgm == GameMode.SPECTATOR) && newgm == GameMode.SURVIVAL){
            new BukkitRunnable(){
                public void run() {
                    cPlayer.computeAndPutEquipmentPairList();
                    cPlayer.setFakeSlotItems();
                };
            }.runTaskLater(plugin, 2);
        }

    }

   


    
}
