import java.util.Random;

import bwapi.Color;
import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;

public class Medic extends FightingUnit {

	UnitEntity following = null;

	public Medic(Unit unit) {
		super(unit);
	}

	@Override
	public void onFrame(Game game, Player player) {
		if (game.getFrameCount() % 40 == 0) {
			if (unit.getDistance(squad.getMedianPosition()) > TilePosition.SIZE_IN_PIXELS * 16) {
				unit.move(squad.getMedianPosition());
				return;
			}
			//				if (squad.isInCombat()) {
			//					Position toGo = MathUtil.moveToLocation(squad.getAveragePosition().getPoint(), squad.getAttackPosition().getPoint(), TilePosition.SIZE_IN_PIXELS * 2).getPoint();
			Position toGo = squad.getLeadPosition();
			game.drawCircleMap(unit.getPosition(), 5, Color.Yellow);
			if (toGo.getDistance(unit.getPosition()) > TilePosition.SIZE_IN_PIXELS * 4) {
				unit.attack(toGo);
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

	private void followNew() {
		Random r = new Random();
		if (squad.getAttackUnits().size() > 0) {
			following = squad.getAttackUnits().get(r.nextInt(squad.getAttackUnits().size()));
			unit.follow(following.getUnit());
		}
	}

	@Override
	public boolean isSupport() {
		return true;
	}

	@Override
	public void onEntityDestroyed(UnitEntity unit) {
		if (unit.equals(following)) {
			followNew();
		}
		super.onEntityDestroyed(unit);
	}

}
