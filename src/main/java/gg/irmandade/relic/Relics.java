package gg.irmandade.relic;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemEnchantments;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.Registries;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/** Definição das 20 relíquias (slot 1..20 da GUI do /Relics). */
public final class Relics {
    public static final String TAG = "imdd_relic";

    // ids internos (guardados em custom_data)
    public static final String BIZONHO_GRAVETO = "bizonho_graveto";
    public static final String FAZENDEIRO = "fazendeiro";
    public static final String BIZONHO_MACHADO = "bizonho_machado";
    public static final String MINERADOR = "minerador";
    public static final String CACADOR = "cacador";
    public static final String SORTE = "sorte";
    public static final String GUERREIRO = "guerreiro";
    public static final String DEFESA = "defesa";
    public static final String FIM = "fim";
    public static final String INVASOR = "invasor";
    public static final String IMORTALIDADE = "imortalidade";
    public static final String OCEANO = "oceano";
    public static final String EXPLORADOR = "explorador";
    public static final String ESPIAO = "espiao";
    public static final String VENTO = "vento";
    public static final String WARDEN = "warden";
    public static final String STOMPER = "stomper";
    public static final String DRAGAO = "dragao";
    public static final String INVISIBILIDADE = "invisibilidade";
    public static final String NETHER = "nether";

    private Relics() {}

    // ---------- identificação ----------
    public static String idOf(ItemStack stack) {
        if (stack.isEmpty()) return "";
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return "";
        return data.copyTag().getStringOr(TAG, "");
    }

    public static boolean is(ItemStack stack, String id) {
        return !stack.isEmpty() && idOf(stack).equals(id);
    }

