/*
 *     Copyright (c) 2015, NeumimTo https://github.com/NeumimTo
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package cz.neumimto.rpg.events.skills;

import cz.neumimto.rpg.events.CancellableEvent;
import cz.neumimto.rpg.players.IActiveCharacter;
import cz.neumimto.rpg.scripting.JsBinding;
import cz.neumimto.rpg.skills.ISkill;

/**
 * Created by NeumimTo on 26.7.2015.
 */
@JsBinding(JsBinding.Type.CLASS)
public class SkillLearnEvent extends CancellableEvent {

	IActiveCharacter character;
	ISkill skill;

	public SkillLearnEvent(IActiveCharacter character, ISkill skill) {
		this.character = character;
		this.skill = skill;
	}

	public IActiveCharacter getCharacter() {
		return character;
	}

	public ISkill getSkill() {
		return skill;
	}
}
