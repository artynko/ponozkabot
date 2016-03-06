import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitCommandType;

abstract public class FightingUnit extends AbstractUnitEntity implements EntityWithSquad {

	private static final int DISTANCE_TO_MEDIAN = 5;
	protected Squad squad;

	public FightingUnit(Unit unit) {
		super(unit);
	}

	abstract public boolean isSupport();

	public void assigntToSquad(Squad squad) {
		squad.assignUnit(this);
		this.squad = squad;
	}

	@Override
	public void onFrame(Game game, Player player) {
		if (!unit.isCompleted()) {
			return;
		}
		if (game.getFrameCount() % 20 == 0) {
			Position groupPosition = squad.getMedianPosition();
			if (squad.getUnits().size() < 8) {
				groupPosition = squad.getLastPosition();
			}
			if (squad.getBehaviour() == Squad.Behaviour.DEFENSE) {
				groupPosition = squad.getAttackPosition();
			}
			double distaneToGroup = unit.getPosition().getDistance(groupPosition);
			if (distaneToGroup > TilePosition.SIZE_IN_PIXELS * DISTANCE_TO_MEDIAN) {
				if (distaneToGroup > TilePosition.SIZE_IN_PIXELS * 30) {
					unit.move(groupPosition);
					return;
				}
				if (unit.getPosition().getDistance(squad.getAttackPosition()) < groupPosition.getDistance(squad.getAttackPosition())) {
					if (unit.getLastCommand().getUnitCommandType() != UnitCommandType.Stop) {
						unit.stop();
					}
				} else {
					unit.attack(squad.getAttackPosition());
				}
			} else {
				if (unit.isIdle() && squad.getAttackPosition().getPoint().getDistance(unit.getPosition()) > TilePosition.SIZE_IN_PIXELS * 3) {
					//						&& unit.getLastCommand().getUnitCommandType() != UnitCommandType.Attack_Move) {
					unit.attack(squad.getAttackPosition());
				}
			}
		}
		//		if (unit.isCompleted() && unit.isIdle()) {
		//			if (squad.getAttackPosition().getPoint().getDistance(unit.getPosition()) > TilePosition.SIZE_IN_PIXELS * 6) {
		//				unit.attack(squad.getAttackPosition());
		//			}
		//		}
	}

	@Override
	public void onEntityCreate(UnitEntity unit) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onEntityDestroyed(UnitEntity unit) {
		// TODO Auto-generated method stub

	}

}
