import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import bwapi.Game;
import bwapi.Player;
import bwapi.TilePosition;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.BaseLocation;
import bwta.Region;

public class BaseService implements GameEntity {

	/**
	 * Frames between 2 expands
	 */
	private static final int EXPAND_TIME_THRESHOLD = 2000;
	/**
	 * Expands when workers per base are greater then this
	 */
	private static final int EXPAND_ON_WORKERS = 19;
	private List<Builder> builders = new ArrayList<>();
	private List<Base> bases = new ArrayList<>();
	private BaseLocation startPosition;
	private EconomyService economyService;
	private Builder expandBuilder;
	private BaseLocation expandPosition;
	private UnitEntity baseInConstruction = null;
	private Base expandBase = null;
	private int lastExpandFrame = -10000;
	private int maxBuilders = 72;

	public BaseService(EconomyService economyService) {
		this.economyService = economyService;
	}

	public int basesSize() {
		return bases.size();
	}

	@Override
	public void onFrame(Game game, Player player) {
		if (startPosition == null) {
			startPosition = BWTA.getStartLocation(player);
			List<Unit> units = game.getUnitsOnTile(startPosition.getTilePosition());
			// get the command center
			Unit commandCenterUnit = null;
			for (Unit unit : units) {
				if (unit.getType() == UnitType.Terran_Command_Center) {
					commandCenterUnit = unit;
				}
			}
			Base base = addNewBase(startPosition, commandCenterUnit);
			for (Builder builder : builders) {
				base.addBuilder(builder);
			}
		}
		int builders = 0;
		for (Base base : bases) {
			base.onFrame(game, player);
			builders += base.getBuilderSize();
		}
		if (baseInConstruction != null && baseInConstruction.getUnit().isCompleted()) {
			expandBuilder = null;
			expandPosition = null;
			baseInConstruction = null;
			for (Base base : bases) {
				if (!base.equals(expandBase)) {
					base.transferBuilders(expandBase);
				}
			}
			expandBase = null;
		}
		if (game.getFrameCount() % 100 == 0) {
			// get mining bases
			int miningCount = 0;
			for (Base base : bases) {
				if (base.isMining()) {
					miningCount++;
				}
			}
			if (game.getFrameCount() > 8200 && game.getFrameCount() - lastExpandFrame > EXPAND_TIME_THRESHOLD && expandBuilder == null && (float) builders / EXPAND_ON_WORKERS > miningCount) {
				expand(game, bases.get(bases.size() - 1));
			}
			Base oversaturatedBase = null;
			for (Base base : bases) {
				if (base.isOverSaturated()) {
					oversaturatedBase = base;
					break;
				}
			}
			if (oversaturatedBase != null) {
				for (Base base : bases) {
					if (!base.isOverSaturated() && !base.beingConstructed()) {
						oversaturatedBase.transferBuilder(base);
					}
				}
			}
		}
		// handle saturation
	}

	private void expand(Game game, Base fromBase) {
		game.drawTextScreen(10, 200, "Need to expand");
		lastExpandFrame = game.getFrameCount();
		expandBuilder = fromBase.getFreeBuilder();
		List<BaseLocation> possibleLocations = new ArrayList<>();
		// get all reachable baseLocations
		Set<TilePosition> currentBases = new HashSet<>();
		for (Base base : bases) {
			currentBases.add(base.getBaseLocation().getTilePosition());
		}
		for (Region r : fromBase.getBaseLocation().getRegion().getReachableRegions()) {
			for (BaseLocation l : r.getBaseLocations()) {
				if (!currentBases.contains(l.getTilePosition())) {
					possibleLocations.add(l);
				}
			}
		}
		expandPosition = null;
		int distance = Integer.MAX_VALUE; // find the closest base
		for (BaseLocation l : possibleLocations) {
			double currentDist = BWTA.getGroundDistance(l.getTilePosition(), fromBase.getBaseLocation().getTilePosition());
			if (currentDist < distance) {
				distance = (int) currentDist;
				expandPosition = l;
			}
		}
		expandBuilder.build(UnitType.Terran_Command_Center, expandPosition.getTilePosition());
	}

	private Base addNewBase(BaseLocation location, Unit commandCenterUnit) {
		Base main = new Base(location, commandCenterUnit, economyService, this);
		bases.add(main);
		return main;
	}

	public List<Builder> getBuilders() {
		return builders;
	}

	public Base getMain() {
		return bases.get(0);
	}

	public void setBuilders(List<Builder> builders) {
		this.builders = builders;
	}

	public BaseLocation getStartPosition() {
		return startPosition;
	}

	@Override
	public void onEntityCreate(UnitEntity unit) {
		if (unit.getUnit().getType() == UnitType.Terran_Command_Center) {
			if (startPosition != null) {
				baseInConstruction = unit;
				expandBase = addNewBase(expandPosition, baseInConstruction.getUnit());
			}
		}
		if (unit instanceof Builder) {
			Builder builder = (Builder) unit;
			for (Base base : bases) {
				if (base.getBaseLocation().getPosition().getDistance(builder.getUnit().getPosition()) < TilePosition.SIZE_IN_PIXELS * 10) { // add builder to closest base
					base.addBuilder(builder);
				}
			}
			builders.add(builder);
		}
		for (Base base : bases) {
			base.onEntityCreate(unit);
		}
	}

	@Override
	public void onEntityDestroyed(UnitEntity unit) {
		for (Base base : bases) {
			base.onEntityDestroyed(unit);
		}
		builders.remove(unit);

	}

	public List<Base> getBases() {
		return bases;
	}

	public int getMaxBuilders() {
		return maxBuilders;
	}

	public void setMaxBuilders(int maxBuilders) {
		this.maxBuilders = maxBuilders;
	}

}
