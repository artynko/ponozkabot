import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;

public class SiegeTank extends FightingUnit {
	int lastSiege = 0;

	public SiegeTank(Unit unit) {
		super(unit);
	}

	@Override
	public boolean isSupport() {
		return true;
	}

	@Override
	public void onFrame(Game game, Player player) {
		int range = UnitType.Terran_Siege_Tank_Siege_Mode.groundWeapon().maxRange() - 130;
//		game.drawCircleMap(unit.getPosition(), range, Color.Red);
		Position pos = squad.getMedianPosition();
		if (squad.getBehaviour() == Squad.Behaviour.DEFENSE) {
			pos = squad.getAttackPosition();
		}
		else if (squad.getUnits().size() < 8) {
			pos = squad.getLastPosition();
		}
		if (unit.getPosition().getDistance(pos) > TilePosition.SIZE_IN_PIXELS * 12) {
			if (unit.isSieged())
				unit.unsiege();
			super.onFrame(game, player);
			return;
		} else if (squad.getBehaviour() == Squad.Behaviour.DEFENSE && unit.getPoint().getDistance(pos) < TilePosition.SIZE_IN_PIXELS * 2) {
			unit.siege();
			return;
		}

		for (Unit u : game.getUnitsInRadius(unit.getPosition(), range)) {
			if (!u.getType().isFlyer() && u.isVisible() && !u.isCloaked() && u.getPlayer().equals(game.enemy())) {
				if (!unit.isSieged()) {
					unit.siege();
					lastSiege = game.getFrameCount();
				}
				return;
			}
		}
		if (unit.isSieged() && game.getFrameCount() - lastSiege > 80) {
			unit.unsiege();
		}
		super.onFrame(game, player);
	}

}
