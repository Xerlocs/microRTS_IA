//Para que funcione, ir a FEStatePane.java y añadir: import ai.abstraction.DieIgn_IA;
//Y en la clase IAs agregar: DieIgn_IA.class,

package ai.abstraction;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;
import rts.units.*;

public class DieIgn_IA extends AbstractionLayerAI {

    protected UnitTypeTable utt;
    UnitType trabajador;
    UnitType base;
    boolean recursos = true;

    // Para futuras modificaciones
    UnitType cuartel;
    UnitType lightType;
    

    public DieIgn_IA(UnitTypeTable a_utt) {
        this(a_utt, new AStarPathFinding());
    }

    public DieIgn_IA(UnitTypeTable a_utt, PathFinding a_pf) {
        super(a_pf);
        reset(a_utt);
    }
    
    public void reset() {
    	super.reset();
    }
    
    public void reset(UnitTypeTable a_utt)  
    {
        utt = a_utt;
        if (utt!=null) {
            trabajador = utt.getUnitType("Worker");
            base = utt.getUnitType("Base");

            // Para futuras modificaciones
            cuartel = utt.getUnitType("Barracks");
            lightType = utt.getUnitType("Light");
        }
    }   
    
    
    public AI clone() {
        return new DieIgn_IA(utt, pf);
    }
    
