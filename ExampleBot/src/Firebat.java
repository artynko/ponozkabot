import bwapi.Unit;


public class Firebat extends FightingUnit {

	public Firebat(Unit unit) {
		super(unit);
	}

	@Override
	public boolean isSupport() {
		return false;
	}

}
