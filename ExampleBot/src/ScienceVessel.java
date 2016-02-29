import bwapi.Color;
import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.TechType;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitCommand;

public class ScienceVessel extends FightingUnit {

	UnitEntity following = null;

	public ScienceVessel(Unit unit) {
		super(unit);
	}

	@Override
	public void onFrame(Game game, Player player) {
		if (game.getFrameCount() % 40 == 0) {
			//				if (squad.isInCombat()) {
			//					Position toGo = MathUtil.moveToLocation(squad.getAveragePosition().getPoint(), squad.getAttackPosition().getPoint(), TilePosition.SIZE_IN_PIXELS * 2).getPoint();
			Position toGo = squad.getLeadPosition();
			game.drawCircleMap(unit.getPosition(), 5, Color.Yellow);
			if (toGo.getDistance(unit.getPosition()) > TilePosition.SIZE_IN_PIXELS * 4) {
				unit.move(toGo);
			}

			if (squad.isInCombat()) {
				if (unit.getEnergy() > 100) {
					Unit target = null;
					for (Unit u : game.getUnitsOnTile(squad.getLeadPosition().toTilePosition())) {
						if (u.exists() && u.getPlayer().equals(player) && !u.isDefenseMatrixed()) {
							target = u;
							break;
						}
					}
					if (target != null) {
						unit.useTech(TechType.Defensive_Matrix, target);
					}
				}
			}
		}
		//				} else {
		//					super.onFrame(game, player);
		//				}

		//		if (unit.getLastCommand().getUnitCommandType() != UnitCommandType.Follow) {
		//			followNew();
		//		} else {
		//			super.onFrame(game, player);
		//		}

	}

	@Override
	public boolean isSupport() {
		return true;
	}

}