    /** Qualquer item com unbreakable={} é indestrutível. */
    public static boolean isIndestructible(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.UNBREAKABLE);
    }

    // ---------- construção ----------
    private static Style name(String hex) {
        return Style.EMPTY.withColor(TextColor.parseColor(hex).result().orElse(TextColor.fromRgb(0xFFFFFF)))
                .withBold(true).withUnderlined(true);
    }

    private static Holder<Enchantment> ench(HolderLookup.Provider reg, ResourceKey<Enchantment> key) {
        return reg.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static final class B {
        final ItemStack stack;
        final HolderLookup.Provider reg;
        final ItemEnchantments.Mutable ench;
        boolean hasEnch = false;
        final LinkedHashSet<net.minecraft.core.component.DataComponentType<?>> hidden = new LinkedHashSet<>();

        B(HolderLookup.Provider reg, Item item, String id, String nameText, String hex) {
            this.reg = reg;
            this.stack = new ItemStack(item);
            this.ench = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            CompoundTag tag = new CompoundTag();
            tag.putString(TAG, id);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(nameText).withStyle(name(hex)));
            stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
            hidden.add(DataComponents.UNBREAKABLE);
        }

        B e(ResourceKey<Enchantment> key, int level) {
            ench.set(ench(reg, key), level);
            hasEnch = true;
            return this;
        }

        B glint() { stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true); return this; }

        B hide(net.minecraft.core.component.DataComponentType<?> c) { hidden.add(c); return this; }

        B cooldown(String group, float seconds) {
            stack.set(DataComponents.USE_COOLDOWN,
                    new UseCooldown(seconds, Optional.of(Identifier.fromNamespaceAndPath("irmandade", group))));
            return this;
        }

        ItemStack done() {
            if (hasEnch) stack.set(DataComponents.ENCHANTMENTS, ench.toImmutable());
            stack.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(false, hidden));
            return stack;
        }
    }

    /** Retorna as 20 relíquias na ordem dos slots (índice 0 = slot 1). */
    public static List<ItemStack> all(HolderLookup.Provider r) {
        List<ItemStack> l = new ArrayList<>();

        // 1
        l.add(new B(r, Items.STICK, BIZONHO_GRAVETO, "Relíquia do Bizonho", "#b5853f")
                .e(Enchantments.KNOCKBACK, 50).hide(DataComponents.ATTRIBUTE_MODIFIERS).done());
        // 2
        l.add(new B(r, Items.LEATHER_LEGGINGS, FAZENDEIRO, "Relíquia do Fazendeiro", "#037b0d")
                .e(Enchantments.PROTECTION, 8).done());
        // 3
        l.add(new B(r, Items.DIAMOND_AXE, BIZONHO_MACHADO, "Relíquia do Bizonho", "#b6893f")
                .e(Enchantments.KNOCKBACK, 150).done());
        // 4
        l.add(new B(r, Items.NETHERITE_PICKAXE, MINERADOR, "Relíquia do Minerador", "#f9f40f")
                .e(Enchantments.EFFICIENCY, 10).e(Enchantments.FORTUNE, 3).done());
        // 5
        l.add(new B(r, Items.CROSSBOW, CACADOR, "Relíquia do Caçador", "#f6024e")
                .e(Enchantments.MULTISHOT, 3).e(Enchantments.PIERCING, 10).e(Enchantments.QUICK_CHARGE, 5).done());
        // 6
        l.add(new B(r, Items.RABBIT_FOOT, SORTE, "Relíquia da Sorte", "#07f806").glint().done());
        // 7
        l.add(new B(r, Items.NETHERITE_SWORD, GUERREIRO, "Relíquia do Guerreiro", "#ed020c").glint()
                .e(Enchantments.BANE_OF_ARTHROPODS, 5).e(Enchantments.KNOCKBACK, 3).e(Enchantments.LOOTING, 3)
                .e(Enchantments.SHARPNESS, 10).e(Enchantments.SMITE, 5).e(Enchantments.SWEEPING_EDGE, 3).done());
        // 8
        l.add(new B(r, Items.SHIELD, DEFESA, "Relíquia da Defesa", "#807b81").glint()
                .e(Enchantments.THORNS, 5).done());
        // 9
        l.add(new B(r, Items.ELYTRA, FIM, "Relíquia do Fim", "#f6faa1").glint()
                .e(Enchantments.PROTECTION, 15).done());
        // 10 (sem tooltip_display no comando original: mostra tudo, exceto nada escondido)
        ItemStack invasor = new B(r, Items.LEATHER_LEGGINGS, INVASOR, "Relíquia do Invasor", "#817c7e").glint()
                .e(Enchantments.BINDING_CURSE, 1).e(Enchantments.PROTECTION, 8).e(Enchantments.VANISHING_CURSE, 1).done();
        invasor.set(DataComponents.DYED_COLOR, new DyedItemColor(4212811));
        invasor.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        l.add(invasor);
        // 11 (totem sem DEATH_PROTECTION: a lógica é feita pelo mod, com cooldown próprio)
        ItemStack totem = new B(r, Items.TOTEM_OF_UNDYING, IMORTALIDADE, "Relíquia da Imortalidade", "#3fcc7b").glint()
                .cooldown("relic_imortalidade", 10f).done();
        totem.remove(DataComponents.DEATH_PROTECTION);
        l.add(totem);
        // 12
        l.add(new B(r, Items.TRIDENT, OCEANO, "Relíquia do Oceano", "#29eff9").glint()
                .e(Enchantments.CHANNELING, 1).e(Enchantments.IMPALING, 10).e(Enchantments.LOOTING, 3)
                .e(Enchantments.LOYALTY, 3).e(Enchantments.SHARPNESS, 5).done());
        // 13
        l.add(new B(r, Items.DIAMOND_BOOTS, EXPLORADOR, "Relíquia do Explorador", "#fcc814").glint()
                .e(Enchantments.FROST_WALKER, 2).done());
        // 14 (biscoito não comestível: botão direito ativa o efeito)
        ItemStack cookie = new B(r, Items.COOKIE, ESPIAO, "Relíquia do Espião", "#e09451").glint()
                .cooldown("relic_espiao", 1200f).done();
        cookie.remove(DataComponents.FOOD);
        cookie.remove(DataComponents.CONSUMABLE);
        l.add(cookie);
        // 15
        l.add(new B(r, Items.WIND_CHARGE, VENTO, "Relíquia do Deus do Vento", "#e9e6f4").glint()
                .cooldown("relic_vento", 0.25f).done());
        // 16
        l.add(new B(r, Items.RECOVERY_COMPASS, WARDEN, "Relíquia do Warden", "#13888e").glint()
                .cooldown("relic_warden", 10f).done());
        // 17
        l.add(new B(r, Items.MACE, STOMPER, "Relíquia do Stomper", "#302b37").glint()
                .e(Enchantments.DENSITY, 5).hide(DataComponents.ENCHANTMENTS).done());
        // 18
        l.add(new B(r, Items.DRAGON_EGG, DRAGAO, "Relíquia do Dragão", "#6a1ef6").glint().done());
        // 19
        l.add(new B(r, Items.LEATHER_BOOTS, INVISIBILIDADE, "Relíquia da Invisibilidade", "#ffffff").glint()
                .e(Enchantments.PROTECTION, 10).done());
        // 20
        l.add(new B(r, Items.GOLDEN_HELMET, NETHER, "Relíquia do Nether", "#eda411").glint()
                .e(Enchantments.PROTECTION, 8).done());
        return l;
    }
}
