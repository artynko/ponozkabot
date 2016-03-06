import bwapi.Unit;


public class Goliath extends FightingUnit {

	public Goliath(Unit unit) {
		super(unit);
	}

	@Override
	public boolean isSupport() {
		return false;
	}

}
