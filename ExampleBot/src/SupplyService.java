import java.util.Random;

import bwapi.Color;
import bwapi.Game;
import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.Chokepoint;
import bwta.Region;

public class SupplyService implements GameEntity {

	private BaseService baseService;
	private Builder builder;
	private Random random = new Random();
	private TilePosition buildLocation = null;
	private Unit depot = null;
	private boolean enabled = true;

	public SupplyService(EconomyService economyService, BaseService baseService) {
		super();
		this.baseService = baseService;
	}

	@Override
	public void onFrame(Game game, Player player) {
		if (!enabled) {
			return;
		}
		if (builder != null) {
			//			game.drawTextScreen(100, 100, "Reserving minerals for supply depot");
			game.drawCircleMap(builder.getUnit().getPosition(), 5, Color.Cyan);
			drawBox(game, baseService.getStartPosition().getTilePosition(), 4, 3);
			if (depot != null && depot.isCompleted()) {
				depot = null;
				builder = null;
			}
		} else {
			if (player.supplyTotal() < 400 && player.supplyTotal() - player.supplyUsed() <= getSupplyMargin(player)) {
				Base baseToBuild = null;
				// figure out where to build
				double bestRatio = 0;
				for (Base b : baseService.getBases()) {
					if (b.isCompleted() && b.getBuilderSize() > 10 && b.getBuildingRatio() > bestRatio) {
						baseToBuild = b;
						bestRatio = b.getBuildingRatio();
					}
				}
				if (baseToBuild == null) {
					baseToBuild = baseService.getMain();
				}
				builder = baseToBuild.getFreeBuilder();
				if (builder != null) {
					do { // find build location and store it
						buildLocation = getBuildPosition(game, baseToBuild.getBaseLocation().getTilePosition());
					} while (!buildLocation.isValid());
					builder.build(UnitType.Terran_Supply_Depot, buildLocation);
					baseToBuild.increaseBuildings(UnitType.Terran_Supply_Depot);
				}
			}
		}
	}

	private int getSupplyMargin(Player player) {
		if (player.supplyUsed() < 20) {
			return 4;
		} else if (player.supplyUsed() < 36) {
			return 8;
		} else if (player.supplyUsed() < 70) {
			return 12;
		} else if (player.supplyUsed() < 90) {
			return 20;
		} else {
			return 40;
		}

	}

	private TilePosition getBuildPosition(Game game, TilePosition startPosition) {
		Region r = BWTA.getRegion(startPosition);
		TilePosition tp = r.getPolygon().getPoints().get(random.nextInt(r.getPolygon().getPoints().size())).toTilePosition();
		// move it towards base
		tp = MathUtil.moveToLocation(tp.toPosition(), startPosition.toPosition(), TilePosition.SIZE_IN_PIXELS * 6).toTilePosition();
		game.drawCircleMap(tp.toPosition(), 5, Color.Yellow, true);
		tp = game.getBuildLocation(UnitType.Terran_Supply_Depot, tp, 6);
		game.drawCircleMap(tp.toPosition(), 5, Color.White, true);
		for (Chokepoint ch : r.getChokepoints()) { // should not be closer then 10 tiles to any chokepoint
			if (ch.getCenter().getDistance(tp.toPosition()) < TilePosition.SIZE_IN_PIXELS * 8) {
				return getBuildPosition(game, startPosition);
			}
		}
		return tp;
	}

	private void drawBox(Game game, TilePosition tp, int sizeX, int sizeY) {
		game.drawBoxMap(tp.toPosition(), new Position(tp.toPosition().getX() + TilePosition.SIZE_IN_PIXELS * sizeX, tp.toPosition().getY() + TilePosition.SIZE_IN_PIXELS
				* sizeY), Color.White);
	}

	@Override
	public void onEntityCreate(UnitEntity entity) {
		if (entity.getUnit().getType() == UnitType.Terran_Supply_Depot) {
			depot = entity.getUnit();
		}
	}

	@Override
	public void onEntityDestroyed(UnitEntity unit) {
		// TODO Auto-generated method stub

	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
