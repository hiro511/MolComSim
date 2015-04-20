/**
 * The NanoMachine class represents a molecular machine
 * that can transmit information molecules and receive
 * acknowledgement molecules, transmit acknowledgement
 * molecules and receive information molecules, or both.
 * 
 */

import java.util.*;

public class NanoMachine {
	
	private Position position;
	private double radius;
	private MolComSim simulation;
	private Receiver rx;
	private Transmitter tx;
	// These are to track communication status for adaptive change
	public static final int LAST_COMMUNICATION_SUCCESS = 1;
	public static final int LAST_COMMUNICATION_FAILURE = -1;
	public static final int NO_PREVIOUS_COMMUNICATION = 0;

	private NanoMachine(Position psn, double r) {
		this.position = psn;
		this.radius = r;
	}

	/** Make this NanoMachine a Transmitter only
	 * 
	 * @param position Where the NanoMachine is located
	 * @param radius The radius of the NanoMachine
	 * @param molReleasePsn Position inside the transmitter at which point new molecules are released (created) 
	 * @param mpl The parameters for the molecules the Nanomachine will transmit
	 * @param sim The simulation in which this is taking place
	 * @return The resulting transmitter-only nanomachine
	 */
	public static NanoMachine createTransmitter(Position position, double radius, Position molReleasePsn, ArrayList<MoleculeParams> mpl, MolComSim sim) {
		NanoMachine retVal = new NanoMachine(position, radius);
		retVal.tx = new Transmitter(retVal, molReleasePsn, mpl, sim);
		retVal.rx = null;
		return retVal;
	}

	/** Make this NanoMachine a Receiver only
	 * 
	 * @param position Where the NanoMachine is located
	 * @param radius The radius of the NanoMachine
	 * @param molReleasePsn Position inside the transmitter at which point new molecules are released (created) 
	 * @param mpl The parameters for the molecules the Nanomachine will receive
	 * @param sim The simulation in which this is taking place
	 * @return The resulting receiver-only nanomachine
	 */
	public static NanoMachine createReceiver(Position position, double radius, Position molReleasePsn, ArrayList<MoleculeParams> mpl, MolComSim sim) {
		NanoMachine retVal = new NanoMachine(position, radius);
		retVal.rx = new Receiver(retVal, molReleasePsn, mpl, sim);
		retVal.tx = null;
		return retVal;
	}

	/** Make this NanoMachine a Transmitter and Receiver
	 * 
	 * @param position Where the NanoMachine is located
	 * @param radius The radius of the NanoMachine
	 * @param infoMolReleasePsn Position inside the transmitter at which point new info molecules are released (created) 
	 * @param ackMolReleasePsn Position inside the transmitter at which point new acko molecules are released (created) 
	 * @param mpl The parameters for the molecules the Nanomachine will transmit and receive
	 * @param ackParams 
	 * @param sim The simulation in which this is taking place
	 * @return The resulting transmitter-receiver nanomachine
	 */
	public static NanoMachine createIntermediateNode(Position position, double radius, 
			Position infoMolReleasePsn, Position ackMolReleasePsn, ArrayList<MoleculeParams> mpl, 
			ArrayList<MoleculeParams> ackParams, MolComSim sim) {
		NanoMachine retVal = new NanoMachine(position, radius);
		retVal.rx = new Receiver(retVal, ackMolReleasePsn, mpl, sim);
		retVal.tx = new Transmitter(retVal, infoMolReleasePsn, mpl, sim);
		return retVal;	
	}

	/**
	 * Creates information molecules originating at this
	 * NanoMachine's transmitter, if it has one
	 */
	public void createInfoMolecules() {
		if(tx != null) {
			tx.createMolecules();
		}
	}

	/**
	 * Calls next step for transmitter
	 * and/or receiver 
	 */
	public void nextStep() {
		if(tx != null) {
			tx.nextStep();
		}
		if(rx != null) {
			rx.nextStep();
		}
	}

	/** Receives molecule by either transmitter or receiver,
	 *  depending on type of molecule
	 * 
	 * @param m Molecule being received
	 */
	public void receiveMolecule(Molecule m) {
		if(m instanceof InformationMolecule && rx != null) {
			//System.out.println("info msg #: " + m.getMsgId() + " received at " + position);
			rx.receiveMolecule(m);
		} 
		else if(m instanceof AcknowledgementMolecule && tx != null) {
			//System.out.println("ack msg #: " + m.getMsgId() + " received at " + position);
			tx.receiveMolecule(m);
		}
	}

	public Position getPosition() {
		return position;
	}

	public double getRadius() {
		return radius;
	}

	public MolComSim getSimulation() {
		return simulation;
	}
	
	public int getTransmitterMessageId(){
		if (tx != null)
			return tx.getCurrMsgId();
		return -1;
	}
	
	public int getReceiverMessageId(){
		if (rx != null)
			return rx.getCurrMsgId();
		return -1;
	}
	

	/**
	 * Inner class that enables NanoMachine to transmit
	 *  information molecules
	 *
	 */
	public static class Transmitter {

		private MolComSim simulation;
		private int currMsgId = 1;
		private int retransmissionsLeft;
		private MoleculeCreator moleculeCreator;
		private NanoMachine nanoMachine;
		private int countdown;
		private boolean createMoleculesDelayed = false;
		private Position molReleasePsn;
		
