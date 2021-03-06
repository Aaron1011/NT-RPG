package cz.neumimto.skills.active;

import cz.neumimto.core.ioc.Inject;
import cz.neumimto.effects.negative.PandemicEffect;
import cz.neumimto.rpg.IEntity;
import cz.neumimto.rpg.ResourceLoader;
import cz.neumimto.rpg.damage.SkillDamageSource;
import cz.neumimto.rpg.damage.SkillDamageSourceBuilder;
import cz.neumimto.rpg.effects.EffectService;
import cz.neumimto.rpg.entities.EntityService;
import cz.neumimto.rpg.players.IActiveCharacter;
import cz.neumimto.rpg.skills.ExtendedSkillInfo;
import cz.neumimto.rpg.skills.NDamageType;
import cz.neumimto.rpg.skills.SkillNodes;
import cz.neumimto.rpg.skills.SkillResult;
import cz.neumimto.rpg.skills.SkillSettings;
import cz.neumimto.rpg.skills.parents.ActiveSkill;
import cz.neumimto.rpg.skills.tree.SkillType;
import cz.neumimto.rpg.skills.mods.SkillContext;
import cz.neumimto.rpg.utils.Utils;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;

import java.util.Set;

/**
 * Created by NeumimTo on 6.8.2017.
 */
@ResourceLoader.Skill("ntrpg:pandemic")
public class Pandemic extends ActiveSkill {

	@Inject
	private EffectService effectService;

	@Inject
	private EntityService entityService;

	public void init() {
		super.init();
		SkillSettings settings = new SkillSettings();
		settings.addNode(SkillNodes.RADIUS, 10, 5);
		settings.addNode(SkillNodes.DURATION, 3000, 500);
		settings.addNode(SkillNodes.DAMAGE, 15, 3);
		settings.addNode(SkillNodes.PERIOD, 1500, -10);
		setSettings(settings);
		addSkillType(SkillType.AOE);
		addSkillType(SkillType.DISEASE);
		setDamageType(NDamageType.MAGICAL);
	}

	@Override
	public void cast(IActiveCharacter character, ExtendedSkillInfo info, SkillContext modifier) {
		float damage = getFloatNodeValue(info, SkillNodes.DAMAGE);
		int radius = getIntNodeValue(info, SkillNodes.RADIUS);
		long period = getLongNodeValue(info, SkillNodes.PERIOD);
		long duration = getLongNodeValue(info, SkillNodes.DURATION);
		Set<Entity> nearbyEntities = Utils.getNearbyEntities(character.getLocation(), radius);
		for (Entity entity : nearbyEntities) {
			if (Utils.isLivingEntity(entity)) {
				IEntity iEntity = entityService.get(entity);
				if (Utils.canDamage(character, (Living) entity)) {
					PandemicEffect effect = new PandemicEffect(character, iEntity, damage, duration, period);
					SkillDamageSource build = new SkillDamageSourceBuilder()
							.fromSkill(this)
							.setEffect(effect)
							.setCaster(character)
							.build();
					effect.setDamageSource(build);
					effectService.addEffect(effect, iEntity, this);
				}
			}
		}
		modifier.next(character, info, SkillResult.OK);
	}
}
