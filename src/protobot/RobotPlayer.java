package protobot;
import battlecode.common.*;
import sun.font.TrueTypeFont;

import java.text.BreakIterator;

import static battlecode.common.Team.A;
import static battlecode.common.Team.B;

public strictfp class RobotPlayer {
    static RobotController rc;
    static boolean RefineryExist = false;
    static boolean FulfillmentExist = false;
    static boolean SchoolExist = false;
    static int NMiners = 0;

    static Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST
    };
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    static int turnCount;
    static MapLocation hqLoc;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        turnCount = 0;

        System.out.println("I'm a " + rc.getType() + " and I just got created!");
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case HQ:                 runHQ();                break;
                    case MINER:              runMiner();             break;
                    case REFINERY:           runRefinery();          break;
                    case VAPORATOR:          runVaporator();         break;
                    case DESIGN_SCHOOL:      runDesignSchool();      break;
                    case FULFILLMENT_CENTER: runFulfillmentCenter(); break;
                    case LANDSCAPER:         runLandscaper();        break;
                    case DELIVERY_DRONE:     runDeliveryDrone();     break;
                    case NET_GUN:            runNetGun();            break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void findHQ() throws GameActionException {
        if (hqLoc == null) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                    hqLoc = robot.location;
                }
            }
        } else {
            System.out.println("HQ loc: " + hqLoc);
        }
    }

    static void runHQ() throws GameActionException {
        if(hqLoc == null){
            findHQ();
        }
        if (NMiners < 6 || (rc.getRoundNum() < 100)) {
            for (Direction dir : directions) {
                if(rc.canBuildRobot(RobotType.MINER, dir)){
                    rc.buildRobot(RobotType.MINER, dir);
                    NMiners += 1;
                }
            }
        }
    }


    static void runMiner() throws GameActionException {
        MapLocation myLocation = rc.getLocation(), HqLocation = null; //Identify robot and HQ locations
        tryBlockchain();

        MapLocation[] nearSoup = rc.senseNearbySoup();  //Identify soup locations
        Direction target = null;  //Variable that will storage a direction to move next time

        if (hqLoc == null) { //Locate HQ if it has not been located
            RobotInfo[] robots = rc.senseNearbyRobots(); //Sense near robots
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) { //Identify team's HQ
                    HqLocation = robot.location; //Saves the location
                    break;
                }
            }
        }

        if (SchoolExist == false && (rc.getRoundNum() < 120)) {
            for (Direction dir : directions) {
                if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir)) {
                    rc.buildRobot(RobotType.DESIGN_SCHOOL, dir);
                    SchoolExist = true;
                }
            }
        }

        if (RefineryExist == false) {
            for (Direction dir : directions) {
                if (rc.canBuildRobot(RobotType.REFINERY, dir)) {
                    rc.buildRobot(RobotType.REFINERY, dir);
                    RefineryExist = true;
                }
            }
        }

        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {  //If the miner has reached the limit of soup, proceeds to refine it
            target = rc.getLocation().directionTo(HqLocation);    //Define the direction to HQ as target
            if(rc.canDepositSoup(target)){ //Checks if HQ is at that direction to drop the soap
                if (tryRefine(target))     //Tries to refine the soup
                    System.out.println("I refined soup! " + rc.getTeamSoup());
            }
            else if (movingTo(target)) {   //If HQ is not at the direction it moves to that direction
                System.out.println("Going to refine soup");
            }
        }
        else {  //In the case it has space to keep looking for soup
            for (MapLocation Soup : nearSoup) {  //Loop through the soup locations
                if (rc.canMineSoup(myLocation.directionTo(Soup))) { //Checks if it is possible to mine
                    rc.mineSoup(myLocation.directionTo(Soup));  //Mines
                    System.out.println("I mined soup! " + rc.getSoupCarrying());
                }
                else{  //If it is not possible
                    target = rc.getLocation().directionTo(Soup); //Define the direction to a soup mine as target
                    if (movingTo(target)){ //Moves to that direction
                        System.out.println("Going to mine soup");
                    }
                }
                break;
            }
        }

        //if (FulfillmentExist == false) {
        //    for (Direction dir : directions) {
        //        if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, dir)) {
        //            rc.buildRobot(RobotType.FULFILLMENT_CENTER, dir);
        //            FulfillmentExist = true;
        //        }
        //    }
        //}


    }

    static void runRefinery() throws GameActionException {
        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {
        for (Direction dir : directions) {
            tryBuild(RobotType.LANDSCAPER, dir);
            }
    }

    static void runFulfillmentCenter() throws GameActionException {
        for (Direction dir : directions)
            tryBuild(RobotType.DELIVERY_DRONE, dir);
    }

    static void runLandscaper() throws GameActionException {
        if (rc.getDirtCarrying() == 0) {
            tryDig();
        }
        if (hqLoc != null) {
            for (Direction dir : directions) {
                MapLocation tiletoCheck = hqLoc.add(dir);
                if (rc.getLocation().distanceSquaredTo(tiletoCheck) < 4
                        && rc.canDepositDirt(rc.getLocation().directionTo(tiletoCheck))) {
                    rc.depositDirt(rc.getLocation().directionTo(tiletoCheck));
                    System.out.println("built a wall");
                }
            }
        }
        tryMove(randomDirection());
    }

    static void runDeliveryDrone() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within capturing range
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);

            if (robots.length > 0) {
                // Pick up a first robot within range
                rc.pickUpUnit(robots[0].getID());
                System.out.println("I picked up " + robots[0].getID() + "!");
            }
        } else {
            // No close robots, so search for robots within sight radius
            tryMove(randomDirection());
        }
    }

    static void runNetGun() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        RobotInfo target = null;
        MapLocation myLocation = rc.getLocation();
        int distSquared = -1;
        RobotInfo[] nearbyEnemy = rc.senseNearbyRobots(GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED, enemy);
        for (RobotInfo nearbyRobot : nearbyEnemy) {
            if (rc.canShootUnit(nearbyRobot.ID)) {
                if (target == null) {
                    target = nearbyRobot;
                    distSquared = myLocation.distanceSquaredTo(target.location);
                }
                int nearbyDist = myLocation.distanceSquaredTo(target.location);
                if (nearbyDist < distSquared) {
                    target = nearbyRobot;
                    distSquared = nearbyDist;
                }
            }
        }
        if (target != null) {
            rc.shootUnit(target.ID);
        }
    }

    static boolean movingTo(Direction dir) throws GameActionException {
        Direction[] toTry = {dir, dir.rotateLeft(),dir.rotateRight(),dir.rotateLeft().rotateLeft(),dir.rotateRight().rotateRight()};
        for (Direction direction : toTry) {
            if (tryMove(direction)) {
                return true;
            }
        }
        return false;
    }
    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random RobotType spawned by miners.
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnedByMiner() {
        return spawnedByMiner[(int) (Math.random() * spawnedByMiner.length)];
    }

    static boolean tryMove() throws GameActionException {
        for (Direction dir : directions)
            if (tryMove(dir))
                return true;
        return false;
        // MapLocation loc = rc.getLocation();
        // if (loc.x < 10 && loc.x < loc.y)
        //     return tryMove(Direction.EAST);
        // else if (loc.x < 10)
        //     return tryMove(Direction.SOUTH);
        // else if (loc.x > loc.y)
        //     return tryMove(Direction.WEST);
        // else
        //     return tryMove(Direction.NORTH);
    }

    /**
     * Attempts to move in a given direction.
     *
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryDig() throws GameActionException {
        Direction dir = randomDirection();
        if (rc.canDigDirt(dir)){
            rc.digDirt(dir);
            return true;
        }
        return false;
    }

    static boolean tryMove(Direction dir) throws GameActionException {
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.isReady() && rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to refine soup in a given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }


    static void tryBlockchain() throws GameActionException {
        if (turnCount < 3) {
            int[] message = new int[7];
            for (int i = 0; i < 7; i++) {
                message[i] = 123;
            }
            if (rc.canSubmitTransaction(message, 10))
                rc.submitTransaction(message, 10);
        }
        // System.out.println(rc.getRoundMessages(turnCount-1));
    }
}
