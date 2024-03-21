import com.huawei.codecraft.core.Berth;
import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.way.Mapinfo;
import com.huawei.codecraft.zone.Region;
import com.huawei.codecraft.zone.RegionManager;
import com.huawei.codecraft.zone.Zone;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static com.huawei.codecraft.Const.*;
import static com.huawei.codecraft.zone.RegionManager.*;

public class RegionManagerTest {
    private static final int[][][] berthsPos = {
            // map8
            {{20, 139}, {26, 116}, {28, 159}, {30, 67}, {50, 41}, {124, 13}, {173, 26}, {179, 57}, {179, 92}, {179, 156}},
           // map1
            {{5, 65}, {5, 135}, {8, 35}, {12, 153}, {20, 165}, {48, 3}, {48, 187}, {190, 3}, {190, 187}, {196, 195}},
            // map2
            {{3, 113}, {3, 161}, {3, 188}, {20, 87}, {29, 73}, {32, 62}, {38, 42}, {117, 3}, {135, 3}, {172, 3}},
            // map3
            {{61, 67}, {61, 87}, {61, 112}, {70, 137}, {83, 62}, {87, 137}, {88, 62}, {103, 62}, {110, 62}, {137, 88}},
            // map4
            {{5, 91}, {5, 107}, {71, 26}, {71, 172}, {75, 56}, {123, 142}, {127, 26}, {127, 172}, {193, 91}, {193, 107}},
            // map5
            {{95, 21}, {95, 61}, {95, 101}, {95, 139}, {95, 183}, {102, 11}, {102, 52}, {102, 92}, {102, 127}, {102, 169}},
            // map6
            {{1, 44}, {1, 153}, {24, 104}, {68, 97}, {96, 152}, {103, 43}, {130, 104}, {173, 97}, {197, 43}, {197, 154}},
            // map7
            {{7, 99}, {19, 99}, {35, 99}, {49, 121}, {49, 171}, {149, 17}, {149, 66}, {161, 99}, {175, 99}, {190, 99}}
    };
    private static final String[] FILE_NAMES = {
            "test\\map8.txt",  // map-3.7 0
            "test\\map1.txt",  // map-3.8 1
            "test\\map2.txt",  // map-3.6 2
            "test\\map3.txt",  // map-3.9 3
            "test\\map4.txt",  // map-3.10 4
            "test\\map5.txt",  // map-3.11 5
            "test\\map6.txt",  // map-3.12 6
            "test\\map7.txt"   // map-3.13 7
    };
    private final int map = 3; // 测试地图,0对应map-3.6，测试不同地图修改这个
    private RegionManager regionManager;
    private void initBerth() {
        for (int i = 0; i < 10; i++) {
           berths[i] = new Berth(i);
           berths[i].pos.x = berthsPos[map][i][0];
           berths[i].pos.y = berthsPos[map][i][1];
           pointToBerth.put(berths[i].pos, berths[i]);
       }
    }

