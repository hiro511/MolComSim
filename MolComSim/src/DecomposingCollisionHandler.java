/**
 * Handles collisions in the case that molecules decompose over time
 */

public class DecomposingCollisionHandler extends CollisionHandler{

	//TODO: Not yet implemented
	public Position handlePotentialCollisions(Molecule mol, Position nextPosition, MolComSim simulation) {
		/*if(!nextPosition.isOccupied(simulation)){
		return nextPosition;
		}
		else {
		if(mol.getMoleculeType.equals(MoleculeType.INFO) && and collision with ack molecule and msg ID of both match OR
			mol is ack and collision with info and msg ID of both match)
		{
			delete the info molecule from the simulation
		}
		return mol.getPosition()
	}*/
		throw new UnsupportedOperationException("The method is not implemented yet.");
	}

}