    public PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);
        PlayerAction pa = new PlayerAction();
                
        // Comportamiento de bases:
        for(Unit u:pgs.getUnits()) {
            if (u.getType()==base && 
                u.getPlayer() == player && 
                gs.getActionAssignment(u)==null) {
                comportamientoBases(u,p,pgs);
            }
        }

        // Comportamiento unidades melee:
        for(Unit u:pgs.getUnits()) {
            if (u.getType().canAttack && !u.getType().canHarvest && 
                u.getPlayer() == player && 
                gs.getActionAssignment(u)==null) {
                comportamientoMelee(u,p,gs);
            }        
        }

        // Comportamiento de trabajadores:
        List<Unit> workers = new LinkedList<>();
        for(Unit u:pgs.getUnits()) {
            if (u.getType().canHarvest && 
                u.getPlayer() == player) {
                workers.add(u);
            }        
        }
        comportamientoTrabajadores(workers,p,gs);
        
        // Para futuras modificaciones
        // Comportamiento de cuarteles:
        /*for (Unit u : pgs.getUnits()) {
            if (u.getType() == cuartel
                    && u.getPlayer() == player
                    && gs.getActionAssignment(u) == null) {
                barracksBehavior(u, p, pgs);
            }
        }*/    
        
        return translateActions(player,gs);
    }
    
    
    public void comportamientoBases(Unit u,Player p, PhysicalGameState pgs) {
        if (p.getResources()>=trabajador.cost) train(u, trabajador);

        // Para futuras modificaciones
        /*
        int nworkers = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getType() == trabajador
                    && u2.getPlayer() == p.getID()) {
                nworkers++;
            }
        }
        if (nworkers < 2 && p.getResources() >= trabajador.cost) {
            train(u, trabajador);
        }
        else if(p.getResources()>=trabajador.cost) train(u, trabajador);*/
    }

    // Para futuras modificaciones
    /*
    public void barracksBehavior(Unit u, Player p, PhysicalGameState pgs) {
        if (p.getResources() >= lightType.cost) {
            train(u, lightType);
        }
    }*/

    public void comportamientoMelee(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit enemigoCercano = null;
        int distanciaCercana = 0;
        int miBase = 0;

        for(Unit u2:pgs.getUnits()) {
            if (u2.getPlayer()>=0 && u2.getPlayer()!=p.getID()) { 
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (enemigoCercano==null || d<distanciaCercana) {
                    enemigoCercano = u2;
                    distanciaCercana = d;
                }
            }
            else if(u2.getPlayer()==p.getID() && u2.getType() == base)
            {
                miBase = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
            }
        }
        if (enemigoCercano!=null) {
            attack(u,enemigoCercano);
        }
        else
        {
            attack(u, null);
        }
        
    }
    
    public void comportamientoTrabajadores(List<Unit> workers,Player p, GameState gs) {

        PhysicalGameState pgs = gs.getPhysicalGameState();
        int nbases = 0;
        int nbarracks = 0;
        int recursosUsados = 0;
        Unit cosechador = null;
        List<Unit> trabajadoresLibres = new LinkedList<>(workers);
        
        if (workers.isEmpty()) return;
        
        for(Unit u2:pgs.getUnits()) {
            if (u2.getType() == base
                    && u2.getPlayer() == p.getID()) {
                nbases++;
            }
            if (u2.getType() == cuartel
                    && u2.getPlayer() == p.getID()) {
                nbarracks++;
            }
        }
        
        List<Integer> posicionesReservadas = new LinkedList<>();
        if (nbases==0 && !trabajadoresLibres.isEmpty() && recursos) {
            // Construir una base:
            if (p.getResources()>=base.cost + recursosUsados) {
                Unit u = trabajadoresLibres.remove(0);
                buildIfNotAlreadyBuilding(u,base,u.getX(),u.getY(),posicionesReservadas,p,pgs);
                recursosUsados+=base.cost;
            }
        }

        // Para futuras modificaciones
        /*
        if (nbarracks == 0) {
            // build a barracks:
            if (p.getResources() >= cuartel.cost + recursosUsados && !trabajadoresLibres.isEmpty()) {
                Unit u = trabajadoresLibres.remove(0);
                buildIfNotAlreadyBuilding(u,cuartel,u.getX(),u.getY(),posicionesReservadas,p,pgs);
                recursosUsados += cuartel.cost;
            }
        }*/
        
        if (trabajadoresLibres.size()>1 && recursos) cosechador = trabajadoresLibres.remove(0);
        // Cosechar con el cosechador:
        if (cosechador!=null) {
            Unit baseCercana = null;
            Unit recursoCercano = null;
            int distanciaCorta = 0;
            for(Unit u2:pgs.getUnits()) {
                if (u2.getType().isResource) { 
                    int d = Math.abs(u2.getX() - cosechador.getX()) + Math.abs(u2.getY() - cosechador.getY());
                    if (recursoCercano==null || d<distanciaCorta) {
                        recursoCercano = u2;
                        distanciaCorta = d;
                    }
                }
            }
            distanciaCorta = 0;
            for(Unit u2:pgs.getUnits()) {
                if (u2.getType().isStockpile && u2.getPlayer()==p.getID()) { 
                    int d = Math.abs(u2.getX() - cosechador.getX()) + Math.abs(u2.getY() - cosechador.getY());
                    if (baseCercana==null || d<distanciaCorta) {
                        baseCercana = u2;
                        distanciaCorta = d;
                    }
                }
            }
            if (recursoCercano!=null && baseCercana!=null) {
                AbstractAction aa = getAbstractAction(cosechador);
                if (aa instanceof Harvest) {
                    Harvest h_aa = (Harvest)aa;
                    if (h_aa.getTarget() != recursoCercano || h_aa.getBase()!=baseCercana) {
                        harvest(cosechador, recursoCercano, baseCercana);
                    } else {
                    }
                } else {
                    harvest(cosechador, recursoCercano, baseCercana);
                }
            }
            else if((recursoCercano==null) && (p.getResources() == 0) && (trabajadoresLibres.isEmpty()))
            {
                
                trabajadoresLibres.add(cosechador);
                recursos = false;
            }
        }
        for(Unit u:trabajadoresLibres) comportamientoMelee(u, p, gs);
        
    }
    
    
    @Override
    public List<ParameterSpecification> getParameters()
    {
        List<ParameterSpecification> parameters = new ArrayList<>();
        
        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));

        return parameters;
    }
}
