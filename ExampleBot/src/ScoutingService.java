import java.util.ArrayList;
import java.util.List;

import bwapi.Color;
import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwta.BWTA;
import bwta.BaseLocation;

public class ScoutingService implements GameEntity {

	private static final int BOX_SIZE = 8;
	private BaseService baseService; // for periodic scouting
	private ArmyControlService armyControlService; // for mass scouting
	private Builder scout;
	private List<ScoutedBase> bases = new ArrayList<>();
	private ScoutedBase nextScoutBase;

	public ScoutingService(BaseService baseService, ArmyControlService armyControlService) {
		this.baseService = baseService;
		this.armyControlService = armyControlService;
	}

	@Override
	public void onFrame(Game game, Player player) {
		if (game.getFrameCount() % 40 != 0)
			return;

		if (bases.isEmpty()) {
			for (BaseLocation location : BWTA.getBaseLocations()) {
				bases.add(new ScoutedBase(false, location.isStartLocation(), -10000, location.getPosition()));
			}
		}
		if (scout == null && game.getFrameCount() > 1400 && game.getFrameCount() % 200 == 0) {
			scout = baseService.getMain().getFreeBuilder();
		}
		if (scout != null) {
			game.drawBoxMap(new Point(scout.getUnit().getPosition()).add(new Point(-BOX_SIZE, -BOX_SIZE)).toPosition(), new Point(scout.getUnit().getPosition()).add(new Point(BOX_SIZE, BOX_SIZE)).toPosition(), Color.Teal, true);
			if (nextScoutBase == null) {
				getNextScoutBase(game);
			} else {
				// check if the scout is already there
				scout.getUnit().move(nextScoutBase.position);
				if (scout.getUnit().getPosition().getDistance(nextScoutBase.position) < TilePosition.SIZE_IN_PIXELS * 5) {
					System.out.println("sending to next base");
					nextScoutBase.lastScoutedFrame = game.getFrameCount();
					boolean enemyBase = false;
					for (Unit unit : game.getUnitsOnTile(nextScoutBase.position.toTilePosition())) {
						if (unit.getPlayer().equals(game.enemy())) {
							enemyBase = true;
							break;
						}
					}
					nextScoutBase.enemyBase = enemyBase;
					nextScoutBase = null;

					getNextScoutBase(game);
				}
			}
		}
	}

	private void getNextScoutBase(Game game) {
		for (ScoutedBase sb : bases) {
			if (!sb.enemyBase && (sb.startPosition || game.getFrameCount() > 20000) && game.getFrameCount() - sb.lastScoutedFrame > 10000) {
				System.out.println("Send out");
				nextScoutBase = sb;
				scout.setReserved(true);
				scout.getUnit().stop();
				scout.getUnit().move(sb.position);
				return;
			}
		}
		Position p = BWTA.getStartLocation(game.self()).getPosition();
		scout.getUnit().move(p);
		if (scout.getUnit().getPosition().getDistance(p) < TilePosition.SIZE_IN_PIXELS * 6) {
			scout.setReserved(false);
			scout = null;
		}
	}

	@Override
	public void onEntityCreate(UnitEntity unit) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onEntityDestroyed(UnitEntity unit) {
		if (scout.equals(unit)) {
			scout = null;
			nextScoutBase = null;
		}

	}

	private class ScoutedBase {
		public boolean enemyBase = false;
		public int lastScoutedFrame = 0;
		public Position position;
		public boolean startPosition;

		public ScoutedBase(boolean enemyBase, boolean startPosition, int lastScoutedFrame, Position position) {
			this.enemyBase = enemyBase;
			this.lastScoutedFrame = lastScoutedFrame;
			this.position = position;
			this.startPosition = startPosition;
		}

	}

}
