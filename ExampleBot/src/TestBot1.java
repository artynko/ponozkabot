import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bwapi.Color;
import bwapi.DefaultBWListener;
import bwapi.Game;
import bwapi.Mirror;
import bwapi.Player;
import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BWTA;
import bwta.BaseLocation;

public class TestBot1 extends DefaultBWListener {

	private Mirror mirror = new Mirror();

	private Game game;

	private Player self;
	private Player enemy;
	private List<GameEntity> services;
	private List<UnitEntity> units;
	private EconomyService economyService;
	private BaseBuildingService baseBuildingService;
	private MacroService macroService;
	private ArmyControlService armyControlService;
	private ScoutingService scoutingService;
	private ResearchService researchService;
	private CounterService counterService;
	private SixPoolDefenseService sixPoolDefenseService;

	private SupplyService supplyService;

	private BaseService baseService;

	public void run() {
		units = new ArrayList<>();
		services = new ArrayList<>();

		// register all the service
		economyService = new EconomyService();
		baseService = new BaseService(economyService);
		supplyService = new SupplyService(economyService, baseService);
		baseBuildingService = new BaseBuildingService(economyService, baseService);
		macroService = new MacroService(economyService);
		armyControlService = new ArmyControlService(baseService, economyService);
		scoutingService = new ScoutingService(baseService, armyControlService);
		researchService = new ResearchService(economyService);
		sixPoolDefenseService = new SixPoolDefenseService();
		counterService = new CounterService(macroService, sixPoolDefenseService, baseBuildingService);

		services.add(economyService);
		services.add(supplyService);
		services.add(baseService);
		services.add(baseBuildingService);
		services.add(macroService);
		services.add(armyControlService);
		services.add(scoutingService);
		services.add(counterService);
		services.add(sixPoolDefenseService);
		services.add(researchService);

		mirror.getModule().setEventListener(this);
		mirror.startGame();
	}

	@Override
	public void onUnitCreate(Unit unit) {
		System.out.println("Created:" + unit.getType());
		if (unit.getPlayer() != self)
			return;
		UnitEntity unitEntity = null;
		if (unit.getType() == UnitType.Terran_SCV) {
			unitEntity = new Builder(unit, economyService);
		} else if (unit.getType() == UnitType.Terran_Marine) {
			unitEntity = new Marine(unit);
		} else if (unit.getType() == UnitType.Terran_Medic) {
			unitEntity = new Medic(unit);
		} else if (unit.getType() == UnitType.Terran_Firebat) {
			unitEntity = new Firebat(unit);
		} else if (unit.getType() == UnitType.Terran_Science_Vessel) {
			unitEntity = new ScienceVessel(unit);
		} else {
			unitEntity = new BaseUnitEntity(unit);
		}
		if (unitEntity != null) {
			units.add(unitEntity);
			for (GameEntity entity : services) {
				entity.onEntityCreate(unitEntity);
			}
			for (GameEntity entity : units) {
				entity.onEntityCreate(unitEntity);
			}
		}
	}

	@Override
	public void onStart() {
		game = mirror.getGame();
		self = game.self();
		enemy = game.enemy();
		game.enableFlag(1);
		game.setLocalSpeed(30);

		// Use BWTA to analyze map
		// This may take a few minutes if the map is processed first time!
		System.out.println("Analyzing map...");
		BWTA.readMap();
		BWTA.analyze();
		System.out.println("Map data ready");

		int i = 0;
		for (BaseLocation baseLocation : BWTA.getBaseLocations()) {
			System.out.println("Base location #" + (++i) + ". Printing location's region polygon:");
			for (Position position : baseLocation.getRegion().getPolygon().getPoints()) {
				System.out.print(position + ", ");
			}
			System.out.println();
		}

		for (Unit myUnit : self.getUnits()) {
			game.drawCircle(bwapi.CoordinateType.Enum.Map, myUnit.getPosition().getX(), myUnit.getPosition().getY(), 5, Color.Cyan);
			game.drawCircle(bwapi.CoordinateType.Enum.Map, myUnit.getPoint().getX(), myUnit.getPoint().getY(), 3, Color.Red);

			// if it's a drone and it's idle, send it to the closest mineral
			// patch
		}

	}

