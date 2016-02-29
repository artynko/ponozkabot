import bwapi.Unit;



public abstract class AbstractUnitEntity implements UnitEntity {
	protected Unit unit;
	
	public AbstractUnitEntity(Unit unit) {
		this.unit = unit;
	}

	@Override
	public Unit getUnit() {
		return unit;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof UnitEntity))
			return false;
		UnitEntity e = (UnitEntity) obj;
		return e.getUnit().equals(this.getUnit());
	}

	@Override
	public int hashCode() {
		return getUnit().hashCode();
	}
	
	


}
