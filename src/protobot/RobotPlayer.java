package protobot;
import battlecode.common.*;
import sun.font.TrueTypeFont;

import java.text.BreakIterator;

import static battlecode.common.GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED;
import static battlecode.common.Team.A;
import static battlecode.common.Team.B;

public strictfp class RobotPlayer {
    static RobotController rc;
    static int NMiners = 0;
    static int numDesignSchool = 0;
    static int numFulfillment = 0;
    static int numRefinery = 0;
    static int numLandscaper= 0;

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
                    case HQ:
                        runHQ();
                        break;
                    case MINER:
                        runMiner();
                        break;
                    case REFINERY:
                        runRefinery();
                        break;
                    case VAPORATOR:
                        runVaporator();
                        break;
                    case DESIGN_SCHOOL:
                        if (numLandscaper < 10) {
                            runDesignSchool(); //doesnt run design school if no need
                        }
                        break;
                    case FULFILLMENT_CENTER:
                        runFulfillmentCenter();
                        break;
                    case LANDSCAPER:
                        runLandscaper();
                        break;
                    case DELIVERY_DRONE:
                        runDeliveryDrone();
                        break;
                    case NET_GUN:
                        runNetGun();
                        break;
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
            if (hqLoc == null) {
                HQLocFromChain();
            }
        }
    }

    static void runHQ() throws GameActionException {
        if (hqLoc == null) {
            findHQ();
        }
        if (NMiners < 6 || (rc.getRoundNum() < 50)) {
            for (Direction dir : directions) {
                if (rc.canBuildRobot(RobotType.MINER, dir)) {
                    rc.buildRobot(RobotType.MINER, dir);
                    NMiners += 1;
                }
            }
        }
    }


    static void runMiner() throws GameActionException {
        updateDesignCount();
        updateFufillmentCount();
        updateRefineryCount();
        MapLocation myLocation = rc.getLocation(); //Identify robot and HQ locations

        MapLocation[] nearSoup;  //Identify soup locations
        Direction target = null;  //Variable that will storage a direction to move next time

        if (hqLoc == null) { //Locate HQ if it has not been located
            RobotInfo[] robots = rc.senseNearbyRobots(); //Sense near robots
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) { //Identify team's HQ
                    hqLoc = robot.location;//Saves the location
                    sendHqLoc(hqLoc);
                    break;
                }
            }
        }

        if (numDesignSchool < 2) {
            for (Direction dir : directions) {
                if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, dir)) {
                    rc.buildRobot(RobotType.DESIGN_SCHOOL, dir);
                }
            }
        }

        if (numRefinery < 2 && numDesignSchool >= 2) {
            for (Direction dir : directions) {
                if (rc.canBuildRobot(RobotType.REFINERY, dir)) {
                    rc.buildRobot(RobotType.REFINERY, dir);
                }
            }
        }

        if (rc.getSoupCarrying() > 3*(RobotType.MINER.soupLimit/4)) {  //If the miner has reached the limit of soup, proceeds to refine it
            target = rc.getLocation().directionTo(hqLoc);    //Define the direction to HQ as target
            if (rc.canDepositSoup(target)) { //Checks if HQ is at that direction to drop the soap
                if (tryRefine(target))     //Tries to refine the soup
                    System.out.println("I refined soup! " + rc.getTeamSoup());
            } else if (movingTo(target)) {   //If HQ is not at the direction it moves to that direction
                System.out.println("Going to refine soup");
            }
        } else {  //In the case it has space to keep looking for soup
            nearSoup = rc.senseNearbySoup();
            for (MapLocation Soup : nearSoup) {  //Loop through the soup locations
                if (rc.canMineSoup(myLocation.directionTo(Soup))) { //Checks if it is possible to mine
                    rc.mineSoup(myLocation.directionTo(Soup));  //Mines
                    System.out.println("I mined soup! " + rc.getSoupCarrying());
                } else {  //If it is not possible
                    target = rc.getLocation().directionTo(Soup); //Define the direction to a soup mine as target
                    if (movingTo(target)) { //Moves to that direction
                        System.out.println("Going to mine soup");
                    }
                }
                break;
            }
        }

        if (numFulfillment < 2 && numRefinery >= 2) {
            for (Direction dir : directions) {
                if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, dir)) {
                    rc.buildRobot(RobotType.FULFILLMENT_CENTER, dir);
                }
            }
        }
        tryMove(randomDirection());

    }

    static void runRefinery() throws GameActionException {
        if (!broadcastedRefineryCreation){
            broadcastRefineryCreation(rc.getLocation());
        }
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {
        updateLandscaperCount();
        if (!broadcastedDesignCreation){
            broadcastDesignSchoolCreation(rc.getLocation());
        }
            for (Direction dir : directions) {
                if (tryBuild(RobotType.LANDSCAPER, dir)){
                    broadcastLandscapeCreation(rc.getLocation());
            }
        }
    }


    static void runFulfillmentCenter() throws GameActionException {
        if (!broadcastedFufillmentCreation){
            broadcastFufillmentCreation(rc.getLocation());
        }
        for (Direction dir : directions) {
            tryBuild(RobotType.DELIVERY_DRONE, dir);
        }
    }

    static void runLandscaper() throws GameActionException {
        if (rc.getDirtCarrying() == 0) {
            tryDig();
        }
        if (hqLoc != null) {
            MapLocation bestPlaceToBuildWall = null;
            int lowestElevation = 99999999;
            for (Direction dir : directions) {
                MapLocation tiletoCheck = hqLoc.add(dir);
                if (rc.getLocation().distanceSquaredTo(tiletoCheck) < 4
                        && rc.canDepositDirt(rc.getLocation().directionTo(tiletoCheck))) {
                    if (rc.senseElevation(tiletoCheck) < lowestElevation) {
                        lowestElevation = rc.senseElevation(tiletoCheck);
                        bestPlaceToBuildWall = tiletoCheck;
                    }
                }
            }
            if (bestPlaceToBuildWall != null) {
                rc.depositDirt(rc.getLocation().directionTo(bestPlaceToBuildWall));
                System.out.println("building a wall");
            }
        }
        tryMove(randomDirection());
    }

    static void runDeliveryDrone() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        boolean carryingEnemy;
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within capturing range
            RobotInfo[] robots = rc.senseNearbyRobots(DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);
            RobotInfo nearest = null;
            MapLocation myLocation = rc.getLocation();
            int distToNearest = DELIVERY_DRONE_PICKUP_RADIUS_SQUARED;
            for (RobotInfo enemyRobot : robots) {
                if (enemyRobot.type == RobotType.DELIVERY_DRONE
                        || enemyRobot.type == RobotType.FULFILLMENT_CENTER || enemyRobot.type == RobotType.HQ
                        || enemyRobot.type == RobotType.NET_GUN || enemyRobot.type == RobotType.REFINERY
                        || enemyRobot.type == RobotType.DESIGN_SCHOOL || enemyRobot.type == RobotType.VAPORATOR)
                    continue;
                int distToEnemy = myLocation.distanceSquaredTo(enemyRobot.location);
                if (distToEnemy < distToNearest) {
                    nearest = enemyRobot;
                    distToNearest = distToEnemy;
                }
            }
            if (distToNearest <= GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED) {
                rc.pickUpUnit(nearest.getID());
                carryingEnemy = true;
                if (carryingEnemy= true) {
                    rc.dropUnit(Direction.WEST);
                }
            }
            else {
                // No close robots, so search for robots within sight radius
                tryMove(randomDirection());
            }

            // if (robots.length > 0) {
            // Pick up a first robot within range
            //   rc.pickUpUnit(robots[0].getID());
            // System.out.println("I picked up " + robots[0].getID() + "!");
            //   }
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
        Direction[] options = {dir, dir.rotateLeft(), dir.rotateRight(), dir.rotateLeft().rotateLeft(), dir.rotateRight().rotateRight()};
        for (Direction direction : options) {
            if (tryMove(direction) && !rc.senseFlooding(rc.adjacentLocation(direction))) {
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

    static boolean tryDig() throws GameActionException {
        Direction dir = randomDirection();
        if (rc.canDigDirt(dir)) {
            rc.digDirt(dir);
            return true;
        }
        return false;
    }

    static boolean tryMove(Direction dir) throws GameActionException {
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.adjacentLocation(dir))) {
            rc.move(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir  The intended direction of movement
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
     * @return true if any move was performed
     * @throws GameActionException
     */
    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }


    //static void tryBlockchain() throws GameActionException {
     //   if (turnCount < 3) {
       //     int[] message = new int[7];
         //   for (int i = 0; i < 7; i++) {
           //     message[i] = 123;
            //}
            //if (rc.canSubmitTransaction(message, 10))
              //  rc.submitTransaction(message, 10);
       // }
    //}

    static final int teamSecretCode = 666666666;
    static final String[] messageType = {"HQ loc", "design school exists", "Refinery exists","Fufillment exists","there is a landscaper"};

    public static void sendHqLoc(MapLocation loc) throws GameActionException {
        int[] message = new int[7];
        message[0] = teamSecretCode;
        message[1] = 0;
        message[2] = loc.x; // this is the x coordinate of our HQ
        message[3] = loc.y; // this is the y coord
        if (rc.canSubmitTransaction(message, 3)) {
            rc.submitTransaction(message, 3);
        }
    }

    public static void HQLocFromChain() throws GameActionException {
        System.out.println("Getting from Blockchain");
        for (int i = 1; i < rc.getRoundNum(); i++) {
            for (Transaction tr : rc.getBlock(i)) {
                int[] mes = tr.getMessage();
                if (mes[0] == teamSecretCode && mes[1] == 0) {
                    System.out.println("found hq");
                    hqLoc = new MapLocation(mes[2], mes[3]);
                }
            }
        }
    }

    public static boolean broadcastedRefineryCreation = false;
    public static boolean broadcastedDesignCreation = false;
    public static boolean broadcastedFufillmentCreation = false;
    public static boolean broadcastedLandscapeCreation = false;

    public static void broadcastDesignSchoolCreation(MapLocation loc) throws GameActionException {
        int[] message = new int[7];
        message[0] = teamSecretCode;
        message[1] = 1;
        message[2] = loc.x; // this is the x coordinate of our design school
        message[3] = loc.y; // this is the y coord
        if (rc.canSubmitTransaction(message, 3)) {
            rc.submitTransaction(message, 3);
            broadcastedDesignCreation = true;
        }
    }
    public static void updateDesignCount() throws GameActionException {
            for (Transaction tr : rc.getBlock(rc.getRoundNum() - 1)) {
                int[] mes = tr.getMessage();
                if (mes[0] == teamSecretCode && mes[1] == 1) {
                    System.out.println("found school");
                    numDesignSchool += 1;
                }
            }
        }
    public static void broadcastRefineryCreation(MapLocation loc) throws GameActionException {
        int[] message = new int[7];
        message[0] = teamSecretCode;
        message[1] = 2;
        message[2] = loc.x; // this is the x coordinate of our refinery
        message[3] = loc.y; // this is the y coord
        if (rc.canSubmitTransaction(message, 3)) {
            rc.submitTransaction(message, 3);
            broadcastedRefineryCreation = true;
        }
    }
    public static void updateRefineryCount() throws GameActionException {
        for (Transaction tr : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mes = tr.getMessage();
            if (mes[0] == teamSecretCode && mes[1] == 2) {
                System.out.println("found refine");
                numRefinery += 1;
            }
        }
    }
    public static void broadcastFufillmentCreation(MapLocation loc) throws GameActionException {
        int[] message = new int[7];
        message[0] = teamSecretCode;
        message[1] = 3;
        message[2] = loc.x; // this is the x coordinate of our fufillment
        message[3] = loc.y; // this is the y coord
        if (rc.canSubmitTransaction(message, 3)) {
            rc.submitTransaction(message, 3);
            broadcastedFufillmentCreation = true;
        }
    }
    public static void updateFufillmentCount() throws GameActionException {
        for (Transaction tr : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mes = tr.getMessage();
            if (mes[0] == teamSecretCode && mes[1] == 3) {
                System.out.println("found fufil");
                numFulfillment += 1;
            }
        }
    }
    public static void broadcastLandscapeCreation(MapLocation loc) throws GameActionException {
        int[] message = new int[7];
        message[0] = teamSecretCode;
        message[1] = 4;
        message[2] = loc.x; // this is the x coordinate of our landscaper
        message[3] = loc.y; // this is the y coord
        if (rc.canSubmitTransaction(message, 3)) {
            rc.submitTransaction(message, 3);
            broadcastedLandscapeCreation = true;
        }
    }
    public static void updateLandscaperCount() throws GameActionException {
        for (Transaction tr : rc.getBlock(rc.getRoundNum() - 1)) {
            int[] mes = tr.getMessage();
            if (mes[0] == teamSecretCode && mes[1] == 4) {
                System.out.println("found a landscaper");
                numLandscaper += 1;
            }
        }
    }
}

