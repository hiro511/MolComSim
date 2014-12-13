/**
 * Acknowledgment Molecule is a type of molecule
 * that is sent out by a Receiver NanoMachine
 */

import java.util.*;

public class AcknowledgementMolecule extends Molecule{

	//Indicates where molecule is intended to go
	private ArrayList<NanoMachine> destinations;
	//Where molecule started from
	private NanoMachine source;

	public AcknowledgementMolecule(MovementController mc, Position psn, double r, MolComSim sim, NanoMachine src, int msgNum, MoleculeMovementType molMvType) {
		super(mc, psn, r, sim, molMvType);
		source = src;
		msgId = msgNum; 
		destinations = sim.getTransmitters();
	}
	
	public AcknowledgementMolecule(Position psn, double r, MolComSim sim, NanoMachine src, int msgNum, MoleculeMovementType molMvType) {
		super(psn, r, sim, molMvType);
		source = src;
		msgId = msgNum; 
		destinations = sim.getTransmitters();
	}
	
	public void move() {
		setPosition(getMovementController().getNextPosition(this, getSimulation()));
		if(reachedDestination() != null){
			reachedDestination().receiveMolecule(this);
		}
	}

	//TODO: Should this be renamed whichDestinationReached?
	/**
	 * @return The destination nanomachine this molecule has arrived at,
	 * 	or null if it has not reached any destination on its list
	 *
	 */
	private NanoMachine reachedDestination() {
		for(NanoMachine dest : destinations){
			if(haveOverlap(dest)){
				return dest;
			}
		}
		return null;
	}

	/** @param dest A Nanomachine this molecule may have reached
	 *  @return true if the molecule is close enough to this nanomachine,
	 *  	else false
	 */
	private boolean haveOverlap(NanoMachine dest) {
		return getPosition().getDistance(dest.getPosition()) < getRadius() + dest.getRadius();
	}

}