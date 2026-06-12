package at.minich.opserver.shop;

import org.bukkit.Material;

public record ShopItem(Material material, double buyPrice, double sellPrice) {
    public double sellPrice() { return buyPrice / 2.0; }
}
