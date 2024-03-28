import com.huawei.codecraft.util.Point;
import com.huawei.codecraft.way.Mapinfo;
import com.huawei.codecraft.way.Path;
import com.huawei.codecraft.way.PathImpl;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class PathTest {
    private static final String[] FILE_NAMES = {
            "test\\map1.txt",  // map-3.6 0
            "test\\map2.txt",  // map-3.7 1
            "test\\map3.txt",  // map-3.8 2
            "test\\map4.txt",  // map-3.9 3
            "test\\map5.txt",  // map-3.10 4
            "test\\map6.txt",  // map-3.11 5
            "test\\map7.txt",  // map-3.12 6
            "test\\map8.txt"   // map-3.13 7
    };
    //private Path path;

    private void init(int map) throws IOException {
        try {
            List<String> lines = Files.readAllLines(Paths.get(FILE_NAMES[map]));
            char[][] inputmap = new char[lines.size()][];
            for (int i = 0; i < lines.size(); i++) {
//                System.out.println(lines.get(i).toCharArray());
                inputmap[i] = lines.get(i).toCharArray();
            }
            Mapinfo.init(inputmap);

            // 初始化路径实例
            Path path = new PathImpl();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test_Function_GetPath() throws IOException {
        int map = 4;
        init(map);
        // 初始化路径实例
        Path path = new PathImpl();
        //ArrayList<Point> path1 = path.getPath(new Point(7, 14), new Point(3, 25));
       // ArrayList<Point> path2 = path.getPath(new Point(1, 3), new Point(6, 9));
        int fps  = path.getPathFps(new Point(100, 114), new Point(15, 122));
       // System.out.println(path1);
        System.out.println(fps);
    }

    @Test
    public void test_Function_GetHidePointPath() throws IOException {
        int map = 1;
        init(map);
        // 初始化路径实例
        Path path = new PathImpl();
        Point pos = new Point(142, 191);
//        List<Point> leftPath = new ArrayList<>(Arrays.asList(
//                new Point(35, 76), new Point(35, 75), new Point(35, 74),
//                new Point(35, 73), new Point(34, 73), new Point(34, 72),
//                new Point(34, 71), new Point(33, 71), new Point(33, 70),
//                new Point(33, 69), new Point(32, 69), new Point(31, 69),
//                new Point(30, 69), new Point(30, 68), new Point(30, 67)
//        ));
        List<Point> leftPath = new ArrayList<>(Arrays.asList(
                new Point(141,190), new Point(142,190), new Point(142,191),
                new Point(143,191), new Point(143,192), new Point(144,192),
                new Point(144,193), new Point(145,193), new Point(145,194),
                new Point(146,194), new Point(146,195), new Point(147,195),
                new Point(147,196), new Point(148,196), new Point(149,196),
                new Point(150,196), new Point(151,196), new Point(151,195),
                new Point(151,194), new Point(152,194), new Point(153,194),
                new Point(153,193), new Point(153,192), new Point(154,192),
                new Point(154,191), new Point(154,190), new Point(155,190),
                new Point(156,190), new Point(157,190), new Point(158,190),
                new Point(158,189), new Point(158,188), new Point(159,188),
                new Point(159,187), new Point(159,186), new Point(159,185),
                new Point(159,184), new Point(159,183), new Point(159,182),
                new Point(160,182), new Point(161,182), new Point(162,182),
                new Point(163,182), new Point(164,182), new Point(165,182),
                new Point(166,182), new Point(167,182), new Point(168,182),
                new Point(169,182), new Point(170,182), new Point(171,182),
                new Point(172,182), new Point(173,182), new Point(173,181),
                new Point(173,180), new Point(173,179), new Point(173,178),
                new Point(173,177), new Point(173,176), new Point(173,175),
                new Point(173,174), new Point(148, 197) // 添加了遗漏的最后一个点
        ));
        ArrayList<Point> hidePath = path.getHidePointPath(pos, leftPath);
        for (Point point : hidePath) {
            System.out.println(point);
        }
    }

    @Test
    public void test_Function_getPathWithBarrierWithLimit() throws IOException {
        int map = 0;
        init(map);
        // 初始化路径实例
        Path path = new PathImpl();
        Point pos = new Point(142, 191);
        Point target = new Point(160, 190);
        HashSet<Point> leftPath = new HashSet<>(Arrays.asList(
                new Point(141,190), new Point(142,190), new Point(142,191),
                new Point(143,191), new Point(143,192), new Point(144,192),
                new Point(144,193), new Point(145,193), new Point(145,194),
                new Point(146,194), new Point(146,195), new Point(147,195),
                new Point(147,196), new Point(148,196), new Point(149,196),
                new Point(150,196), new Point(151,196), new Point(151,195),
                new Point(151,194), new Point(152,194), new Point(153,194),
                new Point(153,193), new Point(153,192), new Point(154,192),
                new Point(154,191), new Point(154,190), new Point(155,190),
                new Point(156,190), new Point(157,190), new Point(158,190),
                new Point(158,189), new Point(158,188), new Point(159,188),
                new Point(159,187), new Point(159,186), new Point(159,185),
                new Point(159,184), new Point(159,183), new Point(159,182),
                new Point(160,182), new Point(161,182), new Point(162,182),
                new Point(163,182), new Point(164,182), new Point(165,182),
                new Point(166,182), new Point(167,182), new Point(168,182),
                new Point(169,182), new Point(170,182), new Point(171,182)
        ));
        ArrayList<Point> hidePath = path.getPathWithBarrierWithLimit(pos, target, leftPath, 30);
        for (Point point : hidePath) {
            System.out.println(point);
        }
    }
}
