package cz.neumimto.rpg.effects.common.stacking;

/**
 * Created by NeumimTo on 5.7.2017.
 */
public interface UnstackableEffectData<T> extends Comparable<T> {

	default boolean isInferiorTo(T replacement) {
		return this != replacement && compareTo(replacement) > 0;
	}
}
