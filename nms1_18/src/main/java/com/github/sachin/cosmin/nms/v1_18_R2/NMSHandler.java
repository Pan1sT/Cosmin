package com.github.sachin.cosmin.nms.v1_18_R2;

import com.github.sachin.cosmin.nbtapi.nms.NMSHelper;
import net.minecraft.nbt.NBTTagCompound;
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class NMSHandler extends NMSHelper {


    private net.minecraft.world.item.ItemStack nmsItem;
    private NBTTagCompound compound;

    public NMSHandler(){

    }

    public NMSHandler(ItemStack item){
        if(item == null) return;
        ItemStack bukkitItem = item.clone();

        this.nmsItem = CraftItemStack.asNMSCopy(bukkitItem);
        this.compound = (nmsItem.s()) ? nmsItem.t() : new NBTTagCompound();
    }


    @Override
    public NMSHelper newItem(ItemStack item) {
        return new NMSHandler(item);
    }

    @Override
    public void setString(String key, String value) {
        compound.a(key,value);
    }

    @Override
    public void setBoolean(String key, boolean value) {
        compound.a(key,value);
    }

    @Override
    public void setInt(String key, int value) {
        compound.a(key,value);
    }

    @Override
    public void setLong(String key, long value) {
        compound.a(key,value);
    }

    @Override
    public void setDouble(String key, double value) {
        compound.a(key,value);
    }

    @Override
    public String getString(String key) {
        return compound.l(key);
    }

    @Override
    public boolean getBoolean(String key) {
        return compound.q(key);
    }

    @Override
    public int getInt(String key) {
        return compound.h(key);
    }

    @Override
    public long getLong(String key) {
        return compound.i(key);
    }

    @Override
    public double getDouble(String key) {
        return compound.k(key);
    }

    @Override
    public boolean hasKey(String key) {
        return compound.b(key);
    }

    @Override
    public org.bukkit.inventory.ItemStack getItem() {
        nmsItem.b(compound);
        return CraftItemStack.asBukkitCopy(nmsItem);
    }

    @Override
    public void removeKey(String key) {
        compound.r(key);
    }
}