		// To track communication status for adaptive change
		private int lastCommunicationStatus = NO_PREVIOUS_COMMUNICATION; // TODO: should really be an enumerated type

		public Transmitter(NanoMachine nm, Position molReleasePsn, ArrayList<MoleculeParams> mpl, MolComSim sim) {
			this.molReleasePsn = molReleasePsn;
			this.nanoMachine = nm;
			this.simulation = sim;
			this.moleculeCreator = new MoleculeCreator(mpl, this.simulation, this.nanoMachine, this.molReleasePsn);
			this.retransmissionsLeft =  this.simulation.getNumRetransmissions();
		}

		/**
		 *  Creates molecules for this transmitter
		 */
		public void createMolecules() {
			moleculeCreator.createMolecules(lastCommunicationStatus);
			countdown = simulation.getRetransmitWaitTime();
		}

		/**
		 * Creates molecules if time hasn't run out
		 */
		public void nextStep() {
			if(createMoleculesDelayed) {
				createMolecules();
				createMoleculesDelayed = false;
			} else if(countdown-- <= 0) {
				if(simulation.isUsingAcknowledgements()) {
					lastCommunicationStatus = LAST_COMMUNICATION_FAILURE;
					if(retransmissionsLeft-- > 0) {
						createMolecules();
					} 
				} else {
					lastCommunicationStatus = LAST_COMMUNICATION_SUCCESS;
					if (currMsgId < simulation.getNumMessages())
						++currMsgId;
					createMolecules();
				}
			} 
		}

		/**
		 * Receive molecule and tell simulation this message has been received,
		 * create more molecules if needed
		 * 
		 * @param m Molecule being received
		 */
		public void receiveMolecule(Molecule m) {
			if(m.getMsgId() == currMsgId) {
				lastCommunicationStatus = LAST_COMMUNICATION_SUCCESS;
				simulation.completedMessage(currMsgId++);
				if(!simulation.isLastMsgCompleted()) {
					createMoleculesDelayed = true;  
					retransmissionsLeft = simulation.getNumRetransmissions();
				}
			} else if(retransmissionsLeft-- > 0){
				lastCommunicationStatus = LAST_COMMUNICATION_FAILURE;
				createMoleculesDelayed = true;
			}			
		}

		public NanoMachine getNanoMachine() {
			return nanoMachine;
		}

		public MolComSim getSimulation() {
			return simulation;
		}

		public int getCurrMsgId() {
			return currMsgId;
		}

	}

	public static class Receiver {

		private MolComSim simulation;
		private int currMsgId;
		private int retransmissionsLeft;
		private MoleculeCreator moleculeCreator;
		private NanoMachine nanoMachine;
		private int countdown;
		private boolean createMoleculesDelayed = false;
		private Position molReleasePsn;

		// To track communication status for adaptive change
		private int lastCommunicationStatus = NO_PREVIOUS_COMMUNICATION;

		public Receiver(NanoMachine nm, Position molReleasePsn, ArrayList<MoleculeParams> mpl, MolComSim sim) {
			this.molReleasePsn = molReleasePsn;
			this.nanoMachine = nm;
			this.simulation = sim;
			if(this.simulation.isUsingAcknowledgements())
			{
				this.moleculeCreator = new MoleculeCreator(mpl, simulation, nanoMachine, molReleasePsn);
				currMsgId = 0;
				retransmissionsLeft =  this.simulation.getNumRetransmissions();
			}
		}

		/**
		 *  Creates molecules for this receiver
		 */
		public void createMolecules() {
			moleculeCreator.createMolecules(lastCommunicationStatus);
			countdown = simulation.getRetransmitWaitTime();
		}
		
		/**
		 * Creates acknowledgment molecules if needed by
		 * this simulation and time hasn't run out
		 */
		public void nextStep() {
			if(createMoleculesDelayed) {
				//System.out.println(simulation.getSimStep());
				createMolecules();
				createMoleculesDelayed = false;
			} else if(simulation.isUsingAcknowledgements() && 
			((countdown-- <= 0) && (retransmissionsLeft-- > 0))){
				lastCommunicationStatus = LAST_COMMUNICATION_FAILURE;
				createMolecules();
			} 
		}

		/**
		 * Receive molecule and tell simulation this message has been received,
		 * create more molecules if needed
		 * 
		 * @param m Molecule being received
		 */
		public void receiveMolecule(Molecule m) {
			if(m.getMsgId() == currMsgId + 1){
				currMsgId++;		
				lastCommunicationStatus = LAST_COMMUNICATION_SUCCESS;
				if(simulation.isUsingAcknowledgements()) {
					createMoleculesDelayed = true;
					retransmissionsLeft =  simulation.getNumRetransmissions();
				} 
				else {
					simulation.completedMessage(currMsgId);
				}
			}
			else if (simulation.isUsingAcknowledgements() && (retransmissionsLeft-- > 0)) {
				lastCommunicationStatus = LAST_COMMUNICATION_FAILURE;
				createMoleculesDelayed = true;
			}
		}

		public NanoMachine getNanoMachine() {
			return nanoMachine;
		}

		public MolComSim getSimulation() {
			return simulation;
		}

		public int getCurrMsgId() {
			return currMsgId;
		}

	}

}
