var {{skill.id}}_executor = new (Java.extend(PassiveScriptSkillHandler, {
    init: function(_caster, _info, _modifier, _context) {
        {{userScript}}
    }
}));