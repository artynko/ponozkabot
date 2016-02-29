import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import bwapi.Color;
import bwapi.Game;
import bwapi.Player;
import bwapi.Unit;
import bwapi.UnitType;
import bwta.BaseLocation;
import bwta.Chokepoint;

/**
 * A base class
 * 
 * @author arty
 * 
 */
public class Base implements GameEntity {

	private static final int AREA_DIVIDER = 4;
	private BaseLocation baseLocation;
	private List<Builder> builders = new ArrayList<>();
	private List<Builder> inRefinery = new ArrayList<>();
	private Unit cc;
	private EconomyService economyService;
	private Unit refineryUnit;
	private BaseService baseService;
	private Builder rampCleaner = null;
	private int buildings = 0;
	private double buildingRatio;

	public Base(BaseLocation baseLocation, Unit commandCenterUnit, EconomyService economyService, BaseService baseService) {
		super();
		this.baseLocation = baseLocation;
		this.cc = commandCenterUnit;
		this.economyService = economyService;
		this.baseService = baseService;
	}

	public void addBuilder(Builder builder) {
		builder.assignToBase(this);
		this.builders.add(builder);

	}

	public void transferBuilder(Base base) {
		transferBuilders(base, 1);
	}

	public void transferBuilders(Base base) {
		transferBuilders(base, builders.size() / 2);
	}

	private void transferBuilders(Base base, int toTransfer) {
		Iterator<Builder> itt = builders.iterator();
		while (itt.hasNext()) {
			Builder b = itt.next();
			if (toTransfer > 0 && !b.isReserved() && !b.getUnit().isConstructing() && !inRefinery.contains(b)) { // don't transfer the refinery ones and only transfer to certain size
				itt.remove();
				base.addBuilder(b);
				toTransfer--;
				b.getUnit().stop();
			}
		}
	}

	public boolean isOverSaturated() {
		return !notSaturated();
	}

	public boolean isMining() {
		int minerals = 0;
		for (Unit unit : baseLocation.getMinerals()) {
			minerals += unit.getResources();
		}
		return minerals > 4000;
	}

	public int getBuilderSize() {
		return builders.size();
	}

	public Builder getFreeBuilder() {
		for (Builder b : builders) {
			if (b.getUnit().exists() && !b.isReserved() && !b.getUnit().isConstructing() && !b.getUnit().isStuck()) {
				return b;
			}
		}
		return null; // TODO: should fix
	}

	@Override
	public void onFrame(Game game, Player player) {
		double area = baseLocation.getRegion().getPolygon().getArea() / AREA_DIVIDER;
		buildingRatio = area - buildings;
		game.drawTextMap(baseLocation.getPosition(), "building: " + buildings + "\nregion area: " + area + "\n" + buildingRatio);
		
		
		if (economyService.availableMinerals() >= 50 && !cc.isTraining() && notSaturated() && baseService.getBuilders().size() < baseService.getMaxBuilders()) {
			cc.train(UnitType.Terran_SCV);
			game.drawCircleMap(cc.getPosition(), 8, Color.Green);
		} else {
			game.drawCircleMap(baseLocation.getPosition(), 5, Color.Red, true);
		}
		if (builders.size() > 16 && inRefinery.size() == 0) {
			Builder b = getFreeBuilder();
			inRefinery.add(b);
			refineryUnit = baseLocation.getGeysers().get(0);
			if (refineryUnit.getTilePosition().isValid()) {
				b.build(UnitType.Terran_Refinery, refineryUnit.getTilePosition());
			}
		}
		if (rampCleaner == null && builders.size() > 10) {
			for (Chokepoint ch : baseLocation.getRegion().getChokepoints()) {
				for (Unit u : game.getUnitsOnTile(ch.getCenter().toTilePosition())) {
					if (u.exists() && u.getType() == UnitType.Resource_Mineral_Field) {
						rampCleaner = getFreeBuilder();
						rampCleaner.getUnit().gather(u);
					}
				}
			}
		}
		if (refineryUnit != null && refineryUnit.getType() == UnitType.Terran_Refinery && refineryUnit.isCompleted() && inRefinery.size() < 3 && builders.size() > 19) {
			Builder b = getFreeBuilder();
			b.setReserved(true);
			inRefinery.add(b);
			b.getUnit().gather(refineryUnit);
		}
	}

	private boolean notSaturated() {
		return builders.size() <= baseLocation.getMinerals().size() * 2 + 4;
	}

	@Override
	public void onEntityCreate(UnitEntity unit) {
	}

	public BaseLocation getBaseLocation() {
		return baseLocation;
	}

	@Override
	public void onEntityDestroyed(UnitEntity unit) {
		if (unit.getUnit().getType().isBuilding() && baseLocation.getRegion().getPolygon().isInside(unit.getUnit().getPosition())) {
			buildings--;
		}
		builders.remove(unit);
		inRefinery.remove(unit);
	}
	
	public boolean beingConstructed() {
		return !cc.isCompleted();
	}

	public double getBuildingRatio() {
		return buildingRatio;
	}
	
	public void increaseBuildings(UnitType type) {
		double area = baseLocation.getRegion().getPolygon().getArea() / AREA_DIVIDER;
		buildings += (type.width() * type.height());
		buildingRatio = area - buildings;
	}
	
	public boolean isCompleted() {
		return cc.isCompleted();
	}

}