	@Override
	public void onFrame() {
		game.drawTextScreen(10, 10, "Playing as " + self.getName() + " - " + self.getRace() + " - " + game.getAPM() + " - " + game.getFrameCount());
		for (GameEntity service : services) {
			service.onFrame(game, self);
		}
		for (GameEntity service : units) {
			service.onFrame(game, self);
		}

		// game.setTextSize(10);


//		for (Unit myUnit : self.getUnits()) {
//			if (someUnit == null && myUnit.exists())
//				someUnit = myUnit;
//			// if there's enough minerals, train an SCV
//		}
//		// iterate through my units
//		bwta.Region r = BWTA.getRegion(someUnit.getPosition());
//		for (int n = 0; n < r.getPolygon().getPoints().size() - 1; n++) {
//			game.drawLineMap(r.getPolygon().getPoints().get(n).getPoint(), r.getPolygon().getPoints().get(n + 1).getPoint(), Color.White);
//			for (Chokepoint ch : r.getChokepoints()) {
//				game.drawCircleMap(ch.getCenter(), (int) ch.getWidth(), Color.Red);
//
//			}
//		}

		// draw my units on screen
	}

	public static void main(String[] args) {
		new TestBot1().run();
	}

	@Override
	public void onUnitDestroy(Unit unit) {
		// enemy unit destroyed
		if (unit.getPlayer().equals(enemy)) {
			for (GameEntity entity : services) {
				if (entity instanceof EnemyUnitDestroyedListener) {
					((EnemyUnitDestroyedListener) entity).onEnemyEntityDestoyed(unit);
				}
			}
			for (GameEntity entity : units) {
				if (entity instanceof EnemyUnitDestroyedListener) {
					((EnemyUnitDestroyedListener) entity).onEnemyEntityDestoyed(unit);
				}
			}
			return;
		}
		
		
		// my unit destroyed
		Iterator<UnitEntity> itt = units.iterator();

		UnitEntity unitEntity = null;
		while (itt.hasNext()) {
			UnitEntity u = itt.next();
			if (u.getUnit().equals(unit)) {
				unitEntity = u;
			}
		}
		if (unitEntity != null) {
			for (GameEntity entity : services) {
				entity.onEntityDestroyed(unitEntity);
			}
			for (GameEntity entity : units) {
				entity.onEntityDestroyed(unitEntity);
			}
			units.remove(unitEntity);
		}
	}

	@Override
	public void onUnitDiscover(Unit unit) {
		if (unit.getPlayer().equals(enemy)) {
			for (GameEntity entity : services) {
				if (entity instanceof EnemyUnitDiscoveredListener) {
					((EnemyUnitDiscoveredListener) entity).onEnemyEntityDiscovered(unit);
				}
			}
			for (GameEntity entity : units) {
				if (entity instanceof EnemyUnitDiscoveredListener) {
					((EnemyUnitDiscoveredListener) entity).onEnemyEntityDiscovered(unit);
				}
			}
		}
//		System.out.println("Discovered:" + unit.getType());
	}

	@Override
	public void onUnitHide(Unit arg0) {
//		System.out.println("Hidden:" + arg0.getType());
		super.onUnitHide(arg0);
	}

	@Override
	public void onUnitShow(Unit arg0) {
//		System.out.println("Shown:" + arg0.getType());
		super.onUnitShow(arg0);
	}

	@Override
	public void onUnitMorph(Unit unit) {
		if (unit.getType() == UnitType.Terran_Refinery) {
			BaseUnitEntity unitEntity = new BaseUnitEntity(unit);
			if (unitEntity != null) {
				units.add(unitEntity);
				for (GameEntity entity : services) {
					entity.onEntityCreate(unitEntity);
				}
				for (GameEntity entity : units) {
					entity.onEntityCreate(unitEntity);
				}
			}
		}
	}

}