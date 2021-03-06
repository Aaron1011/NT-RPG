package cz.neumimto.rpg.skills.parents;

import cz.neumimto.rpg.skills.scripting.PassiveScriptSkillHandler;

/**
 * Created by NeumimTo on 7.10.2018.
 */
public interface IPassiveScriptSkill extends ScriptSkill<PassiveScriptSkillHandler> {

	@Override
	default String getTemplateName() {
		return "templates/passive.js";
	}
}