    private void init() {
        try {
            List<String> lines = Files.readAllLines(Paths.get(FILE_NAMES[map]));
            char[][] inputmap = new char[lines.size()][];
            for (int i = 0; i < lines.size(); i++) {
//                System.out.println(lines.get(i).toCharArray());
                inputmap[i] = lines.get(i).toCharArray();
            }
            Mapinfo.init(inputmap);
            initBerth(); // 单元测试需要自己手动输入berth信息
            regionManager = new RegionManager();
            regionManager.testCreateInitialRegions();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Test
    public void testCreateInitialRegions() {    // 初始区域测试
        long startTime = System.nanoTime();  // Start timing
        init();
        System.out.println("*************testCeateInitialRegions**************");
        printRegionDetails();
        System.out.println("*********************************************");

        long endTime = System.nanoTime();  // End timing
        System.out.println("region init Time taken: " + (endTime - startTime) + " ns");
    }

    @Test
    public void testGetFullPathsFromPoints2Berths() {   // 路径测试
        System.out.println("*************testGetFullPathsFromPoints2Berths**************");
        long startTime = System.nanoTime();  // Start timing
        init();

        // 测试接口函数
        regionManager.testGetFullPathsFromPoints2Berths();

        // 打印
        printPathsDetailsToFile();
        long endTime = System.nanoTime();  // End timing
        System.out.println("region init Time taken: " + (endTime - startTime) + " ns");
        System.out.println("*********************************************");

    }

    @Test
    public void testGlobalPoint2ClosestBerthMap() {  // 测试 点 到 最短泊位的hash表
        System.out.println("*************testGlobalPoint2ClosestBerthMap**************");
        long startTime = System.nanoTime();  // Start timing
        init();
        regionManager.testGetFullPathsFromPoints2Berths(); // 路径点需要根据所有泊位到点得路径来获取

        // 测试接口函数
        regionManager.testGlobalPoint2ClosestBerthMap();

        // 打印
        printGlobalPoint2ClosestBerthToFile();
        long endTime = System.nanoTime();  // End timing
        System.out.println("region init Time taken: " + (endTime - startTime) + " ns");
        System.out.println("*********************************************");
    }

    @Test
    public void testSplitRegions() {
        System.out.println("*************testSplitRegions**************");
        long startTime = System.nanoTime();  // Start timing
        init();
        printRegionDetails();
        System.out.println("*************second split**************");
        regionManager.testGetFullPathsFromPoints2Berths(); // 路径点需要根据所有泊位到点得路径来获取
        regionManager.testGlobalPoint2ClosestBerthMap();

        // 测试接口函数
        regionManager.testSplitRegions();

        // 打印
        printRegionDetails();
        printZoneDetails();
        printHashMapDetailsToFile();
        printPointDetailsToFile();
        printGlobalPoint2ClosestBerthToFile();

        long endTime = System.nanoTime();  // End timing
        System.out.println("region init Time taken: " + (endTime - startTime) + " ns");
        System.out.println("*********************************************");
    }

    /**********************************************************************************
     * 单元测试打印接口，用于打印最后结果的正确性。
     * *********************************************************************************
     */

    // 打印区域大致信息，region id，points，berth
    public void printRegionDetails() {
        System.out.println("Total regions: " + regions.size());
        for (Region region : regions) {
            System.out.println("Region ID: " + region.getId());
            System.out.println("  Berths in region: " + region.getBerths().size());
            for (Berth berth : region.getBerths()) {
                System.out.println("    Berth at: " + berth.pos + "total points:" + berth.points);
            }
            System.out.println("  Accessible points in region: " + region.getAccessiblePoints().size());
            System.out.println(" neighborRegion: ");
            for (Region neighborRegion : region.getNeighborRegions()) {
                System.out.println(neighborRegion.id);
            }
//            System.out.println("  Assigned robots in region: " + region.getAssignedRobots().size());
//            System.out.println("  Path lengths and their frequencies: ");
//            for (Map.Entry<Integer, Integer> entry : region.getPathLenToNumMap().entrySet()) {
//                System.out.println("    Path length: " + entry.getKey() + ", Number of points: " + entry.getValue());
//            }
        }
    }

    public void printGlobalPoint2ClosestBerthToFile() {
        String fileName = "points.txt";
        int total = 0;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false))) {
            for (Berth berth : berths) {
                total += berth.points;
                writer.write("Berth ID: " + berth.id + " points:" + berth.points + "\n");
            }
            writer.write("total: " + total + "\n");
            for (Map.Entry<Point, Berth> entry : pointBerthMap.entrySet()) {
                Point point = entry.getKey();
                Berth closestBerth = entry.getValue();
                writer.write("Point: " + point + " -> Closest Berth ID: " + closestBerth.id + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void printPathsDetailsToFile() {
        String fileName = "berth2point.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false))) {
            writer.write("Paths from Berths to Points:\n");
            for (Berth berth : berths) {
                Map<Point, List<Point>> paths = berth.mapPath;
                if (paths != null) {
                    for (Map.Entry<Point, List<Point>> entry : paths.entrySet()) {
                        Point point = entry.getKey();
                        List<Point> path = entry.getValue();
                        writer.write("Path from Berth " + berth.pos + " to " + point + ":\n");
                        for (Point pathPoint : path) {
                            writer.write(pathPoint + " ");
                        }
                        writer.newLine();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getShortestDistanceBetweenRegions(Region region1, Region region2) {
        int shortestDistance = unreachableFps;

        for (Berth berth1 : region1.berths) {
            for (Berth berth2 : region2.berths) {
                List<Point> path = berth1.mapPath.get(berth2.pos);
                if (path != null && path.size() < shortestDistance) {
                    shortestDistance = path.size();
                }
            }
        }

        return shortestDistance;
    }

    public void printNeighborRegionsDetails() {
        System.out.println("Total regions: " + regions.size());
        for (Region region : regions) {
            System.out.println("Region ID: " + region.id);
            System.out.println("  Neighbor regions and distances:");

            for (Region neighborRegion : region.neighborRegions) {
                int distance = getShortestDistanceBetweenRegions(region, neighborRegion);
                System.out.println("    Neighbor Region ID: " + neighborRegion.id + ", Distance: " + distance);
            }
        }
    }

    // 打印点所属于的区域
    public void printPointDetailsToFile() {
        String fileName = "point2Region.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false))) { // false to overwrite
            writer.write("Total regions: " + regions.size() + "\n");
            for (Region region : regions) {
                writer.write("Region ID: " + region.getId() + "\n");
                writer.write("  Berths in region: " + region.getBerths().size() + "\n");
                for (Berth berth : region.getBerths()) {
                    writer.write("    Berth at: " + berth.pos + "\n");
                }
                writer.write("  Accessible points in region: " + region.getAccessiblePoints().size() + "\n");
                if (!region.getAccessiblePoints().isEmpty()) {
                    writer.write("    Accessible Points: ");
                    for (Point point : region.getAccessiblePoints()) {
                        writer.write(point + " ");
                    }
                    writer.write("\n");  // Move to the next line after printing all points of the region.
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void printZoneDetails() {
        System.out.println("Zone details:");
        for (Zone zone : zones) {
            System.out.println("Zone ID: " + zone.id);
            System.out.println("  Number of Regions: " + zone.regions.size());
            for (Region region : zone.regions) {
                System.out.println("    Region ID: " + region.getId());
                System.out.println("    Number of Points: " + region.getAccessiblePoints().size());
            }
        }
    }

    public void printHashMapDetailsToFile() {
        String fileName = "mapDetails.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false))) {
            // 打印每个区域的邻居区域信息
            writer.write("neighborRegions:\n");
            for (Region region : regions) {
                writer.write("Region ID: " + region.getId() + " Neighbor Regions: ");
                for (Region neighbor : region.neighborRegions) {
                    writer.write(neighbor.getId() + " ");
                }
                writer.write("\n");
            }

            // 打印 pathLenToNumMap 信息
            writer.write("\nPath Length to Number of Points Mapping for each Region:\n");
            for (Region region : regions) {
                writer.write("Region ID: " + region.getId() + "\n");
                for (Map.Entry<Integer, Integer> pathEntry : region.getPathLenToNumMap().entrySet()) {
                    writer.write("  Path Length: " + pathEntry.getKey() + ", Number of Points: " + pathEntry.getValue() + "\n");
                }
            }

            writer.write("\nPath Length to Number of Points Mapping for each Berth:\n");
            for (Berth berth : berths) {
                int total = 0;
                writer.write("Berth ID: " + berth.id + "  " + berth.pos + "\n");
                for (Map.Entry<Integer, Integer> pathEntry : berth.pathLenToNumMap.entrySet()) {
                    writer.write("  Path Length: " + pathEntry.getKey() + ", Number of Points: " + pathEntry.getValue() + "\n");
                    total += pathEntry.getValue();
                }
                writer.write("Total points: " + total);
            }

            // 打印 globalPointToClosestBerth
            writer.write("globalPointToClosestBerth:\n");
            for (Map.Entry<Point, Berth> entry : pointBerthMap.entrySet()) {
                writer.write("Point: " + entry.getKey() + " -> Berth: " + entry.getValue().pos + "\n");
            }

            writer.write("\n"); // 添加空行以区分不同的映射

            // 打印 pointRegionMap
            writer.write("pointRegionMap:\n");
            for (Map.Entry<Point, Region> entry : pointRegionMap.entrySet()) {
                writer.write("Point: " + entry.getKey() + " -> Region ID: " + entry.getValue().getId() + "\n");
            }

            writer.write("\n"); // 添加空行以区分不同的映射

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
