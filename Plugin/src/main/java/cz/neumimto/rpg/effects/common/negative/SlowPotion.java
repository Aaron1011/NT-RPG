package cz.neumimto.rpg.effects.common.negative;

import cz.neumimto.rpg.effects.IEffectConsumer;
import cz.neumimto.rpg.effects.common.mechanics.RPGPotionEffect;
import org.spongepowered.api.effect.potion.PotionEffect;
import org.spongepowered.api.effect.potion.PotionEffectTypes;

/**
 * Created by NeumimTo on 13.8.2017.
 */
public class SlowPotion extends RPGPotionEffect {

	public static final String name = "SlowPotion";

	public SlowPotion(IEffectConsumer iEffectConsumer, long duration, int amplifier) {
		super(name, iEffectConsumer, duration, PotionEffect.builder()
				.amplifier(amplifier).potionType(PotionEffectTypes.SLOWNESS)
				.ambience(true)
		);
	}
}
