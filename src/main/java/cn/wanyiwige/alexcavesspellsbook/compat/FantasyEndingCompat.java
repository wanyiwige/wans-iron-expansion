package cn.wanyiwige.alexcavesspellsbook.compat;

import io.redspace.ironsspellbooks.api.spells.SpellRarity;

/** Uses Fantasy Ending's actual added enum value without bundling its classes. */
public final class FantasyEndingCompat {
    private FantasyEndingCompat() {
    }

    public static SpellRarity originRarity() {
        return OriginHolder.RARITY;
    }

    private static final class OriginHolder {
        private static final SpellRarity RARITY = resolveOrigin();

        private static SpellRarity resolveOrigin() {
            try {
                Class<?> context = Class.forName("com.mega.uom.common.context.IronSpellContext");
                Object value = context.getMethod("ORIGIN").invoke(null);
                if (value instanceof SpellRarity rarity && "ORIGIN".equals(rarity.name())) {
                    return rarity;
                }
                throw new IllegalStateException("Fantasy Ending did not initialize the ORIGIN spell rarity");
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Supernova requires Fantasy Ending's IronSpellContext.ORIGIN()", exception);
            }
        }
    }
}
