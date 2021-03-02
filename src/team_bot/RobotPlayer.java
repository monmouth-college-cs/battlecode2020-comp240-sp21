package team_bot;
import battlecode.common.*;

import java.util.Random;

public strictfp class RobotPlayer {
    static RobotController rc;

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
    static int numMiners = 0;
    static int numDesign_Schools = 0;
    static int numFulfillment_Centers = 0;
    static int numRefineries = 0;
    static int numVaporators = 0;
    static int numNet_Guns = 0;
    // designate a spawnedByMiner bot for this bot to spawn if its a miner
    static RobotType miner_bot_spawn;
    static int numLandscapers = 0;
    static int elevate;

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

        // designate a spawnedByMiner bot for this bot to spawn if its a miner
        miner_bot_spawn = randomSpawnedByMiner();

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

    static void runHQ() throws GameActionException {
        // only make miners if there are less than 8
        if (numMiners < 10) {
            for (Direction dir : directions)
                if(tryBuild(RobotType.MINER, dir)){
                    numMiners += 1;
                }
        }
    }

    static void runMiner() throws GameActionException {
        if (hqLoc == null) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()){
                    hqLoc = robot.location;
                }
            }
        }
        tryBlockchain();

        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo robot: robots){
            if((robot.type == RobotType.FULFILLMENT_CENTER || robot.type == RobotType.DELIVERY_DRONE) && robot.team == rc.getTeam()){
                numFulfillment_Centers+=1;
            } else if((robot.type == RobotType.DESIGN_SCHOOL || robot.type == RobotType.LANDSCAPER) && robot.team == rc.getTeam()){
                numDesign_Schools+=1;
            }

        }
        for (Direction dir : directions) {
            if (tryRefine(dir)) {
                System.out.println("I refined soup! " + rc.getTeamSoup());
            }
        }
        for (Direction dir : directions) {
            if (tryMine(dir)) {
                System.out.println("I mined soup! " + rc.getSoupCarrying());
            }
        }
        // try to randomly spawn bot

        if (spawnFullCenterAndDesign()) {
            System.out.println("Spawned a bot");
        }

        if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
            Direction dirToHQ = rc.getLocation().directionTo(hqLoc);
            if (goTo(dirToHQ))
                System.out.println("moved towards HQ");
        } else {
            MapLocation[] soups = rc.senseNearbySoup();
            for (MapLocation soup : soups) {
                Direction soupDir=rc.getLocation().directionTo(soup);
                goTo(soupDir);
        }

        }

        if (goTo(randomDirection())) {
            System.out.println("I moved!");
        }
    }

    static void runRefinery() throws GameActionException {
        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {
        for (Direction dir : directions){
            if(turnCount<300){
                if (tryBuild(RobotType.LANDSCAPER, dir) && numLandscapers <= 1){
                    System.out.println("landscaper!");
                }
            }
        }
    }

    static void runFulfillmentCenter() throws GameActionException {
        for (Direction dir : directions)
            if(turnCount<300){
                tryBuild(RobotType.DELIVERY_DRONE, dir);
            }
    }

    static void runLandscaper() throws GameActionException {
        if (hqLoc == null) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                    hqLoc = robot.location;
                }
            }
        }
        tryBlockchain();
        for (Direction dir : directions) {
            if (rc.getDirtCarrying() == RobotType.LANDSCAPER.dirtLimit) {
                if(hqLoc != null){
                    Direction dirToHQ = rc.getLocation().directionTo(hqLoc);
                    if(!closeToHQ(hqLoc)){
                        if (goTo(dirToHQ)) {
                            System.out.println("Moved towards HQ");
                        }
                    }
                } else {
                    if (tryMove(randomDirection())){
                        System.out.println("moved in random direction");
                    }
                }
            } else {
                    if (tryDig(dir))
                        System.out.println("I dug dirt" + rc.getDirtCarrying());
            }
        }
        while(closeToHQ(hqLoc) && rc.getDirtCarrying()!=0) {
            for (Direction dir : directions) {
                Direction hqDir = rc.getLocation().directionTo(hqLoc);
                MapLocation dirLoc = rc.adjacentLocation(dir);
                elevate=rc.senseElevation(dirLoc);
                if (dir == hqDir) {
                    System.out.println("I broke!");
                } else if(elevate<=13){
                    System.out.println("I didn't break!");
                    if (tryDeposit(dir))
                        System.out.println("I deposited dirt" + rc.getDirtCarrying());
                }
            }
        }
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
        RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED,enemy);
        for (RobotInfo robot: robots){
            if(robot.type==RobotType.DELIVERY_DRONE){
                int robotIdent=(robot.getID());
                rc.shootUnit(robotIdent);
            }
        }
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
        int rnd = new Random().nextInt(spawnedByMiner.length);
        return spawnedByMiner[rnd];
    }

    static boolean nearbyRobot(RobotType target) throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots();
        for(RobotInfo r : robots) {
            if(r.getType() == target) {
                return true;
            }
        }
        return false;
    }

    static boolean spawnMinerBots() throws GameActionException {
        // make bots
        if (miner_bot_spawn == RobotType.DESIGN_SCHOOL && numDesign_Schools <= 1) {
            if (tryBuild(RobotType.DESIGN_SCHOOL,randomDirection())) {
                System.out.println("created a design school");
                numDesign_Schools += 1;
                return true;
            }
        }
        else if (miner_bot_spawn == RobotType.FULFILLMENT_CENTER && numFulfillment_Centers <= 1) {
            if (tryBuild(RobotType.FULFILLMENT_CENTER,randomDirection())) {
                System.out.println("created a fulfillment center");
                numFulfillment_Centers += 1;
                return true;
            }
        }
        else if (miner_bot_spawn == RobotType.REFINERY && numRefineries <= 1) {
            if (tryBuild(RobotType.REFINERY,randomDirection())) {
                System.out.println("created a refinery");
                numRefineries += 1;
                return true;
            }
        }
        else if (miner_bot_spawn == RobotType.NET_GUN && numNet_Guns <= 1) {
            if (tryBuild(RobotType.NET_GUN,randomDirection())) {
                System.out.println("created a net gun");
                numNet_Guns += 1;
                return true;
            }
        }
        else if (miner_bot_spawn == RobotType.VAPORATOR && numVaporators <= 1) {
            if (tryBuild(RobotType.VAPORATOR, randomDirection())) {
                System.out.println("created a vaporator");
                numVaporators += 1;
                return true;
            }
        }
        return false;
    }

    static boolean spawnFullCenterAndDesign() throws GameActionException {
        // make bots
        if (numDesign_Schools <= 1) {
            if (tryBuild(RobotType.DESIGN_SCHOOL, randomDirection())) {
                System.out.println("created a design school");
                numDesign_Schools += 1;
                return true;
            }
        } else if (numFulfillment_Centers <= 1) {
            if (tryBuild(RobotType.FULFILLMENT_CENTER, randomDirection())) {
                System.out.println("created a fulfillment center");
                numFulfillment_Centers += 1;
                return true;
            }
        }
        return false;
    }

    static boolean tryDig(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDigDirt(dir)) {
            rc.digDirt(dir);
            return true;
        }   else return false;
    }


    static boolean closeToHQ(MapLocation HQLoc) {
        MapLocation robotLoc = rc.getLocation();
        if(HQLoc==null){
            return false;
        }
        if (robotLoc.x == HQLoc.x + 1 || robotLoc.x == HQLoc.x - 1 || robotLoc.x == HQLoc.x) {
            if (robotLoc.y == HQLoc.y + 1 || robotLoc.y == HQLoc.y - 1 || robotLoc.y ==HQLoc.y) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
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

    // slightly better movement than tryMove
    static boolean goTo(Direction dir) throws GameActionException {
        Direction[] toTry = {dir, dir.rotateLeft(),dir.rotateRight(),
                dir.rotateLeft().rotateLeft(),dir.rotateRight().rotateRight()};
        for (Direction d : toTry) {
            if (tryMove(d)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
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

    static boolean tryDeposit(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositDirt(dir)) {
            rc.depositDirt(dir);
            return true;
        }   else return false;
